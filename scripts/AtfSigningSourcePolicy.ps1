#requires -Version 7.0

function Test-AtfPrivateSigningInput([string]$RelativePath) {
    $path = $RelativePath.Replace('\','/')
    if (-not $path.StartsWith('.atf-signing/',[StringComparison]::OrdinalIgnoreCase)) { return $false }
    # Export only the public signing contract, never local key material or backups.
    return $path -inotmatch '^\.atf-signing/(?:\.gitignore|README\.md|whist-android-test\.pem|whist-android-test\.fingerprint)$'
}

function Assert-AtfDirectSourcePath([string]$Path) {
    $current = $Path
    while ($current) {
        if ([IO.File]::Exists($current) -or [IO.Directory]::Exists($current)) {
            if ([IO.File]::GetAttributes($current) -band [IO.FileAttributes]::ReparsePoint) {
                throw 'SOURCE-STAGE-INDIRECT: source and staging paths must not traverse links.'
            }
        }
        $parent = [IO.Directory]::GetParent($current)
        $current = if ($null -eq $parent) { '' } else { $parent.FullName }
    }
}
