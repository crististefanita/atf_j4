param(
    [string]$Profiles = '',
    [Alias('HandsPerProfile')][ValidateRange(1,5)][int]$Hands = 1,
    [string]$Serial = '127.0.0.1:5555',
    [string]$AdbServerSocket = 'tcp:192.168.56.1:5038',
    [string]$AdbExecutable = 'C:\tools\whist-android-sdk\platform-tools\adb.exe',
    [string]$Maven = 'C:\tools\Maven\apache-maven-3.10.0\bin\mvn.cmd',
    [string]$MavenRepository = 'C:\Users\Cristi\.m2\repository',
    [string]$JavaHome = 'C:\tools\Java21\jdk-21.0.12.1+1',
    [switch]$DenseLayout,
    [switch]$HardAiCompatibility,
    [switch]$IncludeGameFlow,
    [switch]$TableViewportOnly,
    [string]$RunRoot = ''
)
$ErrorActionPreference = 'Stop'
if ($TableViewportOnly -and ($DenseLayout -or $IncludeGameFlow -or $HardAiCompatibility)) {
    throw 'TableViewportOnly cannot be combined with DenseLayout, IncludeGameFlow or HardAiCompatibility.'
}
$root = Split-Path -Parent $PSScriptRoot
Import-Module (Join-Path $root 'atf-test-android/tools/AndroidDeviceSetup.psm1') -Force
if ([string]::IsNullOrWhiteSpace($Profiles)) {
    $Profiles = if ($DenseLayout) {
        '720x1280@360,972x1728@440,1080x2448@440,1200x1920@320'
    } else {
        '720x1280@360,1080x1920@440,1080x2448@440,1200x1920@320'
    }
}

