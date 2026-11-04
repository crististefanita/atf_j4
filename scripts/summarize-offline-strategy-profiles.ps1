param(
    [string]$EvidenceRoot = 'C:\work\whist_back\offline-mock',
    [long[]]$Seeds = @(20261107, 20261108),
    [string]$OutputName = 'strategy-profile-hand-size-analysis.json'
)

$ErrorActionPreference = 'Stop'
$strategies = @('PRUDENT_BID_04', 'PRUDENT_BID', 'PRUDENT_BID_12', 'SCORE_FIRST', 'BURN_IF_DOOMED')
$runs = @()

foreach ($seed in $Seeds) {
    $matches = @(Get-ChildItem -LiteralPath $EvidenceRoot -Directory -Filter "strategy-profiles-*-seed$seed" |
        ForEach-Object {
            $receiptPath = Join-Path $_.FullName 'receipt.json'
            $matrixPath = Join-Path $_.FullName 'strategy-profiles.json'
            if ((Test-Path -LiteralPath $receiptPath) -and (Test-Path -LiteralPath $matrixPath)) {
                $receipt = Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
                if ($receipt.seed -eq $seed -and $receipt.status -eq 'PASS') {
                    [pscustomobject]@{
                        Folder = $_.Name
                        Receipt = $receipt
                        Matrix = Get-Content -LiteralPath $matrixPath -Raw | ConvertFrom-Json
                    }
                }
            }
        })
    if ($matches.Count -ne 1) { throw "Expected exactly one passing profile run for seed $seed, found $($matches.Count)" }
    $run = $matches[0]
    if ($run.Receipt.runs -ne 25 -or $run.Receipt.handsPerRun -ne 24 -or
        !$run.Receipt.sameDistributionAndOrder -or $run.Receipt.privateDealsIncluded -or
        $run.Receipt.opaqueCommitmentsCompared -ne 576 -or $run.Matrix.profiles.Count -ne 24) {
        throw "Profile run failed required same-deal / public-information checks: $($run.Folder)"
    }
    $runs += $run
}

$sourceFingerprint = @($runs | ForEach-Object {
    "$($_.Receipt.aiPolicySha256)|$($_.Receipt.monteCarloSha256)|$($_.Receipt.profileEnumSha256)|$($_.Receipt.objectivesYamlSha256)"
} | Sort-Object -Unique)
if ($sourceFingerprint.Count -ne 1) {
    throw 'Selected runs use different code or YAML hashes; analyze each source generation separately.'
}

$pairedHands = @()
foreach ($run in $runs) {
    foreach ($seat in 0..3) {
        $current = $run.Matrix.profiles | Where-Object profile -eq "seat${seat}_HARD_CURRENT" | Select-Object -First 1
        if (!$current) { throw "Missing CURRENT control at seat $seat in $($run.Folder)" }

        foreach ($strategy in $strategies) {
            $candidate = $run.Matrix.profiles | Where-Object profile -eq "seat${seat}_HARD_$strategy" | Select-Object -First 1
            if (!$candidate) { throw "Missing $strategy at seat $seat in $($run.Folder)" }
            if ($candidate.match.hands.Count -ne $current.match.hands.Count) {
                throw "Hand count differs between candidate and CURRENT for $strategy, seat $seat"
            }

            for ($index = 0; $index -lt $current.match.hands.Count; $index++) {
                $baseHand = $current.match.hands[$index]
                $testHand = $candidate.match.hands[$index]
                foreach ($field in @('hand', 'size', 'trump', 'turnedCard', 'dealer')) {
                    if ([string]$baseHand.$field -cne [string]$testHand.$field) {
                        throw "Public hand schedule differs for seed $($run.Receipt.seed), seat $seat, hand $($index + 1), field $field"
                    }
                }

                $baseResult = $baseHand.results | Where-Object seat -eq $seat | Select-Object -First 1
                $testResult = $testHand.results | Where-Object seat -eq $seat | Select-Object -First 1
                $baseBid = $baseHand.bids | Where-Object seat -eq $seat | Select-Object -First 1
                $testBid = $testHand.bids | Where-Object seat -eq $seat | Select-Object -First 1
                $baseCards = @($baseHand.plays | Where-Object seat -eq $seat | ForEach-Object card)
                $testCards = @($testHand.plays | Where-Object seat -eq $seat | ForEach-Object card)
                $changedActions = 0
                $firstChangedAction = 0
                for ($action = 0; $action -lt [Math]::Max($baseCards.Count, $testCards.Count); $action++) {
                    if ($action -ge $baseCards.Count -or $action -ge $testCards.Count -or
                        $baseCards[$action] -cne $testCards[$action]) {
                        $changedActions++
                        if ($firstChangedAction -eq 0) { $firstChangedAction = $action + 1 }
                    }
                }

                $pairedHands += [pscustomobject]@{
                    seed = [long]$run.Receipt.seed
                    seat = $seat
                    strategy = $strategy
                    hand = [int]$baseHand.hand
                    handSize = [int]$baseHand.size
                    trump = [string]$baseHand.trump
                    turnedCard = [string]$baseHand.turnedCard
                    dealer = [int]$baseHand.dealer
                    currentBid = [int]$baseBid.bid
                    candidateBid = [int]$testBid.bid
                    bidChanged = ([int]$baseBid.bid -ne [int]$testBid.bid)
                    currentTricks = [int]$baseResult.tricksWon
                    candidateTricks = [int]$testResult.tricksWon
                    currentPoints = [int]$baseResult.points
                    candidatePoints = [int]$testResult.points
                    currentExact = ([int]$baseResult.bid -eq [int]$baseResult.tricksWon)
                    candidateExact = ([int]$testResult.bid -eq [int]$testResult.tricksWon)
                    cardActionDifferences = $changedActions
                    firstDifferentAction = $firstChangedAction
                    currentCards = $baseCards
                    candidateCards = $testCards
                }
            }
        }
    }
}

