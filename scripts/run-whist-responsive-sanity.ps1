param(
    [string]$Profiles = '720x1280@360,1080x1920@440,1080x2448@440,1200x1920@320',
    [ValidateRange(1,3)][int]$HandsPerProfile = 2,
    [string]$Serial = '127.0.0.1:5555',
    [string]$AdbServerSocket = 'tcp:192.168.56.1:5038',
    [string]$AdbExecutable = 'C:\tools\whist-android-sdk\platform-tools\adb.exe',
    [string]$Maven = 'C:\tools\Maven\apache-maven-3.10.0\bin\mvn.cmd',
    [string]$MavenRepository = 'C:\Users\Cristi\.m2\repository',
    [string]$JavaHome = 'C:\tools\Java21\jdk-21.0.12.1+1',
    [string]$RunRoot = ''
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Import-Module (Join-Path $PSScriptRoot 'AndroidDeviceSetup.psm1') -Force

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
$modules = @('atf-test-kit','atf-mock','atf-validation','atf-console','atf-android','atf-test-android')
foreach ($module in $modules) {
    & robocopy (Join-Path $root $module) (Join-Path $stage $module) /E /XD target build .git .gradle artifacts /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Could not stage source module $module" }
}

New-Item -ItemType Directory -Force -Path (Join-Path $stage 'atf-tests') | Out-Null
Copy-Item -LiteralPath (Join-Path $root 'atf-tests\pom.xml') -Destination (Join-Path $stage 'atf-tests\pom.xml')
foreach ($tree in @('whist-lab-app\game-core\src\main\java','whist-lab-app\ai-lab\src\main\java')) {
    $source = Join-Path $root $tree
    $target = Join-Path $stage $tree
    & robocopy $source $target /E /XD target build .git .gradle artifacts /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Could not stage game source $tree" }
}

$leasePath = 'C:\work\whist_back\android-ui-lease.json'
$leaseId = ''
$leaseAcquired = $false
$state = [ordered]@{
    runRoot=$RunRoot
    buildRoot=$stage
    serial=$Serial
    profiles=$Profiles
    handsPerProfile=$HandsPerProfile
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
    & $Maven @Arguments *> $log
    if ($LASTEXITCODE -ne 0) { throw "$Label failed. See $log" }
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


$coreSource = Join-Path $stage 'lib\atf-core-1.0.0-c3.jar'
$reportingSource = Join-Path $stage 'lib\atf-reporting-1.0.0-j18.jar'
$coreLocal = Join-Path $MavenRepository 'atf\api\atf-core\1.0.0-c3\atf-core-1.0.0-c3.jar'
$reportingLocal = Join-Path $MavenRepository 'atf\api\atf-reporting\1.0.0-j18\atf-reporting-1.0.0-j18.jar'
$libraryHashes = [ordered]@{
    coreSource=(Get-FileHash $coreSource).Hash.ToLowerInvariant()
    coreMaven=(Get-FileHash $coreLocal).Hash.ToLowerInvariant()
    reportingSource=(Get-FileHash $reportingSource).Hash.ToLowerInvariant()
    reportingMaven=(Get-FileHash $reportingLocal).Hash.ToLowerInvariant()
}
if($libraryHashes.coreSource -ne $libraryHashes.coreMaven -or
        $libraryHashes.reportingSource -ne $libraryHashes.reportingMaven) {
    throw 'The Maven cache must contain the same Core and Reporting JARs as J4 lib.'
}
$libraryHashes | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $RunRoot 'library-binaries.json') -Encoding utf8
    $common = @('-o','-f',(Join-Path $stage 'pom.xml'),'-pl','atf-test-android','-am','test',
        "-Dmaven.repo.local=$MavenRepository",'-Dsurefire.failIfNoSpecifiedTests=false')
    Invoke-SanityMaven 'display-profile-components' ($common + @(
        '-Dtest=ResponsiveDisplayProfileComponentTest,AndroidUiDriverComponentTest',
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
    Invoke-SanityMaven 'responsive-ui-sanity' ($common + @(
        '-Dtest=WhistResponsivePlaySanityE2eTest',
        '-Datf.android.enabled=true',
        "-Datf.android.artifacts=$artifacts",
        "-Datf.android.serial=$Serial",
        "-Datf.android.api=$api",
        "-Datf.android.sanity.profiles=$Profiles",
        "-Datf.android.sanity.handsPerProfile=$HandsPerProfile"))

    $report = Join-Path $artifacts 'responsive-ui-sanity\responsive-ui-sanity.html'
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
