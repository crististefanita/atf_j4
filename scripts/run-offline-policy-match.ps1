param(
    [long]$Seed = 20261104,
    [string]$EvidenceRoot = 'C:\work\whist_back\offline-mock'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$repo = Join-Path $projectRoot 'whist-lab-app'
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
    $xmlPath = 'game-core/build/test-results/offlineMockMatchTest/TEST-ro.whistlab.core.match.OfflineMockMatchTest.xml'
    $xml = [xml](Get-Content -LiteralPath $xmlPath -Raw)
    if (!$result.complete -or $result.handsPlayed -ne 24 -or $result.publicCardDecisions -ne 376 -or [int]$xml.testsuite.failures -or [int]$xml.testsuite.errors) {
        throw 'Mock match receipt failed its independent final checks'
    }
    $allPlays = @($result.hands | ForEach-Object { $_.plays })
    $hardPlays = @($allPlays | Where-Object { $_.policy -eq 'HARD' })
    $impossibleHard = @($hardPlays | Where-Object { $_.hardContractImpossible -eq $true })
    $salvageFallback = @($impossibleHard | Where-Object { $_.decision -like '*publicly unreachable contract*' })
    $noChoice = @($impossibleHard | Where-Object { $_.legalOptionCount -eq 1 })
    if ($impossibleHard.Count -ne $result.hardQuotaImpossibleDecisions -or
        $salvageFallback.Count -ne $result.hardSalvageFallbackDecisions -or
        $noChoice.Count -ne $result.hardQuotaImpossibleNoChoice) { throw 'Hard objective event counts do not reconcile' }
    $receipt = [ordered]@{
        status = 'PASS'
        kind = 'FOUR_SEAT_ROMANIAN_1_8_1_OFFLINE_MOCK'
        seed = $Seed
        completeMatch = $true
        hands = $result.handsPlayed
        publicCardDecisions = $result.publicCardDecisions
        roster = $result.policiesBySeat
        finalScores = $result.finalScores
        hardQuotaImpossibleDecisions = $result.hardQuotaImpossibleDecisions
        hardSalvageFallbackDecisions = $result.hardSalvageFallbackDecisions
        hardQuotaImpossibleNoChoice = $result.hardQuotaImpossibleNoChoice
        hardCertifiedSalvageDecisions = $result.hardSalvageCertificateDecisions
        deviceExecuted = $false
        sourceSha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/ai/policy/AiPolicy.java')).Hash
        normalPolicySha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/ai/policy/NormalPlayPolicy.java')).Hash
        hardPolicySha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/ai/engine/MonteCarloAi.java')).Hash
        rulesSha256 = (Get-FileHash -LiteralPath (Join-Path $projectRoot 'config/whist-play-objectives.yaml')).Hash
        matchSha256 = (Get-FileHash -LiteralPath $match).Hash
        nativeTest = $xmlPath
        report = $match
    }
    $receipt | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $run 'receipt.json') -Encoding utf8
    Write-Output "PASS $($result.handsPlayed) hands, $($result.publicCardDecisions) card decisions, $($result.hardSalvageFallbackDecisions) Hard salvage fallbacks"
    Write-Output $run
} finally { Pop-Location }