$byStrategyAndSize = @()
$byStrategyAndSeat = @()
$byStrategy = @()
$examples = @()
foreach ($strategy in $strategies) {
    $strategyRows = @($pairedHands | Where-Object strategy -eq $strategy)
    $pointDelta = ($strategyRows | ForEach-Object { $_.candidatePoints - $_.currentPoints } | Measure-Object -Sum).Sum
    $exactDelta = @($strategyRows | Where-Object candidateExact).Count - @($strategyRows | Where-Object currentExact).Count
    $byStrategy += [ordered]@{
        strategy = $strategy
        pairedHands = $strategyRows.Count
        pointsDeltaFromCurrent = $pointDelta
        exactDeltaFromCurrent = $exactDelta
        positiveHandPairs = @($strategyRows | Where-Object { $_.candidatePoints -gt $_.currentPoints }).Count
        tiedHandPairs = @($strategyRows | Where-Object { $_.candidatePoints -eq $_.currentPoints }).Count
        negativeHandPairs = @($strategyRows | Where-Object { $_.candidatePoints -lt $_.currentPoints }).Count
        bidChangedHands = @($strategyRows | Where-Object bidChanged).Count
        handsWithDifferentCardSequence = @($strategyRows | Where-Object { $_.cardActionDifferences -gt 0 }).Count
        differentCardActions = ($strategyRows | Measure-Object cardActionDifferences -Sum).Sum
        gainedExactContracts = @($strategyRows | Where-Object { $_.candidateExact -and !$_.currentExact }).Count
        lostExactContracts = @($strategyRows | Where-Object { $_.currentExact -and !$_.candidateExact }).Count
    }

    foreach ($seat in 0..3) {
        $seatRows = @($strategyRows | Where-Object seat -eq $seat)
        $matchDeltas = @($seatRows | Group-Object seed | ForEach-Object {
            ($_.Group | ForEach-Object { $_.candidatePoints - $_.currentPoints } | Measure-Object -Sum).Sum
        })
        $matchExactDeltas = @($seatRows | Group-Object seed | ForEach-Object {
            @($_.Group | Where-Object candidateExact).Count - @($_.Group | Where-Object currentExact).Count
        })
        $byStrategyAndSeat += [ordered]@{
            strategy = $strategy
            seat = $seat
            pairedFullMatches = $matchDeltas.Count
            pointsDeltaFromCurrent = ($matchDeltas | Measure-Object -Sum).Sum
            exactDeltaFromCurrent = ($matchExactDeltas | Measure-Object -Sum).Sum
            positiveFullMatches = @($matchDeltas | Where-Object { $_ -gt 0 }).Count
            tiedFullMatches = @($matchDeltas | Where-Object { $_ -eq 0 }).Count
            negativeFullMatches = @($matchDeltas | Where-Object { $_ -lt 0 }).Count
            bidChangedHands = @($seatRows | Where-Object bidChanged).Count
            handsWithDifferentCardSequence = @($seatRows | Where-Object { $_.cardActionDifferences -gt 0 }).Count
        }
    }

    foreach ($size in 1..8) {
        $sizeRows = @($strategyRows | Where-Object handSize -eq $size)
        if ($sizeRows.Count -eq 0) { continue }
        $sizePointDelta = ($sizeRows | ForEach-Object { $_.candidatePoints - $_.currentPoints } | Measure-Object -Sum).Sum
        $sizeExactDelta = @($sizeRows | Where-Object candidateExact).Count - @($sizeRows | Where-Object currentExact).Count
        $byStrategyAndSize += [ordered]@{
            strategy = $strategy
            handSize = $size
            pairedHands = $sizeRows.Count
            pointsDeltaFromCurrent = $sizePointDelta
            exactDeltaFromCurrent = $sizeExactDelta
            bidChangedHands = @($sizeRows | Where-Object bidChanged).Count
            handsWithDifferentCardSequence = @($sizeRows | Where-Object { $_.cardActionDifferences -gt 0 }).Count
            differentCardActions = ($sizeRows | Measure-Object cardActionDifferences -Sum).Sum
            gainedExactContracts = @($sizeRows | Where-Object { $_.candidateExact -and !$_.currentExact }).Count
            lostExactContracts = @($sizeRows | Where-Object { $_.currentExact -and !$_.candidateExact }).Count
            candidateOvertricks = ($sizeRows | ForEach-Object { [Math]::Max(0, $_.candidateTricks - $_.candidateBid) } | Measure-Object -Sum).Sum
            candidateUndertricks = ($sizeRows | ForEach-Object { [Math]::Max(0, $_.candidateBid - $_.candidateTricks) } | Measure-Object -Sum).Sum
        }
    }

    $examples += @($strategyRows | Where-Object {
        $_.cardActionDifferences -gt 0 -and $_.candidatePoints -ne $_.currentPoints
    } | Sort-Object @{Expression={ [Math]::Abs($_.candidatePoints - $_.currentPoints) }; Descending=$true} |
        Select-Object -First 3 @{Name='strategy';Expression={$strategy}},seed,seat,hand,handSize,trump,turnedCard,dealer,currentBid,candidateBid,currentTricks,candidateTricks,currentPoints,candidatePoints,currentExact,candidateExact,cardActionDifferences,firstDifferentAction,currentCards,candidateCards)
}

