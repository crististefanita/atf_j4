#requires -Version 7.0
[CmdletBinding(DefaultParameterSetName='Execute')]
param(
    [Parameter(Mandatory)][string]$CandidateDir,
    [Parameter(Mandatory)][string]$SourceRoot,
    [Parameter(Mandatory)][string]$RunRoot,
    [Parameter(Mandatory)][string]$SelectionFile,
    [Parameter(Mandatory)][string]$Group,
    [Parameter(Mandatory)][string]$ApkPath,
    [Parameter(ParameterSetName='Prepare')][switch]$PrepareOnly,
    [string]$PreparedFrom,
    [string]$Maven = 'mvn',
    [string]$MavenCache = (Join-Path $env:USERPROFILE '.m2/repository'),
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$LeasePath = 'C:\work\whist_back\android-ui-lease.json',
    [string]$Serial = '127.0.0.1:5555',
    [string]$AdbServerSocket = 'tcp:192.168.56.1:5038',
    [string]$AdbExecutable = 'C:\tools\whist-android-sdk\platform-tools\adb.exe'
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'AtfAndroidMaven.ps1')
. (Join-Path $PSScriptRoot 'AtfDeviceLease.ps1')
. (Join-Path $PSScriptRoot 'AtfQualificationTiming.ps1')
$project = Split-Path -Parent $PSScriptRoot
$root = [IO.Path]::GetFullPath($RunRoot)
$source = (Resolve-Path -LiteralPath $SourceRoot).ProviderPath
$candidate = (Resolve-Path -LiteralPath $CandidateDir).ProviderPath
foreach ($boundary in @($project,$source,$candidate)) {
    if ([IO.Path]::GetRelativePath($boundary,$root) -notmatch '^\.\.(?:[\\/]|$)' -and
        [IO.Path]::GetPathRoot($boundary) -ieq [IO.Path]::GetPathRoot($root)) {
        throw 'ANDROID_E2E_RUN_ROOT_MUST_BE_EXTERNAL'
    }
}
if ($root -ieq $project -or (Test-Path -LiteralPath $root)) { throw 'ANDROID_E2E_RUN_ROOT_MUST_BE_NEW' }
$null = New-Item -ItemType Directory -Path $root
$timing = New-AtfQualificationTiming 'signed-e2e' (Join-Path $root 'timing.json')
$workspace = $root
if ($PreparedFrom) {
    $workspace = (Resolve-Path -LiteralPath $PreparedFrom).ProviderPath
    foreach ($boundary in @($project,$source,$candidate,$root)) {
        if ([IO.Path]::GetRelativePath($boundary,$workspace) -notmatch '^\.\.(?:[\\/]|$)' -and
            [IO.Path]::GetPathRoot($boundary) -ieq [IO.Path]::GetPathRoot($workspace)) {
            throw 'ANDROID_E2E_PREPARATION_MUST_BE_EXTERNAL'
        }
    }
}
$stage = Join-Path $workspace 'work'
$scratch = Join-Path $root 'tmp'
$repo = Join-Path $workspace 'maven-repository'
$tool = Join-Path $project 'atf-tests/tools/prepare-android-e2e.mjs'
$isolation = Join-Path $PSScriptRoot 'Invoke-AtfAndroidDisposableUser.ps1'
$manifest = Get-Content -LiteralPath (Join-Path $candidate 'release-validation.json') -Raw | ConvertFrom-Json
$apkHash = $manifest.release.apkSha256
if ($apkHash -cnotmatch '^[0-9a-f]{64}$' -or (Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash -ine $apkHash) {
    throw 'ANDROID_E2E_APK_HASH'
}
$apk = (Resolve-Path -LiteralPath $ApkPath).ProviderPath
$inputFile = Join-Path $root 'source-inputs.json'
$inputScript = Join-Path $PSScriptRoot 'Get-AtfSourceInputs.ps1'
$sourceInputs = Invoke-AtfQualificationPhase $timing 'e2e.source-inventory' {
    & $inputScript -SourceRoot $source -AsJson
}
[IO.File]::WriteAllText($inputFile,$sourceInputs)
Copy-Item -LiteralPath $SelectionFile -Destination (Join-Path $root 'e2e-request.json')
Invoke-AtfQualificationPhase $timing 'e2e.reuse-check' {
    & node $tool plan $candidate $root $Group
    if ($LASTEXITCODE -ne 0) { throw 'ANDROID_E2E_REUSE_PLAN_FAILED' }
}
$plan = Get-Content -LiteralPath (Join-Path $root 'e2e-reuse.json') -Raw | ConvertFrom-Json
$selected = $plan.decisions | Where-Object {$_.group -ceq $Group}
if ($selected.decision -ceq 'REUSE') {
    Write-AtfQualificationDecision $timing "e2e.run.$Group" 'REUSED' 'Previously accepted evidence passed the same APK, source and scenario checks.'
    return $selected
}

$selection = $selected.scenario.selection
$class, $method = $selection.Split('#',2)
$saved = @{}
foreach ($name in @('JAVA_HOME','TEMP','TMP','MAVEN_ARGS','MAVEN_OPTS','ATF_RELEASE_STAGE_TIMING_ROOT')) {
    $saved[$name] = [Environment]::GetEnvironmentVariable($name)
}
function Write-RunJson([string]$Name, $Value) {
    [IO.File]::WriteAllText((Join-Path $root $Name),($Value | ConvertTo-Json -Depth 8) + "`n")
}
function Invoke-PrivateMaven([string]$Log, [string[]]$Arguments) {
    & $Maven '-B' '-o' '-ntp' "-Dmaven.repo.local=$repo" @Arguments *> (Join-Path $root $Log)
    if ($LASTEXITCODE -ne 0) { throw "ANDROID_E2E_MAVEN_FAILED: $Log" }
}
function Get-PrivateLibraryFingerprint {
    $coordinates = Join-Path $repo 'atf/api'
    $installed = @(Get-ChildItem -LiteralPath $coordinates -Recurse -File | Where-Object {$_.Extension -in '.jar','.pom'})
    if (-not $installed.Count) { throw 'ANDROID_E2E_MAVEN_LIBRARIES_MISSING' }
    $records = [Collections.Generic.List[string]]::new()
    foreach ($library in $installed) {
        $input = Join-Path $stage "lib/$($library.Name)"
        $hash = (Get-FileHash -LiteralPath $library.FullName).Hash.ToLowerInvariant()
        if ($hash -cne (Get-FileHash -LiteralPath $input).Hash.ToLowerInvariant()) {
            throw "ANDROID_E2E_MAVEN_LIBRARY_CHANGED: $($library.Name)"
        }
        $records.Add($library.Name + ':' + $hash)
    }
    foreach ($artifact in @('atf-core','atf-reporting','atf-test-kit','atf-console','atf-mock')) {
        if (-not ($installed.Name -like "$artifact-*.jar") -or -not ($installed.Name -like "$artifact-*.pom")) {
            throw "ANDROID_E2E_MAVEN_LIBRARY_MISSING: $artifact"
        }
    }
    $records.Sort([StringComparer]::Ordinal)
    Get-AtfTextFingerprint ($records -join "`n")
}
$lease = $null
$workspaceLock = Open-AtfQualificationWorkspace $workspace
try {
    New-Item -ItemType Directory -Path $scratch | Out-Null
    $env:TEMP = $scratch
    $env:TMP = $scratch
    $env:ATF_RELEASE_STAGE_TIMING_ROOT = $root
    $env:MAVEN_ARGS = $null
    $env:MAVEN_OPTS = '-Djava.io.tmpdir="' + $scratch + '"'
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    $javap = if ($JavaHome) { Join-Path $JavaHome 'bin/javap.exe' } else { (Get-Command javap -ErrorAction Stop).Source }
    $priorBuild = $null
    if ($PreparedFrom) {
        $priorBuild = Get-Content -LiteralPath (Join-Path $workspace 'compilation.json') -Raw | ConvertFrom-Json
        if ($priorBuild.sourceFingerprint -cne $plan.sourceFingerprint -or $priorBuild.installedApkSha256 -cne $apkHash) {
            throw 'ANDROID_E2E_COMPILATION_IDENTITY'
        }
        foreach ($directory in @($workspace, $stage, $repo)) {
            if ((Get-Item -LiteralPath $directory).Attributes -band [IO.FileAttributes]::ReparsePoint) {
                throw 'ANDROID_E2E_INDIRECT_PREPARATION'
            }
        }
    } else {
        New-Item -ItemType Directory -Path $stage | Out-Null
        & (Join-Path $PSScriptRoot 'Copy-AtfSourceInputs.ps1') -SourceRoot $source -Destination $stage -InputManifest $inputFile
        if ($sourceInputs -cne (& $inputScript -SourceRoot $stage -AsJson)) { throw 'ANDROID_E2E_STAGING_CHANGED' }
        Invoke-AtfQualificationPhase $timing 'e2e.maven-cache' {
            Copy-AtfMavenCache $MavenCache $repo (Join-Path $root 'copy-maven-cache.log')
        }
    }

    Push-Location $stage
    try {
        if ($priorBuild) {
            if ((Get-AtfAndroidClassFingerprint $stage) -cne $priorBuild.classFingerprint -or
                (Get-PrivateLibraryFingerprint) -cne $priorBuild.libraryFingerprint) {
                throw 'ANDROID_E2E_PREPARATION_CHANGED'
            }
            Write-AtfQualificationDecision $timing 'e2e.maven-compile' 'REUSED' 'Verified previously compiled classes and libraries.'
        } else {
            Invoke-AtfQualificationPhase $timing 'e2e.maven-compile' {
                Invoke-PrivateMaven 'compile.log' @('-pl','atf-test-android','-am','test-compile','-Datf.android.enabled=false')
            }
        }
        if ($sourceInputs -cne (& $inputScript -SourceRoot $stage -AsJson)) { throw 'ANDROID_E2E_STAGING_CHANGED' }
        $libraryFingerprint = Get-PrivateLibraryFingerprint
        $classes = Join-Path $stage 'atf-test-android/target/test-classes'
        $matches = @(Get-ChildItem -LiteralPath $classes -Recurse -File -Filter "$class.class")
        if ($matches.Count -ne 1) { throw "ANDROID_E2E_EXPECTED_ONE_CLASS: $class" }
        Assert-AtfCompiledMethod $javap $matches[0].FullName $method
        $classFingerprint = Get-AtfAndroidClassFingerprint $stage
        Write-RunJson 'compilation.json' ([ordered]@{sourceFingerprint=$plan.sourceFingerprint;installedApkSha256=$apkHash;selection=$selection;classFingerprint=$classFingerprint;libraryFingerprint=$libraryFingerprint})
        if ($PrepareOnly) {
            Write-AtfQualificationDecision $timing "e2e.run.$Group" 'NOT_RUN' 'PrepareOnly ends before obtaining an Android lease.'
            return (Join-Path $root 'compilation.json')
        }
        $junit = Join-Path $root 'junit'

        $lease = Invoke-AtfQualificationPhase $timing 'e2e.lease' {
            New-AtfDeviceLease $LeasePath $Serial 'Whist signed release E2E' $root
        }
        $scenario = $selected.scenario
        $arguments = @{
            RunRoot=$root;Serial=$Serial;AdbExecutable=$AdbExecutable;ServerSocket=$AdbServerSocket
            LeasePath=$LeasePath;LeaseId=$lease;ExpectedApi=$scenario.api;ExpectedDisplayProfile=$scenario.displayProfile
        }
        try {
            Invoke-AtfQualificationPhase $timing 'e2e.prepare-device' {
                & $isolation @arguments -Phase Prepare -ApkPath $apk -ApkSha256 $apkHash | Out-Host
            }
            if ((Get-AtfAndroidClassFingerprint $stage) -cne $classFingerprint) { throw 'ANDROID_E2E_COMPILED_CLASSES_CHANGED' }
            if ((Get-PrivateLibraryFingerprint) -cne $libraryFingerprint) { throw 'ANDROID_E2E_MAVEN_LIBRARIES_CHANGED' }
            $artifacts = Join-Path $root 'android-artifacts'
            Invoke-AtfQualificationPhase $timing "e2e.run.$Group" {
                Invoke-PrivateMaven 'flow.log' @('-pl','atf-test-android','surefire:test','-Psigned-release-junit',"-Datf.android.junit.dir=$junit","-Dtest=$selection",
                    '-Dsurefire.failIfNoSpecifiedTests=true',
                    '-Datf.android.enabled=true',"-Datf.android.adb=$AdbExecutable","-Datf.android.adbServerSocket=$AdbServerSocket",
                    "-Datf.android.serial=$Serial","-Datf.android.api=$($scenario.api)",
                    "-Datf.android.artifacts=$artifacts","-Dwhist.records.runRoot=$root","-Dwhist.records.leasePath=$LeasePath",
                    "-Datf.android.sanity.profiles=$($scenario.responsiveProfiles)","-Datf.android.commandTimeoutSeconds=90",
                    "-Datf.governance.gitRoot=$stage")
                Assert-AtfSingleJUnitPass $junit $selection
            }
        } finally {
            if (Test-Path -LiteralPath (Join-Path $root 'setup-release.json') -PathType Leaf) {
                Invoke-AtfQualificationPhase $timing 'e2e.restore-device' {
                    & $isolation @arguments -Phase Teardown | Out-Host
                }
            }
        }
        if ((Get-AtfAndroidClassFingerprint $stage) -cne $classFingerprint) { throw 'ANDROID_E2E_COMPILED_CLASSES_CHANGED' }
        if ((Get-PrivateLibraryFingerprint) -cne $libraryFingerprint) { throw 'ANDROID_E2E_MAVEN_LIBRARIES_CHANGED' }
        $ready = Get-Content -LiteralPath (Join-Path $root 'data-isolation-ready.json') -Raw | ConvertFrom-Json
        Write-RunJson 'attempt.json' ([ordered]@{scenario=$scenario;leaseId=$lease;serial=$Serial;testUserId=$ready.testUserId
            compilationReceiptSha256=(Get-FileHash -LiteralPath (Join-Path $root 'compilation.json')).Hash.ToLowerInvariant()})
        Invoke-AtfQualificationPhase $timing 'e2e.extent-evidence' {
            & node $tool record $candidate $root $Group
            if ($LASTEXITCODE -ne 0) { throw 'ANDROID_E2E_EVIDENCE_REJECTED' }
        }
        Join-Path $root 'evidence/e2e-result.json'
    } finally {
        Pop-Location
    }
} finally {
    try { if ($lease) { Remove-AtfDeviceLease $LeasePath $lease } }
    finally {
        $workspaceLock.Dispose()
        foreach ($name in $saved.Keys) { [Environment]::SetEnvironmentVariable($name,$saved[$name]) }
    }
}
