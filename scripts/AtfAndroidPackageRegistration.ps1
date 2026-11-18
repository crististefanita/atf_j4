#requires -Version 7.0

function Invoke-AtfVerifiedPackageRegistration([scriptblock]$Install, [scriptblock]$Observe, [string]$ExpectedHash) {
    if (!$Install -or !$Observe) { throw 'ANDROID_PACKAGE_CALLBACK_REQUIRED' }
    if ($ExpectedHash -cnotmatch '^[a-f0-9]{64}$') { throw 'ANDROID_PACKAGE_EXPECTED_HASH' }
    $failure = $null
    try { & $Install | Out-Null } catch { $failure = $_ }
    try {
        $observed = & $Observe
        if (!$observed -or $observed.path -cnotmatch '^/\S+\.apk$' -or $observed.sha256 -cne $ExpectedHash) {
            throw 'ANDROID_PACKAGE_OBSERVED_IDENTITY'
        }
    } catch {
        if ($failure) {
            $failure.Exception.Data['registrationObservation'] = $_.Exception.Message
            throw $failure
        }
        throw
    }
    [pscustomobject]@{path=$observed.path;sha256=$observed.sha256;commandRecovered=($null -ne $failure)
        commandDiagnostic=$(if ($failure) {$failure.Exception.Message} else {''})}
}