$schedule = @($runs[0].Matrix.profiles[0].match.hands | ForEach-Object { [int]$_.size })
$hardVsNormalBySeedAndSeat = @()
foreach ($run in $runs) {
    foreach ($seat in 0..3) {
        $currentMatch = $run.Matrix.profiles | Where-Object profile -eq "seat${seat}_HARD_CURRENT" | Select-Object -First 1
        $hardVsNormalBySeedAndSeat += [ordered]@{
            seed = [long]$run.Receipt.seed
            seat = $seat
            hardPoints = [int]$currentMatch.focalPoints
            normalPointsSameSeat = [int]$currentMatch.normalBaselinePoints
            pointsDelta = [int]$currentMatch.pointsDeltaVsNormalBaseline
            hardExactContracts = [int]$currentMatch.exactContracts
            normalExactSameSeat = [int]$currentMatch.normalBaselineExactContracts
            exactDelta = [int]$currentMatch.exactDeltaVsNormalBaseline
        }
    }
}
$output = [ordered]@{
    schemaVersion = 1
    kind = 'SAME_DEAL_SAME_SEAT_HAND_SIZE_STRATEGY_COMPARISON'
    seeds = @($runs | ForEach-Object { [long]$_.Receipt.seed })
    runs = $runs.Count
    profilesCompared = $strategies
    focalSeats = @(0,1,2,3)
    handSchedule = $schedule
    sameDistributionAndOrder = $true
    privateDealsIncluded = $false
    sourceHashes = [ordered]@{
        aiPolicy = $runs[0].Receipt.aiPolicySha256
        monteCarlo = $runs[0].Receipt.monteCarloSha256
        hardProfile = $runs[0].Receipt.profileEnumSha256
        objectivesYaml = $runs[0].Receipt.objectivesYamlSha256
    }
    runFolders = @($runs | ForEach-Object { $_.Folder })
    method = 'For each seed and focal seat, compare the candidate complete match with CURRENT at the same seat and pair by hand index. Later public states diverge after changed bids or card actions; size-specific deltas describe whole-hand outcomes and are not isolated move effects.'
    scoringNote = 'Points include the Romanian five-hand streak bonus configured as +10 in these offline matches. A hand-size delta can therefore include a bonus caused by earlier hands.'
    currentHardVsNormalSameSeat = $hardVsNormalBySeedAndSeat
    resultsByStrategy = $byStrategy
    resultsByStrategyAndSeat = $byStrategyAndSeat
    resultsByStrategyAndHandSize = $byStrategyAndSize
    largestPublicActionDivergences = $examples
}

