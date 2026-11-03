param(
    [ValidateSet('quick','regression','strategy','documentation','learning','arena')][string]$Collection='quick',
    [string]$RunRoot='', [string]$Candidate='', [string]$CorpusRoot='', [string]$CandidateFreeze='', [string]$ProtocolFile='', [string]$CasesFile='',
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
if($Collection -eq 'learning'){
    if(-not (Test-Path -LiteralPath $ProtocolFile)){throw 'Learning requires an explicit protocol JSON with the selected phase seed range.'}
    $protocol=Get-Content -LiteralPath $ProtocolFile -Raw | ConvertFrom-Json
    $range=$protocol.$Phase
    $declaredRoles=@("expert","player")
    if($null -ne $protocol.roles){$declaredRoles=@($protocol.roles);if($declaredRoles.Count -lt 1 -or $declaredRoles.Count -gt 2 -or @($declaredRoles | Select-Object -Unique).Count -ne $declaredRoles.Count -or @($declaredRoles | Where-Object {$_ -notin @("expert","player")}).Count){throw "Invalid declared protocol roles."}}
    if($protocol.version -notin @(7,11) -or $null -eq $range){throw 'An explicit schema7/11 protocol and selected phase are required; historical protocols use their retained source snapshots.'}
    if($protocol.version -eq 11 -and ($null -eq $protocol.baselines -or $protocol.baselines.expert -ne "EXPERT_V10" -or $protocol.baselines.player -ne "PLAYER_V8" -or (@($protocol.opponents | Sort-Object) -join ",") -ne "EXPERT_V10,PLAYER_V8")){throw "Round11 requires its frozen ExpertV10/IndependentV8 baselines and opponent families."}
    if(($range.firstSeed -isnot [long] -and $range.firstSeed -isnot [int]) -or ($range.seeds -isnot [long] -and $range.seeds -isnot [int])){throw 'Seed range must contain integers.'}
    if($range.firstSeed -lt 0 -or $range.seeds -lt 1 -or $range.seeds -gt 1000 -or ([long]$range.firstSeed+[long]$range.seeds) -gt [int]::MaxValue){throw 'Invalid declared seed range.'}
}
if($Collection -eq 'arena'){
    if([string]::IsNullOrWhiteSpace($CorpusRoot) -or -not (Test-Path -LiteralPath $ProtocolFile -PathType Leaf)){throw 'Arena requires an external corpus and protocol JSON.'}
    $arenaProtocol=Get-Content -LiteralPath $ProtocolFile -Raw | ConvertFrom-Json
    if($arenaProtocol.schemaVersion -ne 1 -or $arenaProtocol.profile -ne 'ROMANIAN4_SERIES10'){throw 'Unsupported arena protocol.'}
}
if($CasesFile -and ($Collection -ne "learning" -or $protocol.version -notin @(7,11) -or -not (Test-Path -LiteralPath $CasesFile -PathType Leaf))){throw "Case selection requires a current Round7/Round11 learning protocol and an existing selection file."}
$stageRoot=Join-Path ([IO.Path]::GetTempPath()) ('ATF_J4_COLLECTION_'+[guid]::NewGuid().ToString('N'))
$stageApp=Join-Path $stageRoot 'whist-lab-app'
New-Item -ItemType Directory -Path $RunRoot,$stageApp | Out-Null
$inventory=@()
if($Collection -in @("learning","arena")){Copy-Item -LiteralPath $ProtocolFile -Destination (Join-Path $RunRoot "protocol.json")}
if($CasesFile){$stagedCasesFile=Join-Path $RunRoot "case-selection.json";Copy-Item -LiteralPath $CasesFile -Destination $stagedCasesFile}
$appSource=Join-Path $sourceRoot 'whist-lab-app'
foreach($file in Get-ChildItem -LiteralPath $appSource -Recurse -File){
    $relative=[IO.Path]::GetRelativePath($appSource,$file.FullName)
    if(($relative -split '[\\/]') | Where-Object {$_ -in @('.git','.gradle','build')}){continue}
    $target=Join-Path $stageApp $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Copy-Item -LiteralPath $file.FullName -Destination $target
    $inventory+=@{file=$relative;sha256=(Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()}
}
if($Collection -eq 'learning' -and $Phase -eq 'heldout'){
    foreach($property in $freeze.sourceHashes.PSObject.Properties){
        $candidateFile=Join-Path $stageApp $property.Name
        if(-not (Test-Path -LiteralPath $candidateFile) -or (Get-FileHash -LiteralPath $candidateFile -Algorithm SHA256).Hash.ToLowerInvariant() -ne $property.Value){throw "Frozen source differs: $($property.Name)"}
    }
    Copy-Item -LiteralPath $CandidateFreeze -Destination (Join-Path $RunRoot 'candidate-freeze.json')
    $registryFolder=Join-Path $CorpusRoot 'holdout-use'
    New-Item -ItemType Directory -Force -Path $registryFolder | Out-Null
    $registry=Join-Path $registryFolder ("romanian4-$($range.firstSeed)-$([long]$range.firstSeed+[long]$range.seeds-1).json")
    if(Test-Path -LiteralPath $registry){
        $used=Get-Content -LiteralPath $registry -Raw | ConvertFrom-Json
        if($used.candidate -ne $Candidate){throw 'Evaluation range already belongs to another frozen candidate.'}
        foreach($property in $freeze.sourceHashes.PSObject.Properties){if($used.sourceHashes.($property.Name) -ne $property.Value){throw 'Evaluation source changed after reservation.'}}
    }else{
        $reservation=@{candidate=$Candidate;sourceHashes=$freeze.sourceHashes;reservedAt=[DateTimeOffset]::UtcNow.ToString('o');run=$RunRoot} | ConvertTo-Json -Depth 8
        $stream=[IO.File]::Open($registry,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
        $writer=[IO.StreamWriter]::new($stream,[Text.UTF8Encoding]::new($false))
        try{$writer.Write($reservation)}finally{$writer.Dispose()}
    }
}

$inventory | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $RunRoot 'source-inventory.json') -Encoding utf8
$arguments=@('-p','algorithm-build','--offline','--no-daemon','--console=plain',$selection[0].task)
if($Collection -eq 'learning'){if($protocol.version -eq 11){$arguments[-1]=':game-core:roundElevenTest';$learningPrefix='whist.round11'}else{$arguments[-1]=':game-core:roundSevenTest';$learningPrefix='whist.round7'}}
if($Collection -eq 'documentation'){$arguments+=@("-Pwhist.documentationEvidence=$RunRoot","-Pwhist.documentationMarkdown=$RunRoot\rules","-Pwhist.documentationRunId=$([IO.Path]::GetFileName($RunRoot))")}
if($Collection -eq 'learning'){$arguments+=@("-P$learningPrefix.output=$RunRoot","-P$learningPrefix.corpus=$CorpusRoot","-P$learningPrefix.candidate=$Candidate","-P$learningPrefix.phase=$Phase","-P$learningPrefix.firstSeed=$($range.firstSeed)","-P$learningPrefix.seeds=$($range.seeds)")}
if($Collection -eq "learning" -and $declaredRoles.Count -eq 1){$arguments+=("-P$learningPrefix.role="+$declaredRoles[0])}
if($CasesFile){$arguments+=("-P$learningPrefix.casesFile="+$stagedCasesFile)}
if($Collection -eq 'learning' -and $null -ne $protocol.baselines){
    if($protocol.version -eq 7 -and ($protocol.baselines.expert -ne 'EXPERT_V6' -or $protocol.baselines.player -notin @('PLAYER_V6','PLAYER_V8'))){throw 'Unsupported frozen role baseline.'}
    $arguments+=("-P$learningPrefix.playerBaseline="+$protocol.baselines.player)
}
if($Collection -eq "learning" -and $null -eq $protocol.baselines){$arguments+=("-P$learningPrefix.playerBaseline=PLAYER_V6")}
if($Collection -eq 'arena'){$arguments+=@("-Pwhist.arena.protocol=$RunRoot\protocol.json","-Pwhist.arena.output=$RunRoot","-Pwhist.arena.corpus=$CorpusRoot","-Pwhist.componentEvidence=$RunRoot")}
if($Collection -eq 'strategy'){$arguments+=("-Pwhist.componentEvidence=$RunRoot")}
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
