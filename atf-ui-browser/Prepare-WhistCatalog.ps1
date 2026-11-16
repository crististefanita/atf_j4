#requires -Version 7.0
param([Parameter(Mandatory)][string]$WorkRoot, [string]$GradleExecutable)
$ErrorActionPreference = 'Stop'
$project = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../whist-lab-app')).ProviderPath
$run = Join-Path $WorkRoot ('catalogue-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $run -Force
if (!$GradleExecutable) { $GradleExecutable = Join-Path $project 'gradlew.bat' }
$init = Join-Path $run 'external.gradle'
@'
gradle.beforeProject { project ->
    def name = project.path == ':' ? 'root' : project.path.replace(':', '_')
    project.layout.buildDirectory.set(new File(gradle.startParameter.projectProperties['atf.browser.catalogueRoot'], name))
}
'@ | Set-Content -LiteralPath $init -Encoding utf8
$build = Join-Path $run 'build'
& $GradleExecutable --offline --no-daemon --max-workers=2 -p $project --project-cache-dir (Join-Path $run 'cache') `
    "-Patf.browser.catalogueRoot=$build" -I $init :app:generateWhistAchievementCatalog *> (Join-Path $run 'gradle.log')
if ($LASTEXITCODE -ne 0) { throw "Whist catalogue generation failed. See $run/gradle.log" }
(Resolve-Path -LiteralPath (Join-Path $build '_app/generated/sources/whistAchievements/java')).ProviderPath
