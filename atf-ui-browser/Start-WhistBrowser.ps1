#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$WorkRoot,
    [ValidateRange(0,65535)][int]$Port = 8714,
    [string]$ListenAddress = '127.0.0.1',
    [string]$CatalogSource,
    [string]$JsonLibrary,
    [string]$GradleExecutable,
    [switch]$BuildOnly
)
$checkout = Join-Path $PSScriptRoot '..'
$ErrorActionPreference = 'Stop'
$work = [IO.Path]::GetFullPath($WorkRoot)
$source = (Resolve-Path -LiteralPath $checkout).ProviderPath
if ($work -ieq $source -or $work.StartsWith($source + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
    throw 'Use an external browser work directory.'
}
if (!$CatalogSource) {
    $CatalogSource = & (Join-Path $PSScriptRoot 'Prepare-WhistCatalog.ps1') -WorkRoot $work -GradleExecutable $GradleExecutable
}
if (!$JsonLibrary) {
    $gradleHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
    $cache = Join-Path $gradleHome 'caches/modules-2/files-2.1/org.json/json/20240205'
    if (Test-Path -LiteralPath $cache) {
        $JsonLibrary = Get-ChildItem -LiteralPath $cache -Filter 'json-20240205.jar' -Recurse -File | Select-Object -First 1 -ExpandProperty FullName
    }
    if (!$JsonLibrary) { throw 'Supply -JsonLibrary with org.json:json:20240205, the Android JVM test dependency.' }
}
$sources = @(
    Join-Path $PSScriptRoot 'whist/src/main/java'
    Join-Path $checkout 'whist-lab-app/game-core/src/main/java'
    Join-Path $checkout 'whist-lab-app/app/src/main/java/ro/whistlab/app/bidding/HumanBidSelection.java'
    Join-Path $checkout 'whist-lab-app/app/src/main/java/ro/whistlab/app/localization/WhistUiText.java'
    Join-Path $checkout 'whist-lab-app/app/src/main/java/ro/whistlab/app/animation/TrickCollectionRequest.java'
    Join-Path $checkout 'whist-lab-app/app/src/main/java/ro/whistlab/app/theme/GameHeaderTypography.java'
    Join-Path $checkout 'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/theme/TableColors.java'
    $app = Join-Path $checkout 'whist-lab-app/app/src/main/java/ro/whistlab/app'
    foreach ($metric in 'table/TableLayoutMetrics','table/TrickCardMetrics','bidding/BidGridMetrics','cards/CardFaceMetrics','hand/HandLayoutMetrics') {
        Join-Path $app "$metric.java"
    }
    $animation = Join-Path $checkout 'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/animation'
    Join-Path $animation 'CollectionTarget.java'
    Join-Path $checkout 'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/turn/TurnPacing.java'
    $achievements = Join-Path $checkout 'whist-lab-app/app/src/main/java/ro/whistlab/app/achievements'
    foreach ($name in 'WhistAchievementCatalog','WhistAchievementCodec','WhistAchievementRepository','WhistAchievementState','WhistHandFacts','WhistMatchFacts') {
        Join-Path $achievements "$name.java"
    }
    Join-Path $achievements 'presentation/WhistAchievementPresentation.java'
    foreach ($part in 'persistence/GameCheckpointCodec','persistence/SavedNumbers','score/ScoreHistoryTable',
        'score/ScoreHistoryColumn','highscores/HighScores','highscores/HighScoreCodec',
        'highscores/HighScoreRepository','highscores/HighScorePresentation') {
        Join-Path $app "$part.java"
    }
    $CatalogSource
)
& (Join-Path $PSScriptRoot 'Start-CardTableBrowser.ps1') -WorkRoot $WorkRoot -GameRoot $checkout `
    -JavaInputs $sources -MainClass 'ro.whistlab.browser.WhistBrowser' -Port $Port `
    -ListenAddress $ListenAddress -BuildOnly:$BuildOnly -Libraries @($JsonLibrary) `
    -MainArguments @((Join-Path $work 'whist-profile-data'))
