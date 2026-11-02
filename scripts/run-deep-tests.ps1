param(
    [string]$AdbExecutable = 'X:\scrcpy-win64-v5.0.1\adb.exe',
    [string]$Serial = 'emulator-5554',
    [string]$AdbServerSocket = 'tcp:192.168.56.1:5038',
    [int]$AdbTimeoutSeconds = 90,
    [string]$WhistLabProject = '',
    [string]$EvidenceRoot = 'C:\work\whist_back\deep-app-test',
    [string]$AndroidSdk = 'C:\tools\whist-android-sdk'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($WhistLabProject)) {
    $WhistLabProject = Join-Path $root 'whist-lab-app'
}
$evidenceRoot = $EvidenceRoot
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$runRoot = Join-Path $evidenceRoot (Join-Path 'runs' $runId)
$componentResults = Join-Path $runRoot 'components'
$evidenceDir = Join-Path $runRoot 'evidence'
$logsDir = Join-Path $runRoot 'logs'
$componentLogsDir = Join-Path $componentResults 'logs'
$report = Join-Path $runRoot 'final-extent-report.html'
$modules = @('atf-test-kit', 'atf-mock', 'atf-validation', 'atf-console', 'atf-android', 'atf-tests', 'atf-test-android')
$failures = [System.Collections.Generic.List[string]]::new()
$oldEnvironment = @{
    ADB_SERVER_SOCKET = $env:ADB_SERVER_SOCKET
    ATF_ANDROID_ADB = $env:ATF_ANDROID_ADB
    ATF_ANDROID_SERIAL = $env:ATF_ANDROID_SERIAL
    ATF_ANDROID_TIMEOUT_SECONDS = $env:ATF_ANDROID_TIMEOUT_SECONDS
    ANDROID_HOME = $env:ANDROID_HOME
    ANDROID_SDK_ROOT = $env:ANDROID_SDK_ROOT
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
    $source = Join-Path (Join-Path $root $Module) 'target\surefire-reports'
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

New-Item -ItemType Directory -Force -Path $runRoot,$componentResults,$evidenceDir,$logsDir,$componentLogsDir | Out-Null
Push-Location $root
try {
    if (-not (Test-Path -LiteralPath $AdbExecutable)) {
        throw "ADB executable not found: $AdbExecutable"
    }
    $env:ADB_SERVER_SOCKET = $AdbServerSocket
    $env:ATF_ANDROID_ADB = $AdbExecutable
    $env:ATF_ANDROID_SERIAL = $Serial
    $env:ATF_ANDROID_TIMEOUT_SECONDS = [string]$AdbTimeoutSeconds
    & $AdbExecutable -s $Serial get-state | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "ADB cannot reach device '$Serial' through $AdbServerSocket"
    }

    Write-Host 'Installing the J4 reactor artifacts with tests skipped ...'
    & mvn -DskipTests install *> (Join-Path $logsDir 'install.log')
    if ($LASTEXITCODE -ne 0) {
        throw "Maven install failed; see $(Join-Path $logsDir 'install.log')"
    }

    if (-not (Test-Path -LiteralPath (Join-Path $WhistLabProject 'gradlew.bat'))) {
        throw "Whist Lab Gradle project not found: $WhistLabProject"
    }
    $env:ANDROID_HOME = $AndroidSdk
    $env:ANDROID_SDK_ROOT = $AndroidSdk
    $buildLog = Join-Path $logsDir 'whist-lab-android-build.log'
    Push-Location $WhistLabProject
    try {
        & .\gradlew.bat --no-daemon --console=plain :game-core:test :app:assembleDebug *> $buildLog
        if ($LASTEXITCODE -ne 0) {
            throw "Whist Lab Android build failed; see $buildLog"
        }
    }
    finally {
        Pop-Location
    }
    $whistLabApk = Join-Path $WhistLabProject 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $whistLabApk)) {
        throw "Whist Lab APK was not produced: $whistLabApk"
    }
    $apkInstallLog = Join-Path $logsDir 'whist-lab-apk-install.log'
    & $AdbExecutable -s $Serial install -r $whistLabApk *> $apkInstallLog
    if ($LASTEXITCODE -ne 0) {
        throw "Whist Lab APK installation failed; see $apkInstallLog"
    }

    foreach ($module in $modules) {
        if ($module -eq 'atf-android') {
            $args = @('-pl', $module, '-Dtest=AndroidComponentSuite', 'test')
        } elseif ($module -eq 'atf-test-android') {
            $args = @('-pl', $module, '-Dtest=AndroidGameplayE2eSuite', 'test')
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
}
