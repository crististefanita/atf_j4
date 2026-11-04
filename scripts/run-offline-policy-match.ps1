param(
    [long]$Seed = 20261104,
    [string]$EvidenceRoot = 'C:\work\whist_back\offline-mock'
)
$ErrorActionPreference = 'Stop'
$repo = 'C:\work\1\atf_j4\whist-lab-app'
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$run = Join-Path $EvidenceRoot "romanian181-$stamp-seed$Seed"
if (Test-Path -LiteralPath $run) { throw "Evidence run already exists: $run" }
New-Item -ItemType Directory -Path $run | Out-Null
$match = Join-Path $run 'complete-match.json'
$log = Join-Path $run 'gradle.log'
$env:JAVA_HOME = 'C:\tools\Java21\jdk-21.0.12.1+1'
$env:ANDROID_HOME = 'C:\tools\whist-android-sdk'
Push-Location $repo
try {
    & 'C:\tools\gradle\gradle-9.6.1\bin\gradle.bat' --offline --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx512m -XX:ActiveProcessorCount=2' :game-core:offlineMockMatchTest "-Pwhist.offlineMatch.seed=$Seed" "-Pwhist.offlineMatch.output=$match" --console=plain *> $log
    if ($LASTEXITCODE -ne 0) { Get-Content -LiteralPath $log -Tail 25; throw 'Offline match failed' }
    $result = Get-Content -LiteralPath $match -Raw | ConvertFrom-Json
    $xmlPath = 'game-core/build/test-results/offlineMockMatchTest/TEST-ro.whistlab.core.OfflineMockMatchTest.xml'
    $xml = [xml](Get-Content -LiteralPath $xmlPath -Raw)
    if (!$result.complete -or $result.handsPlayed -ne 24 -or $result.publicCardDecisions -ne 376 -or [int]$xml.testsuite.failures -or [int]$xml.testsuite.errors) {
        throw 'Mock match receipt failed its independent final checks'
    }
    $receipt = [ordered]@{
        status = 'PASS'
        kind = 'FOUR_SEAT_ROMANIAN_1_8_1_OFFLINE_MOCK'
        seed = $Seed
        completeMatch = $true
        hands = $result.handsPlayed
        publicCardDecisions = $result.publicCardDecisions
        roster = $result.policiesBySeat
        finalScores = $result.finalScores
        hardCertifiedSalvageDecisions = $result.hardSalvageDecisions
        deviceExecuted = $false
        sourceSha256 = (Get-FileHash -LiteralPath 'game-core/src/main/java/ro/whistlab/core/AiPolicy.java').Hash
        rulesSha256 = (Get-FileHash -LiteralPath 'config/whist-play-objectives.yaml').Hash
        matchSha256 = (Get-FileHash -LiteralPath $match).Hash
        nativeTest = $xmlPath
        report = $match
    }
    $receipt | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $run 'receipt.json') -Encoding utf8
    Write-Output "PASS $($result.handsPlayed) hands, $($result.publicCardDecisions) card decisions"
    Write-Output $run
} finally { Pop-Location }