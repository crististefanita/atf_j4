#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$SourceRoot,
    [Parameter(Mandatory)][string]$Destination,
    [Parameter(Mandatory)][string]$InputManifest
)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath($SourceRoot)
$stage = [IO.Path]::GetFullPath($Destination)
if ($stage.Equals($root,[StringComparison]::OrdinalIgnoreCase) -or
    $stage.StartsWith($root + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
    throw 'Staging-ul trebuie păstrat în afara surselor.'
}
$inputs = Get-Content -LiteralPath $InputManifest -Raw | ConvertFrom-Json
foreach ($inputFile in $inputs) {
    $source = [IO.Path]::GetFullPath((Join-Path $root $inputFile.path))
    $target = [IO.Path]::GetFullPath((Join-Path $stage $inputFile.path))
    if ([IO.Path]::IsPathRooted($inputFile.path) -or
        -not $source.StartsWith($root + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -or
        -not $target.StartsWith($stage + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
        throw "SOURCE-STAGE-ESCAPE: $($inputFile.path)"
    }
    $null = New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force
    Copy-Item -LiteralPath $source -Destination $target
    if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ine $inputFile.sha256) {
        throw "SOURCE-STAGE-CHANGED: $($inputFile.path)"
    }
}
