#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$WorkRoot,
    [Parameter(Mandatory)][string]$Version
)
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot '../scripts/CardTableLibraryVersion.psm1') -Force
Assert-CardTableLibraryVersion -Version $Version -Existing $true
$checkout = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).ProviderPath
$root = [IO.Path]::GetFullPath($WorkRoot)
Assert-CardTableLibraryVersion -Version $Version -Existing (Test-Path -LiteralPath (Join-Path $root "atf-ui-browser-$Version"))
if ($root -ieq $checkout -or $root.StartsWith($checkout + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Use an external browser work directory.'
}
$source = Join-Path $PSScriptRoot 'src/main'
$tests = Join-Path $PSScriptRoot 'tests'
$starter = Join-Path $PSScriptRoot 'Start-CardTableBrowser.ps1'
$launch = @{WorkRoot=$root; GameRoot=$checkout; JavaInputs=@(Join-Path $source 'java'); MainClass='ro.cardtable.browser.tool.BrowserBuild'}
$testInputs = @(
    Join-Path $tests 'fixtures.mjs'
    Join-Path $tests 'packaged-sanity.mjs'
    Join-Path $tests 'hand-presentation.mjs'
    Join-Path $tests 'menu-presentation.mjs'
    Join-Path $tests 'score-presentation.mjs'
    Join-Path $tests 'trick-playback.mjs'
    Join-Path $tests 'shared-presentation.test.mjs'
    Join-Path $tests 'ScoreTypographyProbe.java'
    Join-Path $tests 'MenuHistoryProbe.java'
    Join-Path $tests 'menu-history-contract.mjs'
    Join-Path $tests 'menu-history.test.mjs'
    Join-Path $tests 'seat-presentation.mjs'
    Join-Path $tests 'viewport-contract.mjs'
    Join-Path $tests 'card-typography.mjs'
    Join-Path $checkout 'whist-lab-app/atf-card-table-ui/src/test/resources/card-typography.tsv'
    Join-Path $checkout 'whist-lab-app/atf-card-table-ui/src/test/resources/hand-feedback.tsv'
    Join-Path $tests 'trump-text-fit.mjs'
    Join-Path $checkout 'whist-lab-app/app/src/test/resources/whist-trump-text-fit.tsv'
)
function Get-CleanCommit {
    $commit = git -C $checkout rev-parse --verify HEAD
    if ($LASTEXITCODE -ne 0 -or $commit -cnotmatch '^[a-f0-9]{40}$') { throw 'The package needs a Git source commit.' }
    $changes = @(git -C $checkout status --porcelain)
    if ($LASTEXITCODE -ne 0 -or $changes.Count) { throw 'Build this version from a clean checkout.' }
    $commit
}
function Get-InputManifest {
    $files = @($PSCommandPath, $starter, (Join-Path $checkout 'scripts/CardTableLibraryVersion.psm1')) + @(& $starter @launch -ListSources) + $testInputs
    $files += @(Get-ChildItem -LiteralPath (Join-Path $source 'web') -Recurse -File | ForEach-Object FullName)
    $order = [string[]]$files
    [Array]::Sort($order, [StringComparer]::Ordinal)
    foreach ($file in $order) {
        $entry = Get-Item -LiteralPath $file
        $path = [IO.Path]::GetRelativePath($checkout, $entry.FullName).Replace('\', '/')
        if ($path.StartsWith('../') -or ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw "Invalid package input: $path" }
        [ordered]@{path=$path; sha256=(Get-FileHash -LiteralPath $entry.FullName).Hash.ToLowerInvariant()}
    }
}
function Get-InputHash($Manifest) {
    $bytes = [Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject @($Manifest) -Depth 4 -Compress))
    [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes)).ToLowerInvariant()
}

$sourceCommit = Get-CleanCommit
$inputs = @(Get-InputManifest)
$inputHash = Get-InputHash $inputs
$name = "atf-ui-browser-$Version"
$final = Join-Path $root $name
$receiptName = 'browser-build.json'
$jars = @("$name.jar", "$name-tests.jar")
$files = @($jars | ForEach-Object { $_; "$_.sha256" })

