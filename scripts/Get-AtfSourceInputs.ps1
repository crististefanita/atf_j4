#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$SourceRoot,
    [switch]$AsJson
)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath($SourceRoot)
if (-not (Test-Path -LiteralPath $root -PathType Container)) { throw 'Rădăcina surselor nu există.' }
$policy = Get-Content -LiteralPath (Join-Path $root 'config/atf-governance-policy.json') -Raw | ConvertFrom-Json
$boundaries = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
$protected = [Collections.Generic.List[string]]::new()
$null = $boundaries.Add('')
function Get-RelativeInputPath([string]$path) {
    $absolute = [IO.Path]::GetFullPath((Join-Path $root $path))
    if ([IO.Path]::IsPathRooted($path) -or $absolute.Equals($root,[StringComparison]::OrdinalIgnoreCase) -or
        -not $absolute.StartsWith($root + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
        throw "Cale configurată în afara surselor: $path"
    }
    return [IO.Path]::GetRelativePath($root,$absolute).Replace('\','/')
}
foreach ($module in @($policy.mavenModules) + @($policy.additionalSourceRoots | ForEach-Object module)) {
    $relative = Get-RelativeInputPath $module
    $protected.Add($relative + '/src')
    $segments = $relative.Split('/')
    for ($i = 1; $i -le $segments.Length; $i++) { $null = $boundaries.Add(($segments[0..($i-1)] -join '/')) }
}
foreach ($buildRoot in $policy.buildRoots) {
    $relative = Get-RelativeInputPath $buildRoot
    if (-not (Test-Path -LiteralPath (Join-Path $root "$relative/settings.gradle")) -or
        -not (Test-Path -LiteralPath (Join-Path $root "$relative/build.gradle"))) {
        throw "Rădăcină Gradle fără settings și build explicite: $relative"
    }
    $null = $boundaries.Add($relative)
}
foreach ($source in $policy.additionalSourceRoots) { $protected.Add((Get-RelativeInputPath $source.path)) }
foreach ($module in $policy.mavenModules) {
    [xml]$pom = Get-Content -LiteralPath (Join-Path $root "$module/pom.xml") -Raw
    foreach ($name in @('sourceDirectory','testSourceDirectory')) {
        $node = $pom.SelectSingleNode("/*[local-name()='project']/*[local-name()='build']/*[local-name()='$name']")
        if ($null -ne $node) {
            $path = $node.InnerText.Trim()
            if (-not $path -or $path.Contains('${') -or [IO.Path]::IsPathRooted($path)) { throw "Rădăcină Maven neliterală: $module/$name" }
            $protected.Add((Get-RelativeInputPath "$module/$path"))
        }
    }
}
$excluded = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
foreach ($boundary in $boundaries) {
    foreach ($name in @('.git','.locks','.gradle','.idea','target','build','artifacts','dist','node_modules','.presentation-build')) {
        $candidate = if ($boundary) { "$boundary/$name" } else { $name }
        if (-not @($protected | Where-Object { $_ -eq $candidate -or $_.StartsWith($candidate + '/',[StringComparison]::OrdinalIgnoreCase) }).Count) {
            $null = $excluded.Add($candidate)
        }
    }
}
$null = $excluded.Add('release')
$rows = [Collections.Generic.List[object]]::new()
function Visit-InputDirectory([string]$directory) {
    foreach ($entry in Get-ChildItem -LiteralPath $directory -Force) {
        $relative = [IO.Path]::GetRelativePath($root,$entry.FullName).Replace('\','/')
        if ($entry.PSIsContainer -and $excluded.Contains($relative)) { continue }
        if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Intrare indirectă neinventariată: $relative" }
        if ($entry.PSIsContainer) { Visit-InputDirectory $entry.FullName }
        else { $rows.Add([pscustomobject]@{path=$relative;sha256=(Get-FileHash -LiteralPath $entry.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}) }
    }
}
Visit-InputDirectory $root
$rows.Sort([System.Comparison[object]]{
    param($left,$right)
    [StringComparer]::Ordinal.Compare([string]$left.path,[string]$right.path)
})
$result = $rows.ToArray()
if ($AsJson) { ConvertTo-Json -InputObject $result -Depth 5 -Compress } else { $result }
