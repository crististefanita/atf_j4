#requires -Version 7.0

function New-AtfQualificationTiming([string]$Owner, [string]$File) {
    if (Test-Path -LiteralPath $File) { throw "Existing timing receipt: $File" }
    @{clock=[Diagnostics.Stopwatch]::StartNew()
      file=$File; owner=$Owner; stages=[Collections.Generic.List[object]]::new()}
}

function Add-AtfQualificationTiming($Timing, [string]$Stage, [DateTimeOffset]$Started, [DateTimeOffset]$Ended, [long]$ElapsedMs,
                                    [string]$Status, [string]$Reason = '') {
    $row = [ordered]@{stage=$Stage; status=$Status; startedUtc=$Started.ToString('o')
        endedUtc=$Ended.ToString('o'); elapsedMs=$ElapsedMs}
    if ($Reason) { $row.reason = $Reason }
    $Timing.stages.Add($row)
    $document = [ordered]@{schemaVersion=1; kind='ATF_RELEASE_TIMING'; owner=$Timing.owner; stages=$Timing.stages.ToArray()}
    $json = ($document | ConvertTo-Json -Depth 6) + [Environment]::NewLine
    $temporary = $Timing.file + '.writing'
    [IO.File]::WriteAllText($temporary,$json)
    [IO.File]::Move($temporary,$Timing.file,$true)
}

function Invoke-AtfQualificationPhase($Timing, [string]$PhaseName, [scriptblock]$Action, [switch]$ExitCode) {
    $started = [DateTimeOffset]::UtcNow
    $start = $Timing.clock.ElapsedMilliseconds
    $status = 'FAIL'
    try {
        if ($ExitCode) {
            $result = & $Action
            if ($result -isnot [int]) { throw "A native stage must return one integer exit code: $PhaseName" }
            if ($result -eq 0) { $status = 'PASS' }
            return $result
        }
        & $Action
        $status = 'PASS'
    } finally {
        Add-AtfQualificationTiming $Timing $PhaseName $started ([DateTimeOffset]::UtcNow) ($Timing.clock.ElapsedMilliseconds - $start) $status
    }
}

function Write-AtfQualificationDecision($Timing,[string]$Stage,[ValidateSet('REUSED','NOT_RUN')][string]$Status,
                                         [string]$Reason) {
    $now = [DateTimeOffset]::UtcNow
    Add-AtfQualificationTiming $Timing $Stage $now $now 0 $Status $Reason
}
