#requires -Version 7.0
param(
    [Parameter(Mandatory)][ValidatePattern('^[a-z][a-z0-9_.:-]*$')][string]$Stage,
    [Parameter(Mandatory)][string]$TimingFile,
    [Parameter(Mandatory)][scriptblock]$Command
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'AtfQualificationTiming.ps1')
$file = [IO.Path]::GetFullPath($TimingFile)
$root = Split-Path -Parent $PSScriptRoot
$parent = (Resolve-Path -LiteralPath (Split-Path -Parent $file)).ProviderPath
if ($parent -ieq $root -or $parent.StartsWith($root + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
    throw 'A stage timing receipt must stay outside the project.'
}
$timing = New-AtfQualificationTiming 'release-operation' $file
$exit = Invoke-AtfQualificationPhase $timing $Stage $Command -ExitCode
if ($exit -ne 0) { throw "Stage $Stage failed with exit code $exit. Timing: $file" }
