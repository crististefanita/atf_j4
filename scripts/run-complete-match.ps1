param(
    [string]$AdbExecutable = 'C:\tools\whist-android-sdk\platform-tools\adb.exe',
    [string]$Serial = 'emulator-5554',
    [string]$AdbServerSocket = 'tcp:192.168.56.1:5038',
    [string]$AndroidSdk = 'C:\tools\whist-android-sdk',
    [string]$EvidenceRoot = 'C:\work\whist_back\deep-app-test',
    [string]$RunRoot = '',
    [string]$ReportHeap = '3g',
    [switch]$ReplayOnly,
    [switch]$ContinueOriginal
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if([string]::IsNullOrWhiteSpace($RunRoot)) {
    $RunRoot=Join-Path $EvidenceRoot (Join-Path 'runs' ((Get-Date -Format 'yyyyMMdd-HHmmss')+'-full-match'))
}
$RunRoot=[IO.Path]::GetFullPath($RunRoot)
$evidence=Join-Path $RunRoot 'evidence'
$components=Join-Path $RunRoot 'components'
New-Item -ItemType Directory -Force $evidence,$components | Out-Null
$saved=@{}
foreach($name in @('ADB_SERVER_SOCKET','ATF_ANDROID_ADB','ATF_ANDROID_SERIAL','ATF_ANDROID_TIMEOUT_SECONDS','ANDROID_HOME','ANDROID_SDK_ROOT')) {
    $saved[$name]=[Environment]::GetEnvironmentVariable($name)
}
function Invoke-MatchStage {
    param([string]$Name,[string[]]$Arguments)
    $log=Join-Path $RunRoot ($Name+'.log')
    Write-Host "Running $Name; log: $log"
    & mvn @Arguments *> $log
    if($LASTEXITCODE -ne 0){throw "$Name failed; retained observations and trace are in $RunRoot. See $log"}
}
function Save-MatchXml {
    param([string]$Module,[string[]]$Classes)
    foreach($class in $Classes) {
        Copy-Item -LiteralPath (Join-Path $root "$Module\target\surefire-reports\TEST-$class.xml") -Destination $components -Force
    }
}
Push-Location $root
try {
    $env:ADB_SERVER_SOCKET=$AdbServerSocket
    $env:ATF_ANDROID_ADB=$AdbExecutable
    $env:ATF_ANDROID_SERIAL=$Serial
    $env:ATF_ANDROID_TIMEOUT_SECONDS='90'
    $env:ANDROID_HOME=$AndroidSdk
    $env:ANDROID_SDK_ROOT=$AndroidSdk
    & $AdbExecutable -s $Serial get-state | Out-Null
    if($LASTEXITCODE -ne 0){throw 'Android device is not reachable'}
    Invoke-MatchStage 'bootstrap-current-binaries' @('-q','-DskipTests','install')
    Get-ChildItem -LiteralPath (Join-Path $root 'lib') -Filter '*.jar' | Get-FileHash -Algorithm SHA256 |
        Select-Object Path,Algorithm,Hash | ConvertTo-Json | Set-Content (Join-Path $RunRoot 'library-binaries.json')
    & $AdbExecutable -s $Serial shell dumpsys package com.qsoft.android.whist |
        Select-String 'versionCode=|versionName=' | Set-Content (Join-Path $RunRoot 'original-package-version.txt')
    Invoke-MatchStage 'complete-report-value-controls' @('-q','-pl','atf-tests','test','-Dtest=ReportTextValueMatcherTest')
    Save-MatchXml 'atf-tests' @('atf.tests.ReportTextValueMatcherTest')
    Invoke-MatchStage 'android-adapter-components' @('-q','-pl','atf-android','test','-Dtest=AndroidComponentSuite')
    Save-MatchXml 'atf-android' @('atf.android.AdbClientContractTest','atf.android.evidence.AndroidScreenshotEvidenceComponentTest')
    Invoke-MatchStage 'observation-components' @('-q','-pl','atf-test-android','test','-Dtest=AndroidHierarchyFreshnessTest,OriginalWhistStateTest,OriginalMatchOracleTest,OriginalContinuationBoundaryTest,WhistLabContinuationTest')
    Save-MatchXml 'atf-test-android' @('atf.android.e2e.AndroidHierarchyFreshnessTest','atf.android.e2e.OriginalWhistStateTest','atf.android.e2e.OriginalMatchOracleTest','atf.android.e2e.OriginalContinuationBoundaryTest','atf.android.e2e.WhistLabContinuationTest')
    Push-Location (Join-Path $root 'whist-lab-app')
    try {
        & .\gradlew.bat --no-daemon --console=plain :game-core:test :game-core:aiTournamentTest :app:assembleDebug "-Pwhist.componentEvidence=$evidence\romanian-components" *> (Join-Path $RunRoot 'whist-lab-build.log')
        if($LASTEXITCODE -ne 0){throw "Whist Lab build failed; see $RunRoot\whist-lab-build.log"}
    } finally { Pop-Location }
    $coreResults=Join-Path $components 'game-core'
    New-Item -ItemType Directory -Force $coreResults | Out-Null
    Copy-Item -Path (Join-Path $root 'whist-lab-app\game-core\build\test-results\test\TEST-*.xml') -Destination $coreResults -Force
    Copy-Item -Path (Join-Path $root 'whist-lab-app\game-core\build\test-results\aiTournamentTest\TEST-*.xml') -Destination $coreResults -Force
    Copy-Item -LiteralPath (Join-Path $evidence 'romanian-components\ai-tournament.properties') -Destination (Join-Path $evidence 'romanian-ai-tournament.properties') -Force
    & $AdbExecutable -s $Serial install -r (Join-Path $root 'whist-lab-app\app\build\outputs\apk\debug\app-debug.apk') *> (Join-Path $RunRoot 'lab-install.log')
    if($LASTEXITCODE -ne 0){throw "Whist Lab install failed; see $RunRoot\lab-install.log"}
    if(-not $ReplayOnly) {
        $originalArguments=@('-q','-pl','atf-test-android','test','-Dtest=CompleteOriginalMatchE2eTest','-Datf.android.enabled=true','-Datf.android.fullMatch=true',"-Datf.match.evidenceDir=$evidence")
        if($ContinueOriginal){$originalArguments+='-Datf.match.continue=true'}
        Invoke-MatchStage 'original-match-test' $originalArguments
        Save-MatchXml 'atf-test-android' @('atf.android.e2e.CompleteOriginalMatchE2eTest')
    }
    Invoke-MatchStage 'original-recorded-audit' @('-q','-pl','atf-test-android','test','-Dtest=RecordedOriginalMatchAuditTest','-Datf.match.review=true',"-Datf.match.evidenceDir=$evidence")
    Save-MatchXml 'atf-test-android' @('atf.android.e2e.RecordedOriginalMatchAuditTest')
    Push-Location (Join-Path $root 'whist-lab-app')
    try {
        & .\gradlew.bat --no-daemon --console=plain :game-core:originalReplayTest "-Pwhist.originalReplay=$evidence\original-match.properties" *> (Join-Path $RunRoot 'game-core-original-replay.log')
        if($LASTEXITCODE -ne 0){throw "Whist Lab engine diverged from original; see $RunRoot\game-core-original-replay.log"}
    } finally { Pop-Location }
    $replayResults=Join-Path $components 'game-core-replay'
    New-Item -ItemType Directory -Force $replayResults | Out-Null
    Copy-Item -Path (Join-Path $root 'whist-lab-app\game-core\build\test-results\originalReplayTest\TEST-*.xml') -Destination $replayResults -Force
    Invoke-MatchStage 'whist-lab-replay-test' @('-q','-pl','atf-test-android','test','-Dtest=CompleteWhistLabMatchE2eTest','-Datf.android.enabled=true','-Datf.android.fullMatch=true',"-Datf.match.evidenceDir=$evidence")
    Save-MatchXml 'atf-test-android' @('atf.android.e2e.CompleteWhistLabMatchE2eTest')
    Invoke-MatchStage 'romanian-expert-mobile' @('-q','-pl','atf-test-android','test','-Dtest=WhistLabRomanianExpertE2eTest','-Datf.android.enabled=true',"-Datf.match.evidenceDir=$evidence")
    Save-MatchXml 'atf-test-android' @('atf.android.e2e.WhistLabRomanianExpertE2eTest')
    $report=Join-Path $RunRoot 'final-extent-report.html'
    Invoke-MatchStage 'full-match-extent-publisher' @('-q','-pl','atf-tests','test','-Dtest=FullMatchExtentReportTest','-Datf.match.publish=true',"-Datf.test.heap.max=$ReportHeap","-Datf.match.evidenceDir=$evidence","-Datf.match.reportPath=$report")
    Write-Host "Extent report: $report"
} finally {
    Pop-Location
    foreach($name in $saved.Keys){[Environment]::SetEnvironmentVariable($name,$saved[$name])}
}
