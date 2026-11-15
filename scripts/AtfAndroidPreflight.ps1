#requires -Version 7.0
. (Join-Path $PSScriptRoot 'AtfDeviceLease.ps1')

function Assert-AtfAndroidPreflight($Observation, [string]$LeasePath, [string]$LeaseId, [string]$Serial,
    [string]$AdbServerSocket, [int]$ExpectedApi, [string]$ExpectedDisplayProfile,
    [DateTimeOffset]$Now = [DateTimeOffset]::UtcNow) {
    if ($null -eq $Observation) { throw 'ANDROID_PREFLIGHT_OBSERVATION_REQUIRED' }
    if ($Observation.status -cne 'PASS' -or $Observation.schemaVersion -ne 2 -or
        $Observation.serial -cne $Serial -or $Observation.leaseId -cne $LeaseId -or
        $Observation.adbServerSocket -cne $AdbServerSocket -or $Observation.expectedApi -ne $ExpectedApi -or
        $Observation.expectedDisplayProfile -cne $ExpectedDisplayProfile -or
        $Observation.observed.bootCompleted -isnot [bool] -or -not $Observation.observed.bootCompleted -or
        $Observation.observed.api -isnot [int] -or $Observation.observed.api -lt 1 -or $Observation.observed.api -gt 999 -or
        $Observation.observed.displayProfile -cnotmatch '^[1-9][0-9]{0,4}x[1-9][0-9]{0,4}@[1-9][0-9]{0,3}$' -or
        ($ExpectedApi -and $Observation.observed.api -ne $ExpectedApi) -or
        ($ExpectedDisplayProfile -and $Observation.observed.displayProfile -cne $ExpectedDisplayProfile)) {
        throw 'ANDROID_PREFLIGHT_OBSERVATION_MISMATCH'
    }
    $policy = Get-Content -LiteralPath (Join-Path $PSScriptRoot '../config/release-platform-rag-policy.json') -Raw | ConvertFrom-Json
    $maxAge = $policy.ragEvaluator.evidenceFreshness.androidPreflightMaxAgeSeconds
    if ($maxAge -isnot [long] -or $maxAge -le 0) { throw 'ANDROID_PREFLIGHT_FRESHNESS_POLICY' }
    if ($null -eq $Observation.observedAtUtc) { throw 'ANDROID_PREFLIGHT_TIME_REQUIRED' }
    $observed = [DateTimeOffset]::MinValue
    $culture = [Globalization.CultureInfo]::InvariantCulture
    $utc = [Globalization.DateTimeStyles]::AssumeUniversal
    if (-not [DateTimeOffset]::TryParseExact($Observation.observedAtUtc,'yyyy-MM-ddTHH:mm:ss.fffZ',$culture,$utc,[ref]$observed)) {
        throw 'ANDROID_PREFLIGHT_TIME_INVALID'
    }
    if ($observed -gt $Now) { throw 'ANDROID_PREFLIGHT_TIME_FUTURE' }
    if ($Now -ge $observed.AddSeconds($maxAge)) { throw 'ANDROID_PREFLIGHT_EXPIRED' }
    Get-AtfActiveDeviceLease $LeasePath $LeaseId $Serial | Out-Null
}
