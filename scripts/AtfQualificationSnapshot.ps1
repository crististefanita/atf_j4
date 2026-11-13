#requires -Version 7.0

function Get-AtfReleaseInputs([string]$Root) {
    $release = Join-Path $Root 'release'
    $rows = [Collections.Generic.List[object]]::new()
    function Visit-ReleaseDirectory([string]$Path) {
        $directory = Get-Item -LiteralPath $Path -Force
        if (-not $directory.PSIsContainer -or ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw "RELEASE-SNAPSHOT-DIRECTORY: $Path"
        }
        foreach ($entry in Get-ChildItem -LiteralPath $Path -Force) {
            $relative = [IO.Path]::GetRelativePath($Root, $entry.FullName).Replace('\', '/')
            if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "RELEASE-SNAPSHOT-INDIRECT: $relative" }
            if ($entry.PSIsContainer) { Visit-ReleaseDirectory $entry.FullName }
            else {
                $rows.Add([pscustomobject]@{path=$relative;sha256=(Get-FileHash -LiteralPath $entry.FullName -Algorithm SHA256).Hash.ToLowerInvariant()})
            }
        }
    }
    Visit-ReleaseDirectory $release
    $rows.Sort([Comparison[object]]{param($left,$right) [StringComparer]::Ordinal.Compare($left.path,$right.path)})
    $rows.ToArray()
}

function ConvertTo-AtfInputJson([object[]]$Rows) {
    ConvertTo-Json -InputObject $Rows -Depth 5 -Compress
}

function Get-AtfInputFingerprint([string]$Json) {
    [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($Json))).ToLowerInvariant()
}

function Get-AtfSnapshotDifferences($Snapshot, [string]$Stage, [string]$SourceRoot) {
    foreach ($location in @(@{name='STAGED';root=$Stage}, @{name='LIVE';root=$SourceRoot})) {
        foreach ($scope in @('SOURCE','RELEASE')) {
            try {
                $observed = if ($scope -eq 'SOURCE') {
                    @(& (Join-Path $PSScriptRoot 'Get-AtfSourceInputs.ps1') -SourceRoot $location.root)
                } else { @(Get-AtfReleaseInputs $location.root) }
                $expected = if ($scope -eq 'SOURCE') { $Snapshot.source } else { $Snapshot.release }
                if ((ConvertTo-AtfInputJson $observed) -cne $expected) {
                    $before = [Collections.Generic.Dictionary[string,string]]::new([StringComparer]::Ordinal)
                    $after = [Collections.Generic.Dictionary[string,string]]::new([StringComparer]::Ordinal)
                    foreach ($row in ($expected | ConvertFrom-Json)) { $before[$row.path] = $row.sha256 }
                    foreach ($row in $observed) { $after[$row.path] = $row.sha256 }
                    $paths = [Collections.Generic.SortedSet[string]]::new([StringComparer]::Ordinal)
                    $paths.UnionWith($before.Keys)
                    $paths.UnionWith($after.Keys)
                    foreach ($path in $paths) {
                        if ($before[$path] -cne $after[$path]) { "$($location.name)-$($scope): $path" }
                    }
                }
            } catch { "$($location.name)-$($scope): $($_.Exception.Message)" }
        }
    }
}

function Assert-AtfQualificationSnapshot($Snapshot, [string]$Stage, [string]$SourceRoot) {
    $differences = @(Get-AtfSnapshotDifferences $Snapshot $Stage $SourceRoot)
    if ($differences.Count) { throw "QUALIFICATION-SNAPSHOT-CHANGED: $($differences -join ', ')" }
}

function New-AtfQualificationSnapshot([string]$SourceRoot, [string]$Stage, [string]$RunRoot, [string]$PinnedSourceInputs) {
    $sources = @(& (Join-Path $PSScriptRoot 'Get-AtfSourceInputs.ps1') -SourceRoot $SourceRoot)
    $release = @(Get-AtfReleaseInputs $SourceRoot)
    $snapshot = [pscustomobject]@{
        source=ConvertTo-AtfInputJson $sources
        release=ConvertTo-AtfInputJson $release
        inputCount=$sources.Count
    }
    if ($PinnedSourceInputs) {
        $pinned = Get-Content -LiteralPath $PinnedSourceInputs -Raw | ConvertFrom-Json -NoEnumerate
        if ($pinned -isnot [array] -or
            (ConvertTo-AtfInputJson @($pinned | Select-Object path,sha256)) -cne $snapshot.source) {
            throw 'QUALIFICATION-SOURCE-DIFFERS-FROM-PLAN'
        }
    }

    $sourceManifest = Join-Path $RunRoot 'source-inputs.json'
    $releaseManifest = Join-Path $RunRoot 'release-inputs.json'
    [IO.File]::WriteAllText($sourceManifest, $snapshot.source)
    [IO.File]::WriteAllText($releaseManifest, $snapshot.release)
    $copyManifest = Join-Path (Split-Path -Parent $Stage) 'qualification-copy-inputs.json'
    [IO.File]::WriteAllText($copyManifest, (ConvertTo-AtfInputJson ($sources + $release)))
    & (Join-Path $PSScriptRoot 'Copy-AtfSourceInputs.ps1') -SourceRoot $SourceRoot -Destination $Stage -InputManifest $copyManifest
    Assert-AtfQualificationSnapshot $snapshot $Stage $SourceRoot
    $preflight = Join-Path $Stage 'atf-tests/tools/check-release-artifacts.mjs'
    & node $preflight $Stage *> (Join-Path $RunRoot 'release-preflight.log')
    if ($LASTEXITCODE -ne 0) {
        throw "RELEASE-PREFLIGHT: $(Get-Content -LiteralPath (Join-Path $RunRoot 'release-preflight.log') -Raw)"
    }
    $snapshot
}
