function Assert-CardTableLibraryVersion {
    param([Parameter(Mandatory)][string]$Version, [bool]$Existing = $false)
    if ($Version -cnotmatch '^0\.(0|[1-9][0-9]*)(?:\.(0|[1-9][0-9]*))?$') {
        throw 'Invalid Card Table library version; use 0.x without leading zeroes.'
    }
    if (!$Existing -and $Version -cnotmatch '^0\.(0|[1-9][0-9]*)$') {
        throw 'New Card Table library packages require 0.x; legacy 0.x.y coordinates are reuse-only.'
    }
}
Export-ModuleMember -Function Assert-CardTableLibraryVersion
