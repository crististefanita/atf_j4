#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$EvidenceRoot,
    [Parameter(Mandatory)][string]$MavenClasspathFile
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $EvidenceRoot).ProviderPath
$owner = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).ProviderPath
$jar = Join-Path $owner 'lib/atf-reporting-1.0.0-j24.jar'
$dependencies = (Get-Content -LiteralPath $MavenClasspathFile -Raw).Trim()
$classpath = @($jar,$dependencies) -join [IO.Path]::PathSeparator
& java -cp $classpath (Join-Path $PSScriptRoot 'BrowserExtent.java') $root (Join-Path $root 'extent-browser.html')
if ($LASTEXITCODE -ne 0) { throw 'Browser Extent publication failed.' }
