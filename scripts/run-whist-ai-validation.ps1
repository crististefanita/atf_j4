param(
    [string]$RunRoot='',
    [ValidateSet(5,10)][int]$PositiveSeriesBonus=10,
    [switch]$WithAndroid,
    [string]$Serial='127.0.0.1:5555',
    [string]$AdbServerSocket='tcp:192.168.56.1:5038',
    [string]$AndroidSdk='C:\tools\whist-android-sdk'
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Import-Module (Join-Path $PSScriptRoot 'AndroidDeviceSetup.psm1') -Force
if(!$RunRoot){$RunRoot='C:\work\whist_back\ai-validation\'+(Get-Date -Format yyyyMMdd-HHmmss)}
$RunRoot=[IO.Path]::GetFullPath($RunRoot)
if(Test-Path -LiteralPath $RunRoot){throw 'Choose a fresh evidence folder; previous runs are retained.'}
$stage=Join-Path $env:TEMP ('ATF_J4_AI_'+[guid]::NewGuid().ToString('N'))
$leasePath='C:\work\whist_back\android-ui-lease.json'
$leaseId=[guid]::NewGuid().ToString('N');$leased=$false
New-Item -ItemType Directory -Force $stage,"$RunRoot\accepted-components","$RunRoot\coverage","$RunRoot\rule-docs" | Out-Null
Copy-Item -LiteralPath "$root\pom.xml","$root\lib" -Destination $stage -Recurse
foreach($module in @('atf-test-kit','atf-mock','atf-validation','atf-console','atf-test-android','atf-tests','whist-lab-app')){
    & robocopy "$root\$module" "$stage\$module" /E /XD target build .git .gradle artifacts /NFL /NDL /NJH /NJS /NP | Out-Null
    if($LASTEXITCODE -ge 8){throw "Staging failed: $module"}
}
$saved=@{};foreach($name in @('ANDROID_HOME','ANDROID_SDK_ROOT','ADB_SERVER_SOCKET','ATF_ANDROID_ADB','ATF_ANDROID_SERIAL','ATF_ANDROID_TIMEOUT_SECONDS')){$saved[$name]=[Environment]::GetEnvironmentVariable($name)}
$state=[ordered]@{status='COMPONENTS_RUNNING';run=$RunRoot;stage=$stage;positiveSeriesBonus=$PositiveSeriesBonus;startedAt=(Get-Date -Format o)}
function Invoke-Checked([string]$Label,[scriptblock]$Action){
    Write-Host "$Label -> $RunRoot\$Label.log"
    & $Action *> "$RunRoot\$Label.log"
    if($LASTEXITCODE -ne 0){throw "$Label failed; diagnostics retained in $RunRoot"}
}
function Save-Suite([string]$Module,[string]$Class){Copy-Item -LiteralPath "$stage\$Module\target\surefire-reports\TEST-$Class.xml" -Destination "$RunRoot\accepted-components" -Force}
function Invoke-Training([string]$Label,[int]$Bonus,[string]$Phase,[int]$Seeds,[string]$Output){
    Push-Location "$stage\whist-lab-app"
    try{Invoke-Checked $Label { & .\gradlew.bat --no-daemon --console=plain :game-core:aiTrainingTest "-Pwhist.streakBonus=$Bonus" "-Pwhist.aiTrainingPhase=$Phase" "-Pwhist.aiTrainingSeeds=$Seeds" "-Pwhist.componentEvidence=$Output" }}finally{Pop-Location}
}
Push-Location $stage
try{
    $env:ANDROID_HOME=$AndroidSdk;$env:ANDROID_SDK_ROOT=$AndroidSdk
    $env:ADB_SERVER_SOCKET=$AdbServerSocket;$env:ATF_ANDROID_ADB="$AndroidSdk\platform-tools\adb.exe";$env:ATF_ANDROID_SERIAL=$Serial;$env:ATF_ANDROID_TIMEOUT_SECONDS='90'
    Get-ChildItem "$stage\lib" -File | Get-FileHash -Algorithm SHA256 | Select-Object Path,Hash | ConvertTo-Json | Set-Content "$RunRoot\library-binaries.json"
    Copy-Item -LiteralPath "$root\docs\game\README.md" -Destination "$RunRoot\rule-docs\game-rules.md"
    Copy-Item -LiteralPath "$root\docs\learning\README.md" -Destination "$RunRoot\rule-docs\learning-protocol.md"
    Get-ChildItem "$stage\whist-lab-app" -Recurse -File | Where-Object {$_.Extension -in '.java','.gradle','.xml','.properties'} | Get-FileHash -Algorithm SHA256 | Select-Object Path,Hash | ConvertTo-Json | Set-Content "$RunRoot\source-hashes.json"
    Push-Location "$stage\whist-lab-app"
    try{Invoke-Checked 'components-and-apk' { & .\gradlew.bat --no-daemon --console=plain :game-core:test :game-core:jacocoTestReport :app:assembleDebug "-Pwhist.componentEvidence=$RunRoot\evidence\components" }}finally{Pop-Location}
    Copy-Item -Path "$stage\whist-lab-app\game-core\build\test-results\test\TEST-*.xml" -Destination "$RunRoot\accepted-components"
    Copy-Item -Path "$stage\whist-lab-app\game-core\build\reports\jacoco\test\*" -Destination "$RunRoot\coverage" -Recurse
    Invoke-Checked 'bootstrap-flat-binaries' { & mvn -q -DskipTests install }
    Invoke-Checked 'adapter-components' { & mvn -q -pl atf-test-android,atf-tests -am test '-Dtest=WhistChallengerTest,RomanianMatchOracleTest,RomanianMatchOracleSeriesTest,ReportTextValueMatcherTest,AndroidInlineScreenshotReportContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' }
    foreach($test in @('atf.android.e2e.player.WhistChallengerTest','atf.android.e2e.oracle.romanian.RomanianMatchOracleTest','atf.android.e2e.oracle.romanian.RomanianMatchOracleSeriesTest')){Save-Suite 'atf-test-android' $test}
    foreach($name in @('ReportTextValueMatcherTest','AndroidInlineScreenshotReportContractTest')){Save-Suite 'atf-tests' "atf.tests.$name"}
    Invoke-Training 'classic-regression-holdout' 0 'heldout' 16 "$RunRoot\evidence\components"
    Invoke-Training 'series-development' $PositiveSeriesBonus 'development' 4 "$RunRoot\series-development\evidence"
    Invoke-Training 'series-final-holdout' $PositiveSeriesBonus 'heldout' 16 "$RunRoot\series-heldout\evidence"
    $state.status='COMPONENTS_PASS'
    if($WithAndroid){
        if(Test-Path $leasePath){$existing=Get-Content -Raw $leasePath | ConvertFrom-Json;if($existing.active){throw "Android UI already reserved: $leasePath"}}
        $foreign=Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {$_.CommandLine -match 'atf.android.enabled=true' -and $_.CommandLine -match 'Launcher'}
        if($foreign){throw 'Another Android UI test process is active. No process was stopped.'}
        [ordered]@{active=$true;owner='J4 AI validation';leaseId=$leaseId;serial=$Serial;run=$RunRoot;startedAt=(Get-Date -Format o)} | ConvertTo-Json | Set-Content $leasePath;$leased=$true
        $adb="$AndroidSdk\platform-tools\adb.exe";$apk="$stage\whist-lab-app\app\build\outputs\apk\debug\app-debug.apk"
        Invoke-Checked 'device-ready' {
            Connect-AtfAndroidDevice -AdbExecutable $adb -Serial $Serial `
                -ServerSocket $AdbServerSocket -TimeoutSeconds 15
        }
        Invoke-Checked 'install-own-apk' { & $adb -s $Serial install -r $apk }
        $installedPath=(((& $adb -s $Serial shell pm path ro.whistlab.app) | Where-Object {$_ -like 'package:*'} | Select-Object -First 1) -replace '^package:','').Trim()
        $actual=((& $adb -s $Serial shell sha256sum $installedPath) -split '\s+')[0];$expected=(Get-FileHash $apk -Algorithm SHA256).Hash
        if($actual -ne $expected){throw 'Installed APK differs from the tested build.'}
        Copy-Item $apk "$RunRoot\whist-lab-tested.apk"
        [ordered]@{sha256=$expected;installedSha256=$actual;installedPath=$installedPath;scope='Normal Android menu and complete Expert game'} | ConvertTo-Json | Set-Content "$RunRoot\apk-provenance.json"
        [ordered]@{serial=$Serial;adbServer=$AdbServerSocket;api=(& $adb -s $Serial shell getprop ro.build.version.sdk);display=(& $adb -s $Serial shell wm size);scrcpy='Not required; headless ADB'} | ConvertTo-Json | Set-Content "$RunRoot\environment.json"
        $state.status='ANDROID_RUNNING'
        Invoke-Checked 'original-instructions' { & mvn -q -pl atf-test-android test '-Dtest=OriginalInstructionReviewE2eTest' '-Datf.android.enabled=true' "-Datf.android.artifacts=$RunRoot\original-instructions-final" }
        Save-Suite 'atf-test-android' 'atf.android.e2e.OriginalInstructionReviewE2eTest'
        Invoke-Checked 'android-menu' { & mvn -q -pl atf-test-android test '-Dtest=RomanianMenuOptionsE2eTest' '-Datf.android.enabled=true' "-Datf.match.evidenceDir=$RunRoot\android-menu" }
        Save-Suite 'atf-test-android' 'atf.android.e2e.gameplay.RomanianMenuOptionsE2eTest'
        $game="$RunRoot\android-expert-01"
        Invoke-Checked 'android-hard-challenge' { & mvn -q -pl atf-test-android test '-Dtest=CompleteRomanianExpertE2eTest' '-Datf.android.enabled=true' '-Datf.android.fullMatch=true' '-Datf.android.aiChallenge=true' '-Datf.android.aiDifficulty=HARD' "-Datf.android.streakBonus=$PositiveSeriesBonus" "-Datf.match.evidenceDir=$game" }
        Save-Suite 'atf-test-android' 'atf.android.e2e.gameplay.CompleteRomanianExpertE2eTest'
        Invoke-Checked 'offline-game-audit' { & mvn -q -pl atf-test-android test '-Dtest=RecordedRomanianMatchAuditTest' '-Datf.romanian.audit=true' "-Datf.match.evidenceDir=$game" }
        Save-Suite 'atf-test-android' 'atf.android.e2e.oracle.recorded.RecordedRomanianMatchAuditTest'
        Invoke-Checked 'native-extent' { & mvn -q -pl atf-tests test '-Dtest=WhistAiTrainingExtentReportTest' '-Datf.ai.publish=true' "-Datf.ai.expectedBonus=$PositiveSeriesBonus" "-Datf.ai.runDir=$RunRoot" '-Datf.test.heap.max=2g' }
        $state.status='PASS';Write-Output "$RunRoot\whist-ai-extent-report.html"
    }else{Write-Output "Offline component and paired AI results: $RunRoot"}
}catch{$state.status='FAIL';$state.failure=$_.ToString();throw}
finally{
    $state.completedAt=Get-Date -Format o;$state | ConvertTo-Json | Set-Content "$RunRoot\WORKING_STATE.json"
    if($leased){$current=Get-Content -Raw $leasePath | ConvertFrom-Json;if($current.leaseId -eq $leaseId){$current.active=$false;$current | ConvertTo-Json | Set-Content $leasePath}}
    Pop-Location;foreach($name in $saved.Keys){[Environment]::SetEnvironmentVariable($name,$saved[$name])}
}
