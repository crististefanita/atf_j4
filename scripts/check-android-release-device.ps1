#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$AdbExecutable,
    [Parameter(Mandatory)][string]$Serial,
    [string]$AdbServerSocket = '',
    [Parameter(Mandatory)][string]$LeasePath,
    [Parameter(Mandatory)][string]$LeaseId,
    [ValidateRange(0,999)][int]$ExpectedApi = 0,
    [ValidatePattern('^(?:[1-9][0-9]{0,4}x[1-9][0-9]{0,4}@[1-9][0-9]{0,3})?$')][string]$ExpectedDisplayProfile = '',
    [ValidateRange(1,120)][int]$TimeoutSeconds = 20,
    [Parameter(Mandatory)][string]$Receipt
)
$ErrorActionPreference = 'Stop'
$Receipt = [IO.Path]::GetFullPath($Receipt)
Import-Module (Join-Path $PSScriptRoot '../atf-test-android/tools/AndroidDeviceSetup.psm1') -Force
$result = [ordered]@{
    schemaVersion=1
    status='BLOCKED'
    serial=$Serial
    leaseId=$LeaseId
    expectedApi=$ExpectedApi
    expectedDisplayProfile=$ExpectedDisplayProfile
}
$stream = [IO.File]::Open($Receipt,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::Read)
try {
    try { $lease = Get-Content -LiteralPath $LeasePath -Raw | ConvertFrom-Json }
    catch { throw "ANDROID_PREFLIGHT_LEASE: could not read the selected lease. $($_.Exception.Message)" }
    if ($lease.active -isnot [bool] -or -not $lease.active -or $lease.leaseId -cne $LeaseId -or $lease.serial -cne $Serial) {
        throw 'ANDROID_PREFLIGHT_LEASE: active lease must match the selected device and lease ID.'
    }
    $arguments = @{
        AdbExecutable=$AdbExecutable; Serial=$Serial; ServerSocket=$AdbServerSocket; TimeoutSeconds=$TimeoutSeconds
        ExpectedApi=$ExpectedApi; ExpectedDisplayProfile=$ExpectedDisplayProfile
    }
    $result.observed = Get-AtfAndroidReadiness @arguments
    $result.status = 'PASS'
} catch {
    $result.failure = $_.Exception.Message
    throw
} finally {
    try {
        $bytes = [Text.Encoding]::UTF8.GetBytes(($result | ConvertTo-Json -Depth 5))
        $stream.Write($bytes, 0, $bytes.Length)
    } finally { $stream.Dispose() }
}
[pscustomobject]$result
