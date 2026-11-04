param(
    [Parameter(Mandatory=$true)]
    [string]$ReferenceRunRoot,
    [string]$AdbExecutable = 'C:\tools\whist-android-sdk\platform-tools\adb.exe',
    [string]$Serial = '127.0.0.1:5555',
    [string]$AdbServerSocket = 'tcp:192.168.56.1:5038',
    [string]$AndroidSdk = 'C:\tools\whist-android-sdk',
    [string]$EvidenceRoot = 'C:\work\whist_back\deep-app-test',
    [string]$RunRoot = '',
    [string]$ReportHeap = '3g',
    [string]$Maven = 'mvn'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Import-Module (Join-Path $PSScriptRoot 'AndroidDeviceSetup.psm1') -Force

$root = [IO.Path]::GetFullPath($root)
$referenceRun = [IO.Path]::GetFullPath($ReferenceRunRoot)
$referenceEvidence = Join-Path $referenceRun 'evidence'
$j5Root = Join-Path (Split-Path -Parent $root) 'atf_j5'
if (-not (Test-Path -LiteralPath (Join-Path $referenceEvidence 'original-match.properties'))) {
    throw "J5 reference fixture is missing: $referenceEvidence. Run the J5 ReferenceMatch scenario first."
}
if ($referenceRun.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
    $referenceRun.StartsWith($j5Root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Reference evidence must remain outside the J4 and J5 source directories.'
}
$matchProperties = [IO.File]::ReadAllText((Join-Path $referenceEvidence 'original-match.properties'))
foreach ($required in @('match.complete=true', 'match.handCount=19', 'match.moves=436', 'match.tricks=109')) {
    if (-not $matchProperties.Contains($required)) { throw "J5 reference run is incomplete: missing $required" }
}
for ($hand = 1; $hand -le 19; $hand++) {
    $fixture = Join-Path $referenceEvidence ('original-h{0:D2}-bidding.save2' -f $hand)
    if (-not (Test-Path -LiteralPath $fixture -PathType Leaf)) { throw "J5 fixture is missing: $fixture" }
}

if ([string]::IsNullOrWhiteSpace($RunRoot)) {
    $RunRoot = Join-Path $EvidenceRoot (Join-Path 'runs' ((Get-Date -Format 'yyyyMMdd-HHmmss') + '-whist-lab-reference-replay'))
}
$RunRoot = [IO.Path]::GetFullPath($RunRoot)
if ($RunRoot.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
    $RunRoot.StartsWith($j5Root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Build and evidence outputs belong outside the source projects.'
}
if (Test-Path -LiteralPath $RunRoot) { throw "Choose a fresh run folder; previous evidence is never overwritten: $RunRoot" }

$evidence = Join-Path $RunRoot 'evidence'
$components = Join-Path $RunRoot 'components'
$stage = Join-Path ([IO.Path]::GetTempPath()) ('ATF_J4_WHIST_REPLAY_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $evidence, $components, $stage | Out-Null
Copy-Item -LiteralPath (Join-Path $referenceEvidence 'original-match.properties') -Destination $evidence
for ($hand = 1; $hand -le 19; $hand++) {
    $name = 'original-h{0:D2}-bidding.save2' -f $hand
    Copy-Item -LiteralPath (Join-Path $referenceEvidence $name) -Destination $evidence
}
$referenceRecord = [ordered]@{
    owner = 'atf_j5'
    referenceRun = $referenceRun
    input = 'Recorded Contract Whist match; consumed only as Whist Lab replay input'
    matchPropertiesSha256 = (Get-FileHash -LiteralPath (Join-Path $evidence 'original-match.properties') -Algorithm SHA256).Hash.ToLowerInvariant()
    hands = 19
    inputSnapshots = 19
} | ConvertTo-Json
Set-Content -LiteralPath (Join-Path $RunRoot 'reference-input.json') -Value $referenceRecord -Encoding utf8

Copy-Item -LiteralPath (Join-Path $root 'pom.xml'), (Join-Path $root 'atf.yaml'), (Join-Path $root 'lib'), (Join-Path $root 'config') -Destination $stage -Recurse
$modules = @('atf-test-kit', 'atf-mock', 'atf-validation', 'atf-console', 'atf-test-android', 'atf-tests')
foreach ($module in $modules) {
    & robocopy (Join-Path $root $module) (Join-Path $stage $module) /E /XD target build .git .gradle artifacts /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Could not stage J4 module $module." }
}
& robocopy (Join-Path $root 'whist-lab-app') (Join-Path $stage 'whist-lab-app') /E /XD target build .git .gradle artifacts /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw 'Could not stage the Whist Lab application and offline test sources.' }
Get-ChildItem -LiteralPath $stage -Recurse -File |
    ForEach-Object { @{file=[IO.Path]::GetRelativePath($stage, $_.FullName); sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()} } |
    ConvertTo-Json -Depth 4 |
    Set-Content -LiteralPath (Join-Path $RunRoot 'source-inventory.json') -Encoding utf8

$saved = @{}
foreach ($name in @('ADB_SERVER_SOCKET', 'ATF_ANDROID_ADB', 'ATF_ANDROID_SERIAL', 'ATF_ANDROID_TIMEOUT_SECONDS', 'ANDROID_HOME', 'ANDROID_SDK_ROOT')) {
    $saved[$name] = [Environment]::GetEnvironmentVariable($name)
}
function Invoke-MatchStage {
    param([string]$Name, [string[]]$Arguments)
    $log = Join-Path $RunRoot ($Name + '.log')
    Write-Host "Running $Name; log: $log"
    & $Maven @Arguments *> $log
    if ($LASTEXITCODE -ne 0) { throw "$Name failed; diagnostics remain in $RunRoot. See $log" }
}
function Save-MatchXml {
    param([string]$Module, [string[]]$Classes)
    foreach ($class in $Classes) {
        $source = Join-Path $stage "$Module\target\surefire-reports\TEST-$class.xml"
        if (-not (Test-Path -LiteralPath $source)) { throw "Missing executed test result: $source" }
        Copy-Item -LiteralPath $source -Destination $components -Force
    }
}
Push-Location $stage
try {
    $env:ADB_SERVER_SOCKET = $AdbServerSocket
    $env:ATF_ANDROID_ADB = $AdbExecutable
    $env:ATF_ANDROID_SERIAL = $Serial
    $env:ATF_ANDROID_TIMEOUT_SECONDS = '90'
    $env:ANDROID_HOME = $AndroidSdk
    $env:ANDROID_SDK_ROOT = $AndroidSdk
    Connect-AtfAndroidDevice -AdbExecutable $AdbExecutable -Serial $Serial -ServerSocket $AdbServerSocket -TimeoutSeconds 15 | Out-Null

    Invoke-MatchStage 'bootstrap-current-binaries' @('-q', '-DskipTests', 'install')
    Get-ChildItem -LiteralPath (Join-Path $root 'lib') -Filter '*.jar' | Get-FileHash -Algorithm SHA256 |
        Select-Object Path, Algorithm, Hash | ConvertTo-Json | Set-Content (Join-Path $RunRoot 'library-binaries.json')
    Invoke-MatchStage 'complete-report-value-controls' @('-q', '-pl', 'atf-tests', 'test', '-Dtest=ReportTextValueMatcherTest')
    Save-MatchXml 'atf-tests' @('atf.tests.ReportTextValueMatcherTest')
    Invoke-MatchStage 'android-adapter-components' @('-q', '-pl', 'atf-test-android', 'test', '-Dtest=AndroidComponentSuite')
    Save-MatchXml 'atf-test-android' @('atf.android.AdbClientContractTest', 'atf.android.evidence.AndroidScreenshotEvidenceComponentTest')
    Invoke-MatchStage 'whist-lab-observation-components' @('-q', '-pl', 'atf-test-android', 'test', '-Dtest=AndroidHierarchyFreshnessTest,OriginalWhistStateTest,OriginalMatchOracleTest,WhistLabContinuationTest')
    Save-MatchXml 'atf-test-android' @('atf.android.e2e.AndroidHierarchyFreshnessTest', 'atf.android.e2e.OriginalWhistStateTest', 'atf.android.e2e.OriginalMatchOracleTest', 'atf.android.e2e.WhistLabContinuationTest')

    Push-Location (Join-Path $stage 'whist-lab-app')
    try {
        & .\gradlew.bat --no-daemon --console=plain :game-core:test :game-core:aiTournamentTest :app:assembleDebug "-Pwhist.componentEvidence=$evidence\romanian-components" *> (Join-Path $RunRoot 'whist-lab-build.log')
        if ($LASTEXITCODE -ne 0) { throw "Whist Lab build or offline game tests failed; see $RunRoot\whist-lab-build.log" }
    } finally { Pop-Location }
    $coreResults = Join-Path $components 'game-core'
    New-Item -ItemType Directory -Force $coreResults | Out-Null
    Copy-Item -Path (Join-Path $stage 'whist-lab-app\game-core\build\test-results\test\TEST-*.xml') -Destination $coreResults -Force
    Copy-Item -Path (Join-Path $stage 'whist-lab-app\game-core\build\test-results\aiTournamentTest\TEST-*.xml') -Destination $coreResults -Force
    Copy-Item -LiteralPath (Join-Path $evidence 'romanian-components\ai-tournament.properties') -Destination (Join-Path $evidence 'romanian-ai-tournament.properties') -Force

    & $AdbExecutable -s $Serial install -r (Join-Path $stage 'whist-lab-app\app\build\outputs\apk\debug\app-debug.apk') *> (Join-Path $RunRoot 'lab-install.log')
    if ($LASTEXITCODE -ne 0) { throw "Whist Lab APK installation failed; see $RunRoot\lab-install.log" }

    Push-Location (Join-Path $stage 'whist-lab-app')
    try {
        & .\gradlew.bat --no-daemon --console=plain :game-core:originalReplayTest "-Pwhist.originalReplay=$evidence\original-match.properties" *> (Join-Path $RunRoot 'game-core-original-replay.log')
        if ($LASTEXITCODE -ne 0) { throw "Whist Lab engine diverged from the recorded reference input; see $RunRoot\game-core-original-replay.log" }
    } finally { Pop-Location }
    $replayResults = Join-Path $components 'game-core-replay'
    New-Item -ItemType Directory -Force $replayResults | Out-Null
    Copy-Item -Path (Join-Path $stage 'whist-lab-app\game-core\build\test-results\originalReplayTest\TEST-*.xml') -Destination $replayResults -Force

    Invoke-MatchStage 'whist-lab-reference-replay-e2e' @('-q', '-pl', 'atf-test-android', 'test', '-Dtest=CompleteWhistLabMatchE2eTest', '-Datf.android.enabled=true', '-Datf.android.fullMatch=true', "-Datf.match.evidenceDir=$evidence")
    Save-MatchXml 'atf-test-android' @('atf.android.e2e.CompleteWhistLabMatchE2eTest')
    Invoke-MatchStage 'whist-lab-romanian-expert-e2e' @('-q', '-pl', 'atf-test-android', 'test', '-Dtest=WhistLabRomanianExpertE2eTest', '-Datf.android.enabled=true', "-Datf.match.evidenceDir=$evidence")
    Save-MatchXml 'atf-test-android' @('atf.android.e2e.WhistLabRomanianExpertE2eTest')
    $report = Join-Path $RunRoot 'final-extent-report.html'
    Invoke-MatchStage 'whist-lab-extent-publisher' @('-q', '-pl', 'atf-tests', 'test', '-Dtest=FullMatchExtentReportTest', '-Datf.match.publish=true', "-Datf.test.heap.max=$ReportHeap", "-Datf.match.evidenceDir=$evidence", "-Datf.match.reportPath=$report")
    Write-Host "Whist Lab report: $report"
} finally {
    Pop-Location
    foreach ($name in $saved.Keys) { [Environment]::SetEnvironmentVariable($name, $saved[$name]) }
}
