#requires -Version 7.0
param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string[]]$File = @(),
    [switch]$Json,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenCache = (Join-Path $env:USERPROFILE '.m2/repository'),
    [string]$ScratchRoot = ''
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'AtfLocalPaths.ps1')
$ScratchRoot = Get-AtfEvidenceRoot $ScratchRoot
$toolRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).ProviderPath
$project = (Resolve-Path -LiteralPath $ProjectRoot).ProviderPath
. (Join-Path $PSScriptRoot 'AtfStandaloneJava.ps1')
$classPath = Get-AtfJvmCompilerClasspath -ToolRoot $toolRoot -MavenCache $MavenCache -Yaml
$arguments = @()
foreach ($path in $File) { $arguments += @('--file', $path) }
if ($Json) { $arguments += '--json' }
$result = 1
Invoke-AtfJvmCli -ToolRoot $toolRoot -ProjectRoot $project -ScratchRoot $ScratchRoot -JavaHome $JavaHome `
    -ClassPath $classPath -MainClass 'atf.tests.documentation.cli.DocumentationGovernanceCli' -Arguments $arguments -Result ([ref]$result)
exit $result
