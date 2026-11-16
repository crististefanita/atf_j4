#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$WorkRoot,
    [ValidateRange(0,65535)][int]$Port = 8714,
    [string]$ListenAddress = '127.0.0.1',
    [switch]$BuildOnly
)
$checkout = Join-Path $PSScriptRoot '..'
$sources = @(
    Join-Path $PSScriptRoot 'whist/src/main/java'
    Join-Path $checkout 'whist-lab-app/game-core/src/main/java'
    Join-Path $checkout 'whist-lab-app/app/src/main/java/ro/whistlab/app/bidding/HumanBidSelection.java'
)
& (Join-Path $PSScriptRoot 'Start-CardTableBrowser.ps1') -WorkRoot $WorkRoot -GameRoot $checkout `
    -JavaInputs $sources -MainClass 'ro.whistlab.browser.WhistBrowser' -Port $Port `
    -ListenAddress $ListenAddress -BuildOnly:$BuildOnly
