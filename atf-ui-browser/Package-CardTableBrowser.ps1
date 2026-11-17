#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$WorkRoot,
    [Parameter(Mandatory)][string]$Version
)
$ErrorActionPreference = 'Stop'
if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw 'Expected a three-part release version.' }
$checkout = Join-Path $PSScriptRoot '..'
$source = Join-Path $PSScriptRoot 'src/main'
$classes = & (Join-Path $PSScriptRoot 'Start-CardTableBrowser.ps1') -WorkRoot $WorkRoot -GameRoot $checkout `
    -JavaInputs @(Join-Path $source 'java') -MainClass 'ro.cardtable.browser.tool.BrowserBuild' -BuildOnly
$versioned = Join-Path (Split-Path $classes) "atf-ui-browser-$Version"
$null = New-Item -ItemType Directory -Path $versioned
$web = Join-Path $classes 'ro/cardtable/browser/web'
Copy-Item -LiteralPath (Join-Path $source 'web') -Destination $web -Recurse
$name = 'atf-ui-browser-' + $Version
$runtime = Join-Path $versioned "$name.jar"
$sanity = Join-Path $versioned "$name-tests.jar"
$timestamp = '2026-01-01T00:00:00Z'
& jar --create --file $runtime "--date=$timestamp" -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Browser runtime packaging failed.' }
$tests = Join-Path $PSScriptRoot 'tests'
$handFixtures = Join-Path $checkout 'whist-lab-app/atf-card-table-ui/src/test/resources'
$whistFixtures = Join-Path $checkout 'whist-lab-app/app/src/test/resources'
& jar --create --file $sanity "--date=$timestamp" -C $tests fixtures.mjs -C $tests packaged-sanity.mjs `
    -C $tests hand-presentation.mjs -C $tests menu-presentation.mjs -C $handFixtures hand-feedback.tsv `
    -C $tests trump-text-fit.mjs -C $whistFixtures whist-trump-text-fit.tsv
if ($LASTEXITCODE -ne 0) { throw 'Browser sanity packaging failed.' }
foreach ($file in $runtime, $sanity) {
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $file).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText("$file.sha256", "$hash`n", [Text.Encoding]::ASCII)
}
$versioned
