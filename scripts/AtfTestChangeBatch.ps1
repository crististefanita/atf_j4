#requires -Version 7.0

function Invoke-AtfTestChangeBatch([string]$RunRoot, [scriptblock]$ReadPlan, [scriptblock]$RunControl,
                                  [scriptblock]$CheckSnapshot) {
    $controls = @(& $ReadPlan)
    if (@($controls | Where-Object { $_ -cnotmatch '^[A-Z][A-Z0-9-]{3,79}$' }).Count -or
        @($controls | Sort-Object -Unique).Count -ne $controls.Count) {
        throw 'TEST-CHANGE-BATCH-PLAN: expected distinct control IDs'
    }
    $pending = Join-Path $RunRoot 'proofs.pending'
    $published = Join-Path $RunRoot 'proofs'
    if ((Test-Path -LiteralPath $pending) -or (Test-Path -LiteralPath $published)) {
        throw 'TEST-CHANGE-BATCH-OUTPUT: expected a fresh proof bundle'
    }
    New-Item -ItemType Directory -Path $pending -ErrorAction Stop | Out-Null
    $failures = [Collections.Generic.List[string]]::new()
    foreach ($id in $controls) {
        $output = Join-Path $pending $id
        $exitCode = & $RunControl $id $output
        if ($exitCode -isnot [int] -or $exitCode -ne 0 -or
            !(Test-Path -LiteralPath (Join-Path $output 'proof.pending.json') -PathType Leaf) -or
            !(Test-Path -LiteralPath (Join-Path $output 'component-report.html') -PathType Leaf)) {
            $failures.Add($id)
        }
    }
    if ($failures.Count) {
        throw "TEST-CHANGE-BATCH-CONTROL: $($failures -join ', '); vezi logurile test-change-<ID>.log"
    }
    & $CheckSnapshot
    if (($controls -join "`n") -cne (@(& $ReadPlan) -join "`n")) {
        throw 'TEST-CHANGE-BATCH-PLAN: diff controls changed during execution'
    }
    foreach ($id in $controls) {
        $output = Join-Path $pending $id
        [IO.File]::Move((Join-Path $output 'proof.pending.json'), (Join-Path $output 'proof.json'))
    }
    [IO.Directory]::Move($pending, $published)
    $published
}
