param(
    [ValidateSet('quick','regression','documentation','learning')][string]$Collection='quick',
    [string]$RunRoot='', [string]$Candidate='', [string]$CorpusRoot='', [string]$CandidateFreeze='',
    [ValidateSet('development','heldout')][string]$Phase='development',
    [string]$JavaHome='C:\tools\Java21\jdk-21.0.12.1+1',
    [string]$AndroidSdk='C:\tools\whist-android-sdk'
)
$ErrorActionPreference='Stop'
$sourceRoot=Split-Path -Parent $PSScriptRoot
$catalog=Get-Content -LiteralPath (Join-Path $sourceRoot 'config/test-collections.json') -Raw | ConvertFrom-Json
$selection=@($catalog.collections | Where-Object id -eq $Collection)
if($selection.Count -ne 1){throw 'Unknown collection'}
if($Collection -eq 'learning' -and ($Candidate -notmatch '^[a-z0-9-]+$' -or [string]::IsNullOrWhiteSpace($CorpusRoot))){throw 'Learning requires an explicit fresh candidate and external corpus folder.'}
if([string]::IsNullOrWhiteSpace($RunRoot)){$RunRoot='C:\work\whist_back\collections\'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+$Collection}
$RunRoot=[IO.Path]::GetFullPath($RunRoot)
if($RunRoot.StartsWith([IO.Path]::GetFullPath($sourceRoot)+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Evidence belongs outside the live project.'}
if(Test-Path -LiteralPath $RunRoot){throw 'Fresh output directory required; results are immutable.'}
if($Collection -eq 'learning' -and $Phase -eq 'heldout'){
    if(-not (Test-Path -LiteralPath $CandidateFreeze)){throw 'Heldout requires a candidate freeze created before opening heldout.'}
    $freeze=Get-Content -LiteralPath $CandidateFreeze -Raw | ConvertFrom-Json
    if($freeze.status -ne 'FROZEN_BEFORE_HELDOUT' -or $freeze.candidate -ne $Candidate){throw 'Candidate freeze identity mismatch'}
}
$stageRoot=Join-Path ([IO.Path]::GetTempPath()) ('ATF_J4_COLLECTION_'+[guid]::NewGuid().ToString('N'))
$stageApp=Join-Path $stageRoot 'whist-lab-app'
New-Item -ItemType Directory -Path $RunRoot,$stageApp | Out-Null
$inventory=@()
$appSource=Join-Path $sourceRoot 'whist-lab-app'
foreach($file in Get-ChildItem -LiteralPath $appSource -Recurse -File){
    $relative=[IO.Path]::GetRelativePath($appSource,$file.FullName)
    if(($relative -split '[\\/]') | Where-Object {$_ -in @('.git','.gradle','build')}){continue}
    $target=Join-Path $stageApp $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Copy-Item -LiteralPath $file.FullName -Destination $target
    $inventory+=@{file=$relative;sha256=(Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()}
}
if($Collection -eq 'learning' -and $Phase -eq 'heldout'){Copy-Item -LiteralPath $CandidateFreeze -Destination (Join-Path $RunRoot 'candidate-freeze.json')}
$inventory | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $RunRoot 'source-inventory.json') -Encoding utf8
$arguments=@('--no-daemon','--console=plain',$selection[0].task)
if($Collection -eq 'documentation'){$arguments+=@("-Pwhist.documentationEvidence=$RunRoot","-Pwhist.documentationMarkdown=$RunRoot\rules","-Pwhist.documentationRunId=$([IO.Path]::GetFileName($RunRoot))")}
if($Collection -eq 'learning'){$arguments+=@("-Pwhist.round5.output=$RunRoot","-Pwhist.round5.corpus=$CorpusRoot","-Pwhist.round5.candidate=$Candidate","-Pwhist.round5.phase=$Phase")}
$savedJava=$env:JAVA_HOME;$savedAndroid=$env:ANDROID_HOME;$savedPath=$env:Path
$began=[DateTimeOffset]::UtcNow;$watch=[Diagnostics.Stopwatch]::StartNew();$exitCode=1
Push-Location $stageApp
try{
    $env:JAVA_HOME=$JavaHome;$env:ANDROID_HOME=$AndroidSdk;$env:Path="$JavaHome\bin;$savedPath"
    & .\gradlew.bat @arguments *> (Join-Path $RunRoot 'execution.log')
    $exitCode=$LASTEXITCODE
    $results=Join-Path $stageApp 'game-core/build/test-results'
    if(Test-Path -LiteralPath $results){Copy-Item -LiteralPath $results -Destination (Join-Path $RunRoot 'native-results') -Recurse}
}finally{
    $watch.Stop();Pop-Location;$env:JAVA_HOME=$savedJava;$env:ANDROID_HOME=$savedAndroid;$env:Path=$savedPath
    @{collection=$Collection;startedAt=$began.ToString('o');endedAt=[DateTimeOffset]::UtcNow.ToString('o');executionMilliseconds=$watch.ElapsedMilliseconds;exitCode=$exitCode;status=$(if($exitCode -eq 0){'PASS'}else{'FAIL'});stage=$stageRoot;reportScope='Native collection execution only; no inferred Extent, device or complete-game acceptance.'} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $RunRoot 'collection-result.json') -Encoding utf8
}
if($exitCode -ne 0){throw "Collection failed; preserved results: $RunRoot"}
Write-Output $RunRoot