$outputPath = Join-Path $EvidenceRoot $OutputName
$output | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $outputPath -Encoding utf8
$profileTotals = @()
foreach ($strategy in $strategies) {
    $seatRows = @($byStrategyAndSeat | Where-Object strategy -eq $strategy)
    $handRow = $byStrategy | Where-Object strategy -eq $strategy | Select-Object -First 1
    $profileTotals += [ordered]@{
        strategy = $strategy
        pairedFullMatches = ($seatRows | ForEach-Object { [int]$_.pairedFullMatches } | Measure-Object -Sum).Sum
        pointsDeltaFromCurrent = ($seatRows | ForEach-Object { [double]$_.pointsDeltaFromCurrent } | Measure-Object -Sum).Sum
        exactDeltaFromCurrent = ($seatRows | ForEach-Object { [int]$_.exactDeltaFromCurrent } | Measure-Object -Sum).Sum
        fullMatchesWonTiedLost = @(
            ($seatRows | ForEach-Object { [int]$_.positiveFullMatches } | Measure-Object -Sum).Sum,
            ($seatRows | ForEach-Object { [int]$_.tiedFullMatches } | Measure-Object -Sum).Sum,
            ($seatRows | ForEach-Object { [int]$_.negativeFullMatches } | Measure-Object -Sum).Sum
        )
        bidChangedHands = ($seatRows | ForEach-Object { [int]$_.bidChangedHands } | Measure-Object -Sum).Sum
        handsWithDifferentCardSequence = ($seatRows | ForEach-Object { [int]$_.handsWithDifferentCardSequence } | Measure-Object -Sum).Sum
        gainedExactContracts = [int]$handRow.gainedExactContracts
        lostExactContracts = [int]$handRow.lostExactContracts
    }
}
$hardBySeat = @()
foreach ($seat in 0..3) {
    $rows = @($hardVsNormalBySeedAndSeat | Where-Object seat -eq $seat)
    $hardBySeat += [ordered]@{
        seat = $seat
        pairedFullMatches = $rows.Count
        pointsDelta = ($rows | ForEach-Object { [int]$_.pointsDelta } | Measure-Object -Sum).Sum
        exactDelta = ($rows | ForEach-Object { [int]$_.exactDelta } | Measure-Object -Sum).Sum
        positiveMatches = @($rows | Where-Object { $_.pointsDelta -gt 0 -and $_.exactDelta -gt 0 }).Count
    }
}
$hardBySeed = @()
foreach ($run in $runs) {
    $seedRows = @($hardVsNormalBySeedAndSeat | Where-Object { [long]$_.seed -eq [long]$run.Receipt.seed })
    $hardBySeed += [ordered]@{
        seed = [long]$run.Receipt.seed
        focalSeatMatches = $seedRows.Count
        pointsDelta = ($seedRows | ForEach-Object { [int]$_.pointsDelta } | Measure-Object -Sum).Sum
        exactDelta = ($seedRows | ForEach-Object { [int]$_.exactDelta } | Measure-Object -Sum).Sum
    }
}
$hardPointsDelta = ($hardVsNormalBySeedAndSeat | ForEach-Object { [int]$_.pointsDelta } | Measure-Object -Sum).Sum
$hardExactDelta = ($hardVsNormalBySeedAndSeat | ForEach-Object { [int]$_.exactDelta } | Measure-Object -Sum).Sum
$profileSummary = [ordered]@{
    schemaVersion = 1
    kind = 'SAME_SEAT_SAME_DEALS_STRATEGY_PROFILE_SUMMARY'
    seeds = @($runs | ForEach-Object { [long]$_.Receipt.seed })
    strategyProfileRuns = $runs.Count
    focalSeatProfileMatches = $hardVsNormalBySeedAndSeat.Count
    handsPerMatch = 24
    sameDistributionAndOrder = $true
    privateDealsIncluded = $false
    sourceHashes = $output.sourceHashes
    runFolders = $output.runFolders
    currentHardVsNormalSameSeat = [ordered]@{
        pointsDelta = $hardPointsDelta
        exactDelta = $hardExactDelta
        positiveFocalSeatMatches = @($hardVsNormalBySeedAndSeat | Where-Object { $_.pointsDelta -gt 0 -and $_.exactDelta -gt 0 }).Count
        bySeat = $hardBySeat
        bySeed = $hardBySeed
    }
    candidatesVsCurrent = $profileTotals
    candidatesBySeat = $byStrategyAndSeat
    candidatesByHandSize = $byStrategyAndSize
    qualification = 'These are paired development matches, not a proof of optimal play. Do not promote a profile unless it passes all-seat non-regression on untouched seeds and the component cases that explain its gains.'
}
$summaryPath = Join-Path $EvidenceRoot 'current-strategy-profile-summary.json'
$profileSummary | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $summaryPath -Encoding utf8
Write-Output "PASS: $($pairedHands.Count) paired hand outcomes across $($runs.Count) fresh seeds, four seats, and five strategy profiles"
Write-Output $outputPath
Write-Output $summaryPath
