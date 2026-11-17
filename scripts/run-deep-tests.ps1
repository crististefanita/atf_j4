param(
    [string]$AdbExecutable = 'C:\tools\whist-android-sdk\platform-tools\adb.exe',
    [string]$Serial = '127.0.0.1:5555',
    [string]$AdbServerSocket = 'tcp:192.168.56.1:5038',
    [int]$AdbTimeoutSeconds = 90,
    [string]$WhistLabProject = '',
    [string]$EvidenceRoot = 'C:\work\whist_back\deep-app-test',
    [string]$AndroidSdk = 'C:\tools\whist-android-sdk'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Import-Module (Join-Path $root 'atf-test-android/tools/AndroidDeviceSetup.psm1') -Force
$defaultApp = Join-Path $root 'whist-lab-app'
$appSource = if ($WhistLabProject) { [IO.Path]::GetFullPath($WhistLabProject) } else { $defaultApp }
if (-not (Test-Path -LiteralPath (Join-Path $appSource 'gradlew.bat') -PathType Leaf)) {
    throw "Whist Lab Gradle project not found: $appSource"
}
$evidenceRoot = [IO.Path]::GetFullPath($EvidenceRoot)
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$runRoot = Join-Path $evidenceRoot (Join-Path 'runs' $runId)
foreach ($source in @($root, $appSource)) {
    $prefix = $source.TrimEnd([char]'\',[char]'/') + [IO.Path]::DirectorySeparatorChar
    if ($runRoot.Equals($source, [StringComparison]::OrdinalIgnoreCase) -or
        $runRoot.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'E2E build and evidence must stay outside the source projects.'
    }
}
if (Test-Path -LiteralPath $runRoot) { throw 'Choose a fresh E2E evidence directory.' }
$stage = Join-Path $runRoot 'work'
$scratch = Join-Path $runRoot 'tmp'
$buildApp = Join-Path $stage 'whist-lab-app'
$componentResults = Join-Path $runRoot 'components'
$evidenceDir = Join-Path $runRoot 'evidence'
$logsDir = Join-Path $runRoot 'logs'
$componentLogsDir = Join-Path $componentResults 'logs'
$report = Join-Path $runRoot 'final-extent-report.html'
$modules = @(([xml](Get-Content -LiteralPath "$root\pom.xml" -Raw)).project.modules.module)
$failures = [System.Collections.Generic.List[string]]::new()
$oldEnvironment = @{
    ADB_SERVER_SOCKET = $env:ADB_SERVER_SOCKET
    ATF_ANDROID_ADB = $env:ATF_ANDROID_ADB
    ATF_ANDROID_SERIAL = $env:ATF_ANDROID_SERIAL
    ATF_ANDROID_TIMEOUT_SECONDS = $env:ATF_ANDROID_TIMEOUT_SECONDS
    ANDROID_HOME = $env:ANDROID_HOME
    ANDROID_SDK_ROOT = $env:ANDROID_SDK_ROOT
    TEMP = $env:TEMP
    TMP = $env:TMP
}

function Invoke-MavenAndSaveResults {
    param(
        [string]$Module,
        [string[]]$MavenArguments,
        [string]$Destination
    )

    $started = Get-Date
    $log = Join-Path $componentLogsDir ($Module + '.log')
    Write-Host "Running $Module ..."
    & mvn @MavenArguments *> $log
    $result = $LASTEXITCODE
    $source = Join-Path (Join-Path $stage $Module) 'target\surefire-reports'
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    if (Test-Path -LiteralPath $source) {
        Get-ChildItem -LiteralPath $source -File |
            Where-Object {
                ($_.Name -like 'TEST-*.xml' -or $_.Name -like '*.txt') -and $_.LastWriteTime -ge $started
            } |
            Copy-Item -Destination $Destination -Force
    }
    if ($result -ne 0) {
        $failures.Add("$Module exited $result; see $log")
        Write-Warning "$Module failed; continuing so the final report can include its results."
    }
}

New-Item -ItemType Directory -Force -Path $stage,$scratch,$componentResults,$evidenceDir,$logsDir,$componentLogsDir | Out-Null
$inputs = Join-Path $runRoot 'source-inputs.json'
& (Join-Path $PSScriptRoot 'Get-AtfSourceInputs.ps1') -SourceRoot $root -AsJson |
    Set-Content -LiteralPath $inputs -Encoding utf8
& (Join-Path $PSScriptRoot 'Copy-AtfSourceInputs.ps1') -SourceRoot $root -Destination $stage -InputManifest $inputs
if ($appSource -ine $defaultApp) {
    $buildApp = Join-Path $stage 'custom-whist-app'
    & robocopy $appSource $buildApp /E /XJ /XD target build .git .gradle artifacts /NFL /NDL /NJH /NJS /NP *> (Join-Path $logsDir 'copy-app.log')
    if ($LASTEXITCODE -ge 8) { throw "Could not stage Whist Lab sources: $appSource" }
}
Push-Location $stage
try {
    $env:TEMP = $scratch
    $env:TMP = $scratch
    if (-not (Test-Path -LiteralPath $AdbExecutable)) {
        throw "ADB executable not found: $AdbExecutable"
    }
    $env:ADB_SERVER_SOCKET = $AdbServerSocket
    $env:ATF_ANDROID_ADB = $AdbExecutable
    $env:ATF_ANDROID_SERIAL = $Serial
    $env:ATF_ANDROID_TIMEOUT_SECONDS = [string]$AdbTimeoutSeconds
    Connect-AtfAndroidDevice -AdbExecutable $AdbExecutable -Serial $Serial `
        -ServerSocket $AdbServerSocket -TimeoutSeconds 15 | Out-Null

    Write-Host 'Installing the Whist reactor artifacts with tests skipped ...'
    & mvn -DskipTests install *> (Join-Path $logsDir 'install.log')
    if ($LASTEXITCODE -ne 0) {
        throw "Maven install failed; see $(Join-Path $logsDir 'install.log')"
    }

    $env:ANDROID_HOME = $AndroidSdk
    $env:ANDROID_SDK_ROOT = $AndroidSdk
    $buildLog = Join-Path $logsDir 'whist-lab-android-build.log'
    Push-Location $buildApp
    try {
        & .\gradlew.bat --no-daemon --console=plain :game-core:test :app:assembleDebug *> $buildLog
        if ($LASTEXITCODE -ne 0) {
            throw "Whist Lab Android build failed; see $buildLog"
        }
    }
    finally {
        Pop-Location
    }
    $whistLabApk = Join-Path $buildApp 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $whistLabApk)) {
        throw "Whist Lab APK was not produced: $whistLabApk"
    }
    $apkInstallLog = Join-Path $logsDir 'whist-lab-apk-install.log'
    & $AdbExecutable -s $Serial install -r $whistLabApk *> $apkInstallLog
    if ($LASTEXITCODE -ne 0) {
        throw "Whist Lab APK installation failed; see $apkInstallLog"
    }

    foreach ($module in $modules) {
        if ($module -eq 'atf-test-android') {
            $args = @('-pl', $module, '-Dtest=AndroidComponentSuite,AndroidGameplayE2eSuite', 'test')
            $args += @('-Datf.android.enabled=true', "-Datf.android.commandTimeoutSeconds=$AdbTimeoutSeconds",
                "-Datf.android.artifacts=$evidenceDir")
        } else {
            $args = @('-pl', $module, 'test')
        }
        Invoke-MavenAndSaveResults -Module $module -MavenArguments $args -Destination (Join-Path $componentResults $module)
    }

    Write-Host 'Publishing the aggregate Extent report with inline screenshots ...'
    $publisherLog = Join-Path $logsDir 'extent-publisher.log'
    & mvn -pl atf-tests test '-Dtest=DeepSuiteExtentReportTest' '-Datf.deep.publish=true' `
        "-Datf.deep.resultsDir=$componentResults" "-Datf.deep.evidenceDir=$evidenceDir" `
        "-Datf.deep.reportPath=$report" *> $publisherLog
    if ($LASTEXITCODE -ne 0) {
        $failures.Add("Extent publisher exited $LASTEXITCODE; see $publisherLog")
        Write-Warning 'Extent publisher failed; inspect its log for the missing input or renderer error.'
    }

    if (Test-Path -LiteralPath $report) {
        Write-Host "Extent report: $report"
    }
    Write-Host "Run evidence: $runRoot"
    if ($failures.Count -gt 0) {
        $failures | ForEach-Object { Write-Error $_ -ErrorAction Continue }
        exit 1
    }
}
finally {
    Pop-Location
    $env:ADB_SERVER_SOCKET = $oldEnvironment.ADB_SERVER_SOCKET
    $env:ATF_ANDROID_ADB = $oldEnvironment.ATF_ANDROID_ADB
    $env:ATF_ANDROID_SERIAL = $oldEnvironment.ATF_ANDROID_SERIAL
    $env:ATF_ANDROID_TIMEOUT_SECONDS = $oldEnvironment.ATF_ANDROID_TIMEOUT_SECONDS
    $env:ANDROID_HOME = $oldEnvironment.ANDROID_HOME
    $env:ANDROID_SDK_ROOT = $oldEnvironment.ANDROID_SDK_ROOT
    $env:TEMP = $oldEnvironment.TEMP
    $env:TMP = $oldEnvironment.TMP
}
