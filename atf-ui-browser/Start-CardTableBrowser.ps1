#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$WorkRoot,
    [Parameter(Mandatory)][string]$GameRoot,
    [Parameter(Mandatory)][string[]]$JavaInputs,
    [Parameter(Mandatory)][string]$MainClass,
    [ValidateRange(0,65535)][int]$Port,
    [string]$ListenAddress = '127.0.0.1',
    [switch]$BuildOnly
)
$ErrorActionPreference = 'Stop'
$checkout = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).ProviderPath
$output = [IO.Path]::GetFullPath($WorkRoot)
foreach ($source in $checkout, (Resolve-Path -LiteralPath $GameRoot).ProviderPath) {
    $prefix = $source.TrimEnd([char[]]@('/', '\')) + [IO.Path]::DirectorySeparatorChar
    if ($output.Equals($source, [StringComparison]::OrdinalIgnoreCase) -or
            $output.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Use an external browser work directory.'
    }
}
$build = Join-Path $output ('browser-' + [guid]::NewGuid().ToString('N'))
$classes = Join-Path $build 'classes'
$null = New-Item -ItemType Directory -Path $classes -Force
$inputs = @(
    Join-Path $PSScriptRoot 'src/main/java'
    $ui = Join-Path $checkout 'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui'
    foreach ($contract in 'TableSeatLayout','TableSizing','TableGeometry','TableViewportPolicy','PlayedCardMetrics') {
        Join-Path $ui "table/$contract.java"
    }
    Join-Path $ui 'hand/HandLayoutMetrics.java'
    Join-Path $ui 'cards/CardFaceMetrics.java'
    Join-Path $checkout 'whist-lab-app/card-table-names/src/main/java'
) + $JavaInputs
$sources = foreach ($inputPath in $inputs) {
    $item = Get-Item -LiteralPath $inputPath
    if ($item.PSIsContainer) {
        Get-ChildItem -LiteralPath $item.FullName -Recurse -Filter '*.java' -File | ForEach-Object FullName
    } else {
        $item.FullName
    }
}
function Format-JavacPath([string]$Path) {
    '"' + $Path.Replace('\', '/') + '"'
}
$argFile = Join-Path $build 'javac.args'
$arguments = @('--release', '17', '-encoding', 'UTF-8', '-d', (Format-JavacPath $classes))
$arguments += @($sources | Sort-Object -Unique | ForEach-Object { Format-JavacPath $_ })
[IO.File]::WriteAllLines($argFile, [string[]]$arguments, [Text.UTF8Encoding]::new($false))
& javac "@$argFile"
if ($LASTEXITCODE -ne 0) { throw 'Browser adapter compilation failed.' }
if ($BuildOnly) {
    $classes
    return
}
& java '-cp' $classes $MainClass (Join-Path $PSScriptRoot 'src/main/web') $ListenAddress $Port
if ($LASTEXITCODE -ne 0) { throw 'Browser server stopped with an error.' }
