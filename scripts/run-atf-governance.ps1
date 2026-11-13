#requires -Version 7.0
param(
    [string]$RunRoot = '',
    [string]$SourceRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$PinnedSourceInputs = '',
    [string]$Maven = 'mvn',
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenCache = (Join-Path $env:USERPROFILE '.m2/repository')
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'AtfQualificationSnapshot.ps1')
$sourceRoot = [IO.Path]::GetFullPath($SourceRoot)
if ([string]::IsNullOrWhiteSpace($RunRoot)) {
    $evidenceRoot = if ($env:WHIST_EVIDENCE_ROOT) { $env:WHIST_EVIDENCE_ROOT } else { 'C:\work\whist_back' }
    $RunRoot = Join-Path $evidenceRoot ('atf-governance\' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
}
$RunRoot = [IO.Path]::GetFullPath($RunRoot)
if ($RunRoot.Equals($sourceRoot, [StringComparison]::OrdinalIgnoreCase) -or
    $RunRoot.StartsWith($sourceRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Rezultatele trebuie păstrate în afara proiectului.'
}
if (Test-Path -LiteralPath $RunRoot) { throw 'Directorul de rezultate trebuie să fie nou.' }
$work = Join-Path ([IO.Path]::GetTempPath()) ('atf-quality-' + [guid]::NewGuid().ToString('N'))
$stage = Join-Path $work 'project'
New-Item -ItemType Directory -Path $stage, $RunRoot | Out-Null
$snapshot = New-AtfQualificationSnapshot $sourceRoot $stage $RunRoot $PinnedSourceInputs
$localRepository = Join-Path $work 'maven-repository'
New-Item -ItemType Directory -Path $localRepository | Out-Null
if (Test-Path -LiteralPath $MavenCache -PathType Container) {
    & robocopy $MavenCache $localRepository /E /R:1 /W:1 /XD atf .locks /XF *.lock *.tmp *.lastUpdated /NFL /NDL /NJH /NJS /NP *> (Join-Path $RunRoot 'copy-maven-cache.log')
    if ($LASTEXITCODE -gt 7) { throw 'Copierea cache-ului Maven a eșuat.' }
}
$savedJavaHome = $env:JAVA_HOME
try {
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    Push-Location $stage
    try {
        Assert-AtfQualificationSnapshot $snapshot $stage $sourceRoot
        & $Maven -B "-Dmaven.repo.local=$localRepository" -pl atf-tests -am '-Dtest=AtfQualityGovernanceSuite' '-Dsurefire.failIfNoSpecifiedTests=false' "-Datf.governance.root=$stage" "-Datf.governance.gitRoot=$sourceRoot" test *> (Join-Path $RunRoot 'maven.log')
        $testExit = $LASTEXITCODE
    } finally { Pop-Location }
} finally { $env:JAVA_HOME = $savedJavaHome }
$reports = Join-Path $stage 'atf-tests/target/surefire-reports'
if (Test-Path -LiteralPath $reports) { Copy-Item -LiteralPath $reports -Destination (Join-Path $RunRoot 'junit') -Recurse }
$differences = @(Get-AtfSnapshotDifferences $snapshot $stage $sourceRoot)
$stable = $differences.Count -eq 0
$result = [ordered]@{ root=$sourceRoot; stage=$stage; mavenRepository=$localRepository; output=$RunRoot
    sourceFiles=$snapshot.inputCount; sourceFingerprint=Get-AtfInputFingerprint $snapshot.source
    releaseFingerprint=Get-AtfInputFingerprint $snapshot.release; releaseInputManifest='release-inputs.json'
    stable=$stable; snapshotDifferences=$differences; testExit=$testExit; status=if ($stable -and $testExit -eq 0) {'PASS'} else {'FAIL'} }
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $RunRoot 'governance-result.json') -Encoding utf8
Write-Output ($result | ConvertTo-Json -Compress)
if (-not $stable) { throw 'Snapshot-ul de calificare s-a schimbat în timpul verificării. Consultați $RunRoot/governance-result.json.' }
if ($testExit -ne 0) { throw "Guvernanța a eșuat. Consultați $RunRoot\maven.log și rezultatele JUnit." }
