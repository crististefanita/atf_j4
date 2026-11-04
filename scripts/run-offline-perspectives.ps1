param(
    [long]$Seed = 20261104,
    [string]$EvidenceRoot = 'C:\work\whist_back\offline-mock'
)
$ErrorActionPreference = 'Stop'
$projectRoot = 'C:\work\1\atf_j4'
$repo = Join-Path $projectRoot 'whist-lab-app'
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$run = Join-Path $EvidenceRoot "perspectives-romanian181-$stamp-seed$Seed"
if (Test-Path -LiteralPath $run) { throw "Evidence run already exists: $run" }
New-Item -ItemType Directory -Path $run | Out-Null
$match = Join-Path $run 'perspective-matches.json'
$log = Join-Path $run 'gradle.log'
$env:JAVA_HOME = 'C:\tools\Java21\jdk-21.0.12.1+1'
$env:ANDROID_HOME = 'C:\tools\whist-android-sdk'
Push-Location $repo
try {
    & 'C:\tools\gradle\gradle-9.6.1\bin\gradle.bat' --offline --no-daemon --max-workers=1 `
        '-Dorg.gradle.jvmargs=-Xmx512m -XX:ActiveProcessorCount=2' `
        :game-core:offlinePerspectiveMatchTest `
        "-Pwhist.offlinePerspective.seed=$Seed" `
        "-Pwhist.offlinePerspective.output=$match" --console=plain *> $log
    if ($LASTEXITCODE -ne 0) { Get-Content -LiteralPath $log -Tail 30; throw 'Offline perspective replay failed' }
    $result = Get-Content -LiteralPath $match -Raw | ConvertFrom-Json
    $xmlPath = 'game-core/build/test-results/offlinePerspectiveMatchTest/TEST-ro.whistlab.core.OfflinePerspectiveMatchTest.xml'
    $xml = [xml](Get-Content -LiteralPath $xmlPath -Raw)
    if ($result.runs -ne 6 -or $result.handsPerRun -ne 24 -or
        !$result.sameDistributionAndOrder -or $result.privateDealsIncluded -or
        $result.opaqueDealCommitmentsCompared -ne 120 -or
        @($result.perspectives).Count -ne 6 -or [int]$xml.testsuite.failures -or [int]$xml.testsuite.errors) {
        throw 'Perspective replay receipt failed its independent final checks'
    }
    $receipt = [ordered]@{
        status = 'PASS'
        kind = 'SAME_DEALS_SEAT_ROTATED_NORMAL_HARD'
        seed = $Seed
        runs = $result.runs
        handsPerRun = $result.handsPerRun
        sameDistributionAndOrder = $result.sameDistributionAndOrder
        opaqueCommitmentsCompared = $result.opaqueDealCommitmentsCompared
        privateDealsIncluded = $false
        publicCardDecisions = @($result.perspectives | ForEach-Object { $_.match.publicCardDecisions } | Measure-Object -Sum).Sum
        rosters = @($result.perspectives | ForEach-Object { $_.roster })
        hardPointsByPerspective = @($result.perspectives | ForEach-Object {
            [ordered]@{ name = $_.name; hardPoints = $_.hardPoints; normalPoints = $_.normalPoints;
                hardExactContracts = $_.hardExactContracts; normalExactContracts = $_.normalExactContracts }
        })
        deviceExecuted = $false
        sourceSha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/AiPolicy.java')).Hash.ToLowerInvariant()
        normalPolicySha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/NormalPlayPolicy.java')).Hash.ToLowerInvariant()
        hardPolicySha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/MonteCarloAi.java')).Hash.ToLowerInvariant()
        objectivesYamlSha256 = (Get-FileHash -LiteralPath (Join-Path $projectRoot 'config/whist-play-objectives.yaml')).Hash.ToLowerInvariant()
        matchSha256 = (Get-FileHash -LiteralPath $match).Hash.ToLowerInvariant()
        nativeTestXml = $xmlPath
    }
    $receipt | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $run 'receipt.json') -Encoding utf8
    Write-Output "PASS: $($result.runs) perspectives, same deal order verified, no hidden hands in output"
    Write-Output $run
} finally { Pop-Location }