function Confirm-Package([string]$Directory) {
    $receipt = Get-Content -LiteralPath (Join-Path $Directory $receiptName) -Raw | ConvertFrom-Json -AsHashtable
    if ($receipt.schemaVersion -ne 1 -or $receipt.version -cne $Version -or
            $receipt.sourceCommit -cnotmatch '^[a-f0-9]{40}$' -or $receipt.inputSha256 -cne $inputHash -or
            (Get-InputHash $receipt.inputs) -cne $inputHash) {
        throw "Browser version $Version already exists with different inputs or invalid provenance. Use a new version for changed sources."
    }
    git -C $checkout merge-base --is-ancestor $receipt.sourceCommit $sourceCommit
    if ($LASTEXITCODE -ne 0) { throw 'The recorded browser source commit is outside the current history.' }
    if ($receipt.files.Count -ne $files.Count) { throw 'Invalid browser artifact inventory.' }
    foreach ($file in $files) {
        $rows = @($receipt.files | Where-Object path -CEQ $file)
        $item = Get-Item -LiteralPath (Join-Path $Directory $file)
        if ($rows.Count -ne 1 -or $item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -or
                $rows[0].bytes -ne $item.Length -or $rows[0].sha256 -cne (Get-FileHash -LiteralPath $item.FullName).Hash.ToLowerInvariant()) {
            throw "Browser artifact differs: $file"
        }
    }
    foreach ($jar in $jars) {
        $expected = ($receipt.files | Where-Object path -CEQ $jar).sha256
        if ((Get-Content -LiteralPath (Join-Path $Directory "$jar.sha256") -Raw).Trim() -cne $expected) {
            throw "Browser checksum does not identify $jar."
        }
    }
}
if (Test-Path -LiteralPath $final) {
    Confirm-Package $final
    $final
    return
}

$classes = & $starter @launch -BuildOnly
$staging = Join-Path (Split-Path $classes) 'distribution'
$null = New-Item -ItemType Directory -Path $staging
Copy-Item -LiteralPath (Join-Path $source 'web') -Destination (Join-Path $classes 'ro/cardtable/browser/web') -Recurse
$timestamp = '2026-01-01T00:00:00Z'
& jar --create --file (Join-Path $staging $jars[0]) "--date=$timestamp" -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Browser runtime packaging failed.' }
$jarInputs = @(foreach ($file in $testInputs) {
    '-C'
    Split-Path -Parent $file
    Split-Path -Leaf $file
})
& jar --create --file (Join-Path $staging $jars[1]) "--date=$timestamp" @jarInputs
if ($LASTEXITCODE -ne 0) { throw 'Browser sanity packaging failed.' }
foreach ($jar in $jars) {
    $file = Join-Path $staging $jar
    $sha = (Get-FileHash -LiteralPath $file).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText("$file.sha256", "$sha`n", [Text.Encoding]::ASCII)
}
if ((Get-CleanCommit) -cne $sourceCommit -or (Get-InputHash @(Get-InputManifest)) -cne $inputHash) {
    throw 'Browser source inputs changed during packaging.'
}
$artifacts = @(foreach ($file in $files) {
    $item = Get-Item -LiteralPath (Join-Path $staging $file)
    [ordered]@{path=$file; bytes=$item.Length; sha256=(Get-FileHash -LiteralPath $item.FullName).Hash.ToLowerInvariant()}
})
[ordered]@{schemaVersion=1; version=$Version; sourceCommit=$sourceCommit; inputSha256=$inputHash; inputs=$inputs; files=$artifacts} |
    ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $staging $receiptName) -Encoding utf8
Confirm-Package $staging
try { [IO.Directory]::Move($staging, $final) }
catch [IO.IOException] {
    if (!(Test-Path -LiteralPath $final -PathType Container)) { throw }
    Confirm-Package $final
}
$final
