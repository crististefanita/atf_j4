#requires -Version 7.0
param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
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
$classPath = Get-AtfJvmCompilerClasspath -ToolRoot $toolRoot -MavenCache $MavenCache -Core
$arguments = @()
$result = 1
Invoke-AtfJvmCli -ToolRoot $toolRoot -ProjectRoot $project -ScratchRoot $ScratchRoot -JavaHome $JavaHome `
    -ClassPath $classPath -MainClass 'atf.tests.jvm.cli.JvmInventoryCli' -Arguments $arguments -Result ([ref]$result)
exit $result
