param(
    [string]$RunRoot = '',
    [string]$Serial = '127.0.0.1:5555',
    [string]$AdbExecutable = 'C:\tools\whist-android-sdk\platform-tools\adb.exe',
    [string]$AdbServerSocket = 'tcp:192.168.56.1:5038',
    [string]$AndroidSdk = 'C:\tools\whist-android-sdk',
    [string]$ReportHeap = '3g'
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Import-Module (Join-Path $PSScriptRoot 'AndroidDeviceSetup.psm1') -Force
if([string]::IsNullOrWhiteSpace($RunRoot)){$RunRoot='C:\work\whist_back\romanian-expert\'+(Get-Date -Format 'yyyyMMdd-HHmmss')}
$RunRoot=[IO.Path]::GetFullPath($RunRoot)
if(Test-Path -LiteralPath $RunRoot){throw 'Use a fresh run folder; completed evidence must stay immutable.'}
$stage=Join-Path $env:TEMP ('ATF_J4_ROMANIAN_'+[guid]::NewGuid().ToString('N'))
$component=Join-Path $RunRoot 'component-acceptance'
$evidence=Join-Path $RunRoot 'evidence\android'
New-Item -ItemType Directory -Force $stage,$evidence,"$RunRoot\components","$component\components","$component\evidence","$component\coverage" | Out-Null
Copy-Item -LiteralPath (Join-Path $root 'pom.xml') -Destination $stage
Copy-Item -LiteralPath (Join-Path $root 'lib') -Destination $stage -Recurse
foreach($module in @('atf-test-kit','atf-mock','atf-validation','atf-console','atf-test-android','atf-tests','whist-lab-app')){
    & robocopy (Join-Path $root $module) (Join-Path $stage $module) /E /XD target build .git .gradle artifacts /NFL /NDL /NJH /NJS /NP | Out-Null
    if($LASTEXITCODE -ge 8){throw "Could not stage $module"}
}
$saved=@{}
foreach($name in @('ADB_SERVER_SOCKET','ATF_ANDROID_ADB','ATF_ANDROID_SERIAL','ATF_ANDROID_TIMEOUT_SECONDS','ANDROID_HOME','ANDROID_SDK_ROOT')){$saved[$name]=[Environment]::GetEnvironmentVariable($name)}
function Invoke-Stage([string]$Name,[scriptblock]$Command){
    Write-Host "Running $Name; log: $RunRoot\$Name.log"
    & $Command *> (Join-Path $RunRoot "$Name.log")
    if($LASTEXITCODE -ne 0){throw "$Name failed; retained diagnostics: $RunRoot\$Name.log"}
}
function Save-Xml([string]$Module,[string[]]$Classes){foreach($class in $Classes){Copy-Item -LiteralPath "$stage\$Module\target\surefire-reports\TEST-$class.xml" -Destination "$RunRoot\components"}}
$state=[ordered]@{run=$RunRoot;stage=$stage;status='COMPONENTS_RUNNING';startedAt=(Get-Date -Format o);serial=$Serial}
$state | ConvertTo-Json | Set-Content -LiteralPath "$RunRoot\WORKING_STATE.json"
Push-Location $stage
try{
    $env:ANDROID_HOME=$AndroidSdk;$env:ANDROID_SDK_ROOT=$AndroidSdk
    $env:ADB_SERVER_SOCKET=$AdbServerSocket;$env:ATF_ANDROID_ADB=$AdbExecutable;$env:ATF_ANDROID_SERIAL=$Serial;$env:ATF_ANDROID_TIMEOUT_SECONDS='90'
    Push-Location "$stage\whist-lab-app"
    try{Invoke-Stage 'game-core-and-apk' { & .\gradlew.bat --no-daemon --console=plain :game-core:test :game-core:aiTournamentTest :game-core:jacocoTestReport :app:assembleDebug "-Pwhist.componentEvidence=$RunRoot\evidence\components" }}finally{Pop-Location}
    Copy-Item -Path "$stage\whist-lab-app\game-core\build\test-results\test\TEST-*.xml","$stage\whist-lab-app\game-core\build\test-results\aiTournamentTest\TEST-*.xml" -Destination "$component\components"
    Copy-Item -Path "$component\components\TEST-*.xml" -Destination "$RunRoot\components"
    Copy-Item -Path "$RunRoot\evidence\components\*" -Destination "$component\evidence" -Recurse
    Copy-Item -Path "$stage\whist-lab-app\game-core\build\reports\jacoco\test\*" -Destination "$component\coverage" -Recurse
    Invoke-Stage 'bootstrap-binaries' { & mvn -q -DskipTests install }
    Invoke-Stage 'android-report-components' { & mvn -q -pl atf-test-android,atf-tests -am test '-Dtest=AdbClientContractTest,AndroidScreenshotEvidenceComponentTest,CoreReportingConsumerContractTest,AndroidInlineScreenshotReportContractTest,ReportTextValueMatcherTest,RomanianMatchOracleTest' '-Dsurefire.failIfNoSpecifiedTests=false' }
    Save-Xml 'atf-test-android' @('atf.android.AdbClientContractTest','atf.android.evidence.AndroidScreenshotEvidenceComponentTest')
    Save-Xml 'atf-tests' @('atf.tests.CoreReportingConsumerContractTest','atf.tests.AndroidInlineScreenshotReportContractTest','atf.tests.ReportTextValueMatcherTest')
    Save-Xml 'atf-test-android' @('atf.android.e2e.RomanianMatchOracleTest')
    Invoke-Stage 'component-extent-publisher' { & mvn -q -pl atf-tests test '-Dtest=RuleComponentExtentReportTest' '-Datf.rules.publish=true' "-Datf.rules.runDir=$component" }
    Connect-AtfAndroidDevice -AdbExecutable $AdbExecutable -Serial $Serial `
        -ServerSocket $AdbServerSocket -TimeoutSeconds 15 | Out-Null
    $apk="$stage\whist-lab-app\app\build\outputs\apk\debug\app-debug.apk"
    Get-ChildItem -LiteralPath "$stage\lib" -File | Get-FileHash -Algorithm SHA256 | Select-Object Path,Hash | ConvertTo-Json | Set-Content "$RunRoot\library-binaries.json"
    [ordered]@{serial=$Serial;adbServer=$AdbServerSocket;api=(& $AdbExecutable -s $Serial shell getprop ro.build.version.sdk);display=(& $AdbExecutable -s $Serial shell wm size);density=(& $AdbExecutable -s $Serial shell wm density);scrcpy='Not required; headless ADB control'} | ConvertTo-Json | Set-Content "$RunRoot\environment.json"
    Invoke-Stage 'install-whist-lab' { & $AdbExecutable -s $Serial install -r $apk }
    $installedPath=(((& $AdbExecutable -s $Serial shell pm path ro.whistlab.app) | Where-Object {$_ -like 'package:*'} | Select-Object -First 1) -replace '^package:','').Trim()
    if([string]::IsNullOrWhiteSpace($installedPath)){throw 'Installed Whist Lab APK path is missing'}
    $installedHash=((& $AdbExecutable -s $Serial shell sha256sum $installedPath) -split '\s+')[0]
    $builtHash=(Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
    if($installedHash -ne $builtHash){throw 'Installed APK does not match the accepted component build'}
    Copy-Item -LiteralPath $apk -Destination "$RunRoot\whist-lab-tested.apk"
    [ordered]@{sha256=$builtHash;installedSha256=$installedHash;installedPath=$installedPath;archivedApk="$RunRoot\whist-lab-tested.apk";scope='Same APK for the complete Romanian Whist Android game at the declared Normal or Hard level';installedAt=(Get-Date -Format o)} | ConvertTo-Json | Set-Content "$RunRoot\apk-provenance.json"
    $state.status='ANDROID_RUNNING';$state | ConvertTo-Json | Set-Content "$RunRoot\WORKING_STATE.json"
    $gameFailure=$null
    try{Invoke-Stage 'romanian-live-match-android' { & mvn -q -pl atf-test-android test '-Dtest=CompleteRomanianExpertE2eTest' '-Datf.android.enabled=true' '-Datf.android.fullMatch=true' "-Datf.match.evidenceDir=$evidence" }}catch{$gameFailure=$_}
    Save-Xml 'atf-test-android' @('atf.android.e2e.CompleteRomanianExpertE2eTest')
    if(-not $gameFailure){
        try{Invoke-Stage 'recorded-romanian-audit' { & mvn -q -pl atf-test-android test '-Dtest=RecordedRomanianMatchAuditTest' '-Datf.romanian.audit=true' "-Datf.match.evidenceDir=$evidence" }}catch{$gameFailure=$_}
        Save-Xml 'atf-test-android' @('atf.android.e2e.RecordedRomanianMatchAuditTest')
    }
    $publicationFailure=$null
    try{Invoke-Stage 'romanian-expert-extent' { & mvn -q -pl atf-tests test '-Dtest=RomanianExpertExtentReportTest' '-Datf.romanian.publish=true' "-Datf.romanian.runDir=$RunRoot" "-Datf.test.heap.max=$ReportHeap" }}catch{$publicationFailure=$_}
    if($publicationFailure){$state.publicationFailure=$publicationFailure.ToString()}
    if($gameFailure){throw $gameFailure}
    if($publicationFailure){throw $publicationFailure}
    $state.status='PASS';$state.completedAt=Get-Date -Format o
    Set-Content -LiteralPath (Join-Path (Split-Path -Parent $RunRoot) 'latest-run.txt') -Value $RunRoot
    Write-Output "$RunRoot\romanian-expert-extent-report.html"
}catch{
    $state.status='FAIL';$state.failure=$_.ToString();throw
}finally{
    $state | ConvertTo-Json | Set-Content -LiteralPath "$RunRoot\WORKING_STATE.json"
    Pop-Location
    foreach($name in $saved.Keys){[Environment]::SetEnvironmentVariable($name,$saved[$name])}
}
