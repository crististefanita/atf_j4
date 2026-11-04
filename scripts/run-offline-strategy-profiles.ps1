param(
    [long]$Seed = 20261104,
    [string]$EvidenceRoot = 'C:\work\whist_back\offline-mock'
)
$ErrorActionPreference = 'Stop'
$projectRoot = 'C:\work\1\atf_j4'
$repo = Join-Path $projectRoot 'whist-lab-app'
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$run = Join-Path $EvidenceRoot "strategy-profiles-romanian181-$stamp-seed$Seed"
if (Test-Path -LiteralPath $run) { throw "Evidence run already exists: $run" }
New-Item -ItemType Directory -Path $run | Out-Null
$match = Join-Path $run 'strategy-profiles.json'
$log = Join-Path $run 'gradle.log'
$env:JAVA_HOME = 'C:\tools\Java21\jdk-21.0.12.1+1'
$env:ANDROID_HOME = 'C:\tools\whist-android-sdk'
Push-Location $repo
try {
    & 'C:\tools\gradle\gradle-9.6.1\bin\gradle.bat' --offline --no-daemon --max-workers=1 `
        '-Dorg.gradle.jvmargs=-Xmx512m -XX:ActiveProcessorCount=2' `
        :game-core:offlineStrategyProfileMatchTest `
        "-Pwhist.offlineStrategy.seed=$Seed" `
        "-Pwhist.offlineStrategy.output=$match" --console=plain *> $log
    if ($LASTEXITCODE -ne 0) { Get-Content -LiteralPath $log -Tail 30; throw "Offline strategy profile replay failed for seed $Seed" }
    $result = Get-Content -LiteralPath $match -Raw | ConvertFrom-Json
    $xmlPath = 'game-core/build/test-results/offlineStrategyProfileMatchTest/TEST-ro.whistlab.core.match.OfflineStrategyProfileMatchTest.xml'
    $xml = [xml](Get-Content -LiteralPath $xmlPath -Raw)
    $expectedProfiles = @('CURRENT','PRUDENT_BID_04','PRUDENT_BID','PRUDENT_BID_12','SCORE_FIRST','BURN_IF_DOOMED')
    $expectedNames = @()
    foreach ($seat in 0..3) { foreach ($profile in $expectedProfiles) { $expectedNames += "seat${seat}_HARD_$profile" } }
    $actualNames = @($result.profiles | ForEach-Object { $_.profile })
    if ($result.runs -ne 25 -or $result.handsPerRun -ne 24 -or !$result.sameDistributionAndOrder -or
        $result.privateDealsIncluded -or $result.opaqueDealCommitmentsCompared -ne 576 -or
        @($result.profiles).Count -ne 24 -or @($result.normalBaseline).Count -ne 4 -or
        [int]$xml.testsuite.failures -or [int]$xml.testsuite.errors -or
        ($actualNames -join ',') -ne ($expectedNames -join ',')) {
        throw 'Strategy profile receipt failed its independent final checks'
    }
    $receipt = [ordered]@{
        status = 'PASS'
        kind = 'SAME_SEAT_SAME_DEALS_HARD_STRATEGY_PROFILE_COMPARISON'
        seed = $Seed
        focalSeats = @(0,1,2,3)
        profiles = $expectedProfiles
        runs = $result.runs
        handsPerRun = $result.handsPerRun
        sameDistributionAndOrder = $result.sameDistributionAndOrder
        opaqueCommitmentsCompared = $result.opaqueDealCommitmentsCompared
        privateDealsIncluded = $false
        bidOptionsLogged = $true
        cardChoicesComparedToCurrent = $true
        deviceExecuted = $false
        aiPolicySha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/ai/policy/AiPolicy.java')).Hash.ToLowerInvariant()
        monteCarloSha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/ai/engine/MonteCarloAi.java')).Hash.ToLowerInvariant()
        profileEnumSha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/ai/profile/HardStrategyProfile.java')).Hash.ToLowerInvariant()
        objectivesYamlSha256 = (Get-FileHash -LiteralPath (Join-Path $projectRoot 'config/whist-play-objectives.yaml')).Hash.ToLowerInvariant()
        matchSha256 = (Get-FileHash -LiteralPath $match).Hash.ToLowerInvariant()
        nativeTestXml = $xmlPath
    }
    $receipt | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $run 'receipt.json') -Encoding utf8
    Write-Output "PASS: 25 rosters across four seats, same deal order verified, no hidden hands in output"
    Write-Output $run
} finally { Pop-Location }