if ([string]::IsNullOrWhiteSpace($RunRoot)) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $RunRoot = "C:\work\whist_back\ui-reports\whist-lab\runs\$stamp-responsive-sanity"
}
$RunRoot = [IO.Path]::GetFullPath($RunRoot)
$sourceRoot = [IO.Path]::GetFullPath($root)
if ($RunRoot.StartsWith($sourceRoot + [IO.Path]::DirectorySeparatorChar,
        [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Reports must stay outside the J4 source tree.'
}
if (Test-Path -LiteralPath $RunRoot) {
    throw 'Choose a new run directory. Existing runs are immutable.'
}
if (-not (Test-Path -LiteralPath $MavenRepository -PathType Container)) {
    throw "Offline Maven repository is missing: $MavenRepository"
}
New-Item -ItemType Directory -Path $RunRoot | Out-Null
$artifacts = Join-Path $RunRoot 'android-artifacts'
$stage = Join-Path ([IO.Path]::GetTempPath()) ('ATF_J4_RESPONSIVE_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage,$artifacts | Out-Null

Copy-Item -LiteralPath (Join-Path $root 'pom.xml'),(Join-Path $root 'atf.yaml') -Destination $stage
Copy-Item -LiteralPath (Join-Path $root 'lib'),(Join-Path $root 'config') -Destination $stage -Recurse
[xml]$rootPom = [IO.File]::ReadAllText((Join-Path $root 'pom.xml'))
$modules = @($rootPom.project.modules.module | ForEach-Object { $_.Trim() })
if ($modules.Count -eq 0) { throw 'The Maven reactor declares no source modules to stage.' }
foreach ($module in $modules) {
    $source = Join-Path $root $module
    if (-not (Test-Path -LiteralPath (Join-Path $source 'pom.xml') -PathType Leaf)) {
        throw "The Maven reactor declares $module but its source POM is missing."
    }
    if ($module -eq 'atf-tests') {
        New-Item -ItemType Directory -Force -Path (Join-Path $stage $module) | Out-Null
        Copy-Item -LiteralPath (Join-Path $source 'pom.xml') -Destination (Join-Path $stage $module)
        continue
    }
    & robocopy $source (Join-Path $stage $module) /E /XD target build .git .gradle artifacts /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Could not stage source module declared by the reactor: $module" }
}
$leasePath = 'C:\work\whist_back\android-ui-lease.json'
$leaseId = ''
$leaseAcquired = $false
$state = [ordered]@{
    runRoot=$RunRoot
    buildRoot=$stage
    serial=$Serial
    profiles=$Profiles
    hands=$Hands
    includeGameFlow=$IncludeGameFlow.IsPresent
    tableViewportOnly=$TableViewportOnly.IsPresent
    status='STAGED'
    startedAt=[DateTimeOffset]::UtcNow.ToString('o')
}
$saved = @{}
foreach ($name in @('JAVA_HOME','Path','MAVEN_OPTS','ADB_SERVER_SOCKET','ATF_ANDROID_ADB',
        'ATF_ANDROID_SERIAL','ATF_ANDROID_TIMEOUT_SECONDS')) {
    $saved[$name] = [Environment]::GetEnvironmentVariable($name)
}

function Invoke-SanityMaven {
    param([string]$Label,[string[]]$Arguments)
    $log = Join-Path $RunRoot ($Label + '.log')
    Push-Location $stage
    try {
        & $Maven @Arguments *> $log
        $exitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($exitCode -ne 0) { throw "$Label failed. See $log" }
}

function Acquire-DeviceLease {
    $stream = [IO.File]::Open($leasePath,[IO.FileMode]::OpenOrCreate,
            [IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    try {
        $reader = [IO.StreamReader]::new($stream,[Text.Encoding]::UTF8,$true,1024,$true)
        $content = $reader.ReadToEnd()
        $reader.Dispose()
        if (-not [string]::IsNullOrWhiteSpace($content) -and ($content | ConvertFrom-Json).active) {
            throw 'The Android emulator is already leased. No UI action was sent.'
        }
        $leaseId = [guid]::NewGuid().ToString('N')
        $lease = @{
            active=$true
            owner='J4 responsive UI sanity'
            leaseId=$leaseId
            run=$RunRoot
            serial=$Serial
            startedAt=[DateTimeOffset]::UtcNow.ToString('o')
        }
        $bytes = [Text.Encoding]::UTF8.GetBytes(($lease | ConvertTo-Json))
        $stream.SetLength(0)
        $stream.Position = 0
        $stream.Write($bytes,0,$bytes.Length)
        $stream.Flush($true)
        $script:leaseId = $leaseId
        $script:leaseAcquired = $true
    } finally {
        $stream.Dispose()
    }
}

function Release-DeviceLease {
    if (-not $leaseAcquired -or -not (Test-Path -LiteralPath $leasePath)) { return }
    $stream = [IO.File]::Open($leasePath,[IO.FileMode]::Open,
            [IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    try {
        $reader = [IO.StreamReader]::new($stream,[Text.Encoding]::UTF8,$true,1024,$true)
        $current = $reader.ReadToEnd() | ConvertFrom-Json
        $reader.Dispose()
        if ($current.leaseId -eq $leaseId) {
            $current.active = $false
            $current | Add-Member -NotePropertyName updatedAt -NotePropertyValue ([DateTimeOffset]::UtcNow.ToString('o')) -Force
            $bytes = [Text.Encoding]::UTF8.GetBytes(($current | ConvertTo-Json))
            $stream.SetLength(0)
            $stream.Position = 0
            $stream.Write($bytes,0,$bytes.Length)
            $stream.Flush($true)
        }
    } finally {
        $stream.Dispose()
    }
}

try {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$($saved.Path)"
    $env:MAVEN_OPTS = '-Xms32m -Xmx768m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:ActiveProcessorCount=2'


$pom = [xml][IO.File]::ReadAllText((Join-Path $stage 'pom.xml'))
$namespace = [Xml.XmlNamespaceManager]::new($pom.NameTable)
$namespace.AddNamespace('m', 'http://maven.apache.org/POM/4.0.0')
$versions = [ordered]@{}
foreach ($property in @(
        @{Key='core'; Name='atf.core.version'},
        @{Key='reporting'; Name='atf.reporting.version'})) {
    $node = $pom.SelectSingleNode("/m:project/m:properties/m:$($property.Name)", $namespace)
    if ($null -eq $node -or [string]::IsNullOrWhiteSpace($node.InnerText)) {
        throw "The project POM does not declare $($property.Name)."
    }
    $versions[$property.Key] = $node.InnerText.Trim()
}
$libraryHashes = [ordered]@{}
foreach ($library in @(
        @{Key='core'; Artifact='atf-core'},
        @{Key='reporting'; Artifact='atf-reporting'})) {
    $version = $versions[$library.Key]
    $jar = "$($library.Artifact)-$version.jar"
    $source = Join-Path $stage "lib\$jar"
    $local = Join-Path $MavenRepository "atf\api\$($library.Artifact)\$version\$jar"
    foreach ($path in @($source, $local)) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Required $($library.Artifact) $version JAR is missing: $path"
        }
    }
    $sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
    $localHash = (Get-FileHash -LiteralPath $local -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($sourceHash -ne $localHash) {
        throw "The staged and Maven-cache $($library.Artifact) $version JARs differ."
    }
    $libraryHashes["$($library.Key)Version"] = $version
    $libraryHashes["$($library.Key)Source"] = $sourceHash
    $libraryHashes["$($library.Key)Maven"] = $localHash
}
$libraryHashes | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $RunRoot 'library-binaries.json') -Encoding utf8
    $common = @('-o','-f',(Join-Path $stage 'pom.xml'),'-pl','atf-test-android','-am','test','-Presponsive-sanity',
        "-Dmaven.repo.local=$MavenRepository",'-Dsurefire.failIfNoSpecifiedTests=false')
    Invoke-SanityMaven 'display-profile-components' ($common + @(
        '-Dtest=ResponsiveDisplayProfileComponentTest,AndroidUiDriverComponentTest,AndroidUiFlowReportPublisherTest,FrameFillAuditComponentTest,TableViewportAuditComponentTest',
        '-Datf.android.enabled=false'))

    Acquire-DeviceLease
    $env:ADB_SERVER_SOCKET = $AdbServerSocket
    $env:ATF_ANDROID_ADB = $AdbExecutable
    $env:ATF_ANDROID_SERIAL = $Serial
    $env:ATF_ANDROID_TIMEOUT_SECONDS = '90'
    Connect-AtfAndroidDevice -AdbExecutable $AdbExecutable -Serial $Serial -ServerSocket $AdbServerSocket -TimeoutSeconds 20 | Out-Null

    $package = & $AdbExecutable -s $Serial shell pm path ro.whistlab.app
    if ($LASTEXITCODE -ne 0 -or $package -notmatch 'package:') {
        throw 'Whist Lab must already be installed on the selected emulator.'
    }
    $api = (& $AdbExecutable -s $Serial shell getprop ro.build.version.sdk).Trim()
    $apkPath = (($package | Where-Object { $_ -like 'package:*' } | Select-Object -First 1) -replace '^package:','').Trim()
    $apkHash = ((& $AdbExecutable -s $Serial shell sha256sum $apkPath) -split '\s+')[0]
    if ([string]::IsNullOrWhiteSpace($apkHash)) { throw 'Could not fingerprint the installed APK.' }
    [ordered]@{package='ro.whistlab.app';installedPath=$apkPath;sha256=$apkHash;api=$api;serial=$Serial} |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $RunRoot 'apk-provenance.json') -Encoding utf8

    $state.status = 'ANDROID_RUNNING'
    $state | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $RunRoot 'WORKING_STATE.json') -Encoding utf8
    $testClass = if ($DenseLayout) { 'WhistDenseLayoutE2eTest' } else { 'WhistResponsivePlaySanityE2eTest' }
    $testLabel = if ($DenseLayout) { 'dense-layout-sanity' } else { 'responsive-ui-sanity' }
    Invoke-SanityMaven $testLabel ($common + @(
        "-Dtest=$testClass",
        '-Datf.android.enabled=true',
        "-Datf.android.artifacts=$artifacts",
        "-Datf.android.serial=$Serial",
        "-Datf.android.api=$api",
        "-Datf.android.sanity.profiles=$Profiles",
        "-Datf.android.sanity.hands=$Hands",
        "-Datf.android.sanity.includeGameFlow=$($IncludeGameFlow.IsPresent.ToString().ToLowerInvariant())",
        "-Datf.android.sanity.tableViewportOnly=$($TableViewportOnly.IsPresent.ToString().ToLowerInvariant())",
        "-Datf.android.sanity.hardAiCompatibility=$($HardAiCompatibility.IsPresent.ToString().ToLowerInvariant())"))

    $report = if ($DenseLayout) {
        Join-Path $artifacts 'dense-layout\dense-layout.html'
    } else {
        Join-Path $artifacts 'responsive-ui-sanity\responsive-ui-sanity.html'
    }
    if (-not (Test-Path -LiteralPath $report -PathType Leaf)) {
        throw "The E2E report was not published: $report"
    }
    $state.status = 'PASS'
    $state.report = $report
    $state.completedAt = [DateTimeOffset]::UtcNow.ToString('o')
} catch {
    $state.status = 'FAIL'
    $state.failure = $_.ToString()
    throw
} finally {
    try { Release-DeviceLease }
    catch { $state.leaseReleaseFailure = $_.ToString(); $state.status = 'FAIL' }
    $state | ConvertTo-Json -Depth 5 |
        Set-Content -LiteralPath (Join-Path $RunRoot 'WORKING_STATE.json') -Encoding utf8
    foreach ($name in $saved.Keys) {
        [Environment]::SetEnvironmentVariable($name,$saved[$name])
    }
}

Write-Output $state.report
