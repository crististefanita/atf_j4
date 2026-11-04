param([string]$RunRoot = '', [string]$AndroidSdk = 'C:\tools\whist-android-sdk')
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if([string]::IsNullOrWhiteSpace($RunRoot)){$RunRoot='C:\work\whist_back\rules-components\'+(Get-Date -Format 'yyyyMMdd-HHmmss')}
$RunRoot=[IO.Path]::GetFullPath($RunRoot)
if(Test-Path -LiteralPath (Join-Path $RunRoot 'evidence')){throw 'Choose a fresh run folder to avoid mixing old and current results.'}
New-Item -ItemType Directory -Force (Join-Path $RunRoot 'evidence'),(Join-Path $RunRoot 'components'),(Join-Path $RunRoot 'coverage') | Out-Null
$savedAndroidHome=$env:ANDROID_HOME;$savedAndroidSdk=$env:ANDROID_SDK_ROOT
Push-Location $root
try {
    $env:ANDROID_HOME=$AndroidSdk;$env:ANDROID_SDK_ROOT=$AndroidSdk
    Push-Location (Join-Path $root 'whist-lab-app')
    try {
        & .\gradlew.bat --no-daemon --console=plain --continue :game-core:test :game-core:aiTournamentTest :game-core:jacocoTestReport :app:assembleDebug "-Pwhist.componentEvidence=$RunRoot\evidence" *> (Join-Path $RunRoot 'game-core.log')
        $gradleExit=$LASTEXITCODE
    }finally{Pop-Location}
    Copy-Item -Path 'whist-lab-app\game-core\build\test-results\test\TEST-*.xml' -Destination (Join-Path $RunRoot 'components')
    Copy-Item -Path 'whist-lab-app\game-core\build\test-results\aiTournamentTest\TEST-*.xml' -Destination (Join-Path $RunRoot 'components')
    if(Test-Path 'whist-lab-app\game-core\build\reports\jacoco\test\jacocoTestReport.xml') {
        Copy-Item -Path 'whist-lab-app\game-core\build\reports\jacoco\test\*' -Destination (Join-Path $RunRoot 'coverage') -Recurse
    }
    $installLog=Join-Path $RunRoot 'reactor-install.log'
    & mvn -DskipTests install *> $installLog
    if($LASTEXITCODE -ne 0){throw "Local reactor install failed before Extent publication. See $installLog"}
    & mvn -q -pl atf-tests test '-Dtest=RuleComponentExtentReportTest' '-Datf.rules.publish=true' "-Datf.rules.runDir=$RunRoot" *> (Join-Path $RunRoot 'extent-publisher.log')
    $publisherExit=$LASTEXITCODE
    if($gradleExit -ne 0 -or $publisherExit -ne 0){throw "Component acceptance failed. See retained evidence and logs in $RunRoot"}
    Set-Content -LiteralPath (Join-Path (Split-Path -Parent $RunRoot) 'latest-run.txt') -Value $RunRoot -Encoding utf8
    Write-Output (Join-Path $RunRoot 'component-extent-report.html')
}finally{
    Pop-Location;$env:ANDROID_HOME=$savedAndroidHome;$env:ANDROID_SDK_ROOT=$savedAndroidSdk
}
