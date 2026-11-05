Set-StrictMode -Version Latest

$script:SourceRoots = @('game-core/src/main/java', 'ai-lab/src/main/java')

function Get-WhistMainSourceInventory {
    param([Parameter(Mandatory)][string]$AppRoot)
    $base = [IO.Path]::GetFullPath($AppRoot)
    $rows = [Collections.Generic.List[object]]::new()
    function Visit-WhistSource([string]$Directory) {
        $item = Get-Item -LiteralPath $Directory -Force -ErrorAction Stop
        if (-not $item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw "Source directory must be physical: $Directory"
        }
        foreach ($entry in Get-ChildItem -LiteralPath $Directory -Force) {
            if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) {
                throw "Source links are not part of a frozen inventory: $($entry.FullName)"
            }
            if ($entry.PSIsContainer) { Visit-WhistSource $entry.FullName }
            elseif ($entry.Extension -ceq '.java') {
                $rows.Add([pscustomobject]@{
                    target = [IO.Path]::GetRelativePath($base, $entry.FullName).Replace('\', '/')
                    sha256 = (Get-FileHash -LiteralPath $entry.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
                })
            }
        }
    }
    $baseItem = Get-Item -LiteralPath $base -Force -ErrorAction Stop
    if ($baseItem.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Application root must be physical.' }
    foreach ($tree in $script:SourceRoots) {
        $parent = $base
        foreach ($segment in ($tree -split '/')) {
            $parent = Join-Path $parent $segment
            $ancestor = Get-Item -LiteralPath $parent -Force -ErrorAction Stop
            if ($ancestor.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Linked source ancestor: $parent" }
        }
        Visit-WhistSource (Join-Path $base $tree)
    }
    $targets = [string[]]@($rows | ForEach-Object target)
    [Array]::Sort($targets, [StringComparer]::Ordinal)
    $byPath = [Collections.Generic.Dictionary[string,object]]::new([StringComparer]::Ordinal)
    foreach ($row in $rows) { $byPath.Add($row.target, $row) }
    foreach ($target in $targets) { $byPath[$target] }
}

function Get-WhistInventoryDigest {
    param([object[]]$Rows)
    $text = [Text.StringBuilder]::new()
    $targets = [string[]]@($Rows | ForEach-Object target)
    [Array]::Sort($targets, [StringComparer]::Ordinal)
    $byPath = [Collections.Generic.Dictionary[string,string]]::new([StringComparer]::Ordinal)
    foreach ($row in $Rows) { $byPath.Add($row.target, $row.sha256) }
    foreach ($target in $targets) { [void]$text.Append($target).Append("`t").Append($byPath[$target]).Append("`n") }
    $algorithm = [Security.Cryptography.SHA256]::Create()
    try { [Convert]::ToHexString($algorithm.ComputeHash([Text.Encoding]::UTF8.GetBytes($text.ToString()))).ToLowerInvariant() }
    finally { $algorithm.Dispose() }
}

function New-WhistCandidateBaseline {
    param([Parameter(Mandatory)][string]$AppRoot)
    $rows = @(Get-WhistMainSourceInventory -AppRoot $AppRoot)
    if ($rows.Count -eq 0) { throw 'A candidate baseline cannot be empty.' }
    [pscustomobject]@{
        baselineGuards = $rows
        baselineInventory = [pscustomobject]@{
            schemaVersion = 1
            roots = $script:SourceRoots.Clone()
            sha256 = Get-WhistInventoryDigest $rows
        }
    }
}

function Get-WhistExpectedSources {
    param([Parameter(Mandatory)]$Specification)
    if ($Specification.schemaVersion -notin @(1, 2)) { throw 'Unsupported candidate source schema.' }
    $expected = [Collections.Generic.Dictionary[string,string]]::new([StringComparer]::Ordinal)
    foreach ($guard in @($Specification.baselineGuards)) {
        if ($guard.target -cnotmatch '^(game-core|ai-lab)/src/main/java/[^\\]+\.java$' -or
            ($guard.target -split '/') -contains '..' -or ($guard.target -split '/') -contains '.' -or
            $guard.target.Contains('//') -or $guard.sha256 -cnotmatch '^[a-f0-9]{64}$' -or
            $expected.ContainsKey($guard.target)) { throw 'Invalid or duplicate candidate baseline guard.' }
        $expected.Add($guard.target, $guard.sha256)
    }
    if ($expected.Count -eq 0) { throw 'A candidate baseline cannot be empty.' }
    $metadataProperty = $Specification.PSObject.Properties['baselineInventory']
    if ($Specification.schemaVersion -eq 2 -and $null -eq $metadataProperty) { throw 'Schema 2 requires an explicit baseline inventory.' }
    if ($null -ne $metadataProperty) {
        $metadata = $metadataProperty.Value
        if ($metadata.schemaVersion -ne 1 -or (@($metadata.roots) -join "`n") -cne ($script:SourceRoots -join "`n") -or
            $metadata.sha256 -cnotmatch '^[a-f0-9]{64}$' -or
            $metadata.sha256 -cne (Get-WhistInventoryDigest @($Specification.baselineGuards))) {
            throw 'Candidate baseline inventory identity differs.'
        }
    }
    return ,$expected
}

function Assert-WhistSourceSet {
    param([string]$AppRoot, [Collections.Generic.Dictionary[string,string]]$Expected)
    $actual = @(Get-WhistMainSourceInventory -AppRoot $AppRoot)
    if ($actual.Count -ne $Expected.Count) { throw 'Main Java path set changed; rebase and requalify the candidate.' }
    foreach ($row in $actual) {
        if (-not $Expected.ContainsKey($row.target)) { throw "Undeclared main Java source: $($row.target)" }
        if ($Expected[$row.target] -cne $row.sha256) { throw "Main Java source changed: $($row.target)" }
    }
    [pscustomobject]@{sourceCount=$actual.Count;sha256=Get-WhistInventoryDigest $actual;roots=$script:SourceRoots.Clone()}
}

function Assert-WhistCandidateAcceptedSource {
    param([Parameter(Mandatory)][string]$AppRoot, [Parameter(Mandatory)]$Specification)
    $expected = Get-WhistExpectedSources $Specification
    Assert-WhistSourceSet -AppRoot $AppRoot -Expected $expected
}

function Assert-WhistStagedCandidateSource {
    param([Parameter(Mandatory)][string]$AppRoot, [Parameter(Mandatory)]$Specification)
    $expected = Get-WhistExpectedSources $Specification
    $targets = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($entry in @($Specification.files)) {
        if (-not $targets.Add($entry.target)) { throw 'Duplicate candidate overlay target.' }
        if ($entry.target -cmatch '^(game-core|ai-lab)/src/main/java/[^\\]+\.java$') {
            if (($entry.target -split '/') -contains '..' -or ($entry.target -split '/') -contains '.' -or
                $entry.target.Contains('//') -or $entry.sha256 -cnotmatch '^[a-f0-9]{64}$') { throw 'Invalid candidate overlay source.' }
            $expected[$entry.target] = $entry.sha256
        }
    }
    Assert-WhistSourceSet -AppRoot $AppRoot -Expected $expected
}

Export-ModuleMember -Function Get-WhistMainSourceInventory,New-WhistCandidateBaseline,Assert-WhistCandidateAcceptedSource,Assert-WhistStagedCandidateSource
