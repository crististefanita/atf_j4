# Shared machine-local roots. SourceRoot remains checkout-relative; only generated data uses these roots.
function Get-AtfEvidenceRoot([string]$ConfiguredRoot = '') {
    $selected = if (-not [string]::IsNullOrWhiteSpace($ConfiguredRoot)) { $ConfiguredRoot }
        elseif (-not [string]::IsNullOrWhiteSpace($env:WHIST_EVIDENCE_ROOT)) { $env:WHIST_EVIDENCE_ROOT }
        else {
            $local = [Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)
            if ($local) { Join-Path $local 'WhistATF' } else { Join-Path ([IO.Path]::GetTempPath()) 'WhistATF' }
        }
    if (-not [IO.Path]::IsPathRooted($selected)) { throw 'WHIST_EVIDENCE_ROOT must be an absolute machine path.' }
    [IO.Path]::GetFullPath($selected)
}

function Get-AtfAndroidSdkRoot([string]$ConfiguredRoot = '') {
    $selected = if (-not [string]::IsNullOrWhiteSpace($ConfiguredRoot)) { $ConfiguredRoot }
        elseif (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) { $env:ANDROID_SDK_ROOT }
        else { $env:ANDROID_HOME }
    if ([string]::IsNullOrWhiteSpace($selected)) {
        throw 'Set ANDROID_SDK_ROOT or ANDROID_HOME, or pass -AndroidSdk for this machine.'
    }
    if (-not [IO.Path]::IsPathRooted($selected)) { throw 'ANDROID_SDK_ROOT must be an absolute machine path.' }
    [IO.Path]::GetFullPath($selected)
}
