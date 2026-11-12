param(
    [ValidateSet('cost','context','quick','regression','strategy','documentation','learning','arena','research')][string]$Collection='quick',
    [string]$RunRoot='', [string]$Candidate='', [string]$CorpusRoot='', [string]$CandidateFreeze='', [string]$ProtocolFile='', [string]$CasesFile='', [string]$ResearchCandidate='', [string]$CandidateOverlay='',
    [ValidateSet('development','heldout')][string]$Phase='development', [switch]$CaptureComponents,
    [string]$CostProfile='',
    [string]$JavaHome='C:\tools\Java21\jdk-21.0.12.1+1',
    [string]$AndroidSdk='C:\tools\whist-android-sdk'
)
$ErrorActionPreference='Stop'
function Read-JsonSnapshot([string]$File){
    $bytes=[IO.File]::ReadAllBytes([IO.Path]::GetFullPath($File))
    $stream=[IO.MemoryStream]::new($bytes,$false)
    $reader=[IO.StreamReader]::new($stream,[Text.Encoding]::UTF8,$true)
    try{$value=$reader.ReadToEnd() | ConvertFrom-Json}finally{$reader.Dispose()}
    return @{bytes=$bytes;value=$value}
}
if($ResearchCandidate -and $CandidateOverlay){throw 'ResearchCandidate and CandidateOverlay cannot be combined.'}
$sourceRoot=Split-Path -Parent $PSScriptRoot
$evaluationPolicy=Get-Content -LiteralPath (Join-Path $sourceRoot 'config/whist-evaluation-policy.json') -Raw | ConvertFrom-Json
if($evaluationPolicy.schemaVersion -ne 1 -or -not [IO.Path]::IsPathRooted($evaluationPolicy.registryRoot)){throw 'Invalid installation-wide evaluation registry policy.'}
$evaluationRegistryRoot=$evaluationPolicy.registryRoot
$catalog=Get-Content -LiteralPath (Join-Path $sourceRoot 'config/test-collections.json') -Raw | ConvertFrom-Json
$selection=@($catalog.collections | Where-Object id -eq $Collection)
if($selection.Count -ne 1){throw 'Unknown collection'}
if($CostProfile -and $Collection -ne 'cost'){throw 'CostProfile requires the cost collection.'}
if($Collection -eq 'cost'){
    if(-not $CostProfile){$CostProfile=Join-Path $sourceRoot 'config/whist-ai-cost-profile.json'}
    $costSnapshot=Read-JsonSnapshot $CostProfile
    if($costSnapshot.value.schemaVersion -ne 1){throw 'Unsupported AI cost profile.'}
}
if($Collection -eq 'learning' -and ($Candidate -notmatch '^[a-z0-9-]+$' -or [string]::IsNullOrWhiteSpace($CorpusRoot))){throw 'Learning requires an explicit fresh candidate and external corpus folder.'}
if([string]::IsNullOrWhiteSpace($RunRoot)){$RunRoot='C:\work\whist_back\collections\'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+$Collection}
$RunRoot=[IO.Path]::GetFullPath($RunRoot)
if($RunRoot.StartsWith([IO.Path]::GetFullPath($sourceRoot)+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Evidence belongs outside the live project.'}
if(Test-Path -LiteralPath $RunRoot){throw 'Fresh output directory required; results are immutable.'}
if($Collection -eq 'learning' -and $Phase -eq 'heldout'){
    if(-not (Test-Path -LiteralPath $CandidateFreeze)){throw 'Heldout requires a candidate freeze created before opening heldout.'}
    $freezeSnapshot=Read-JsonSnapshot $CandidateFreeze
    $freeze=$freezeSnapshot.value
    if($freeze.status -ne 'FROZEN_BEFORE_HELDOUT' -or $freeze.candidate -ne $Candidate){throw 'Candidate freeze identity mismatch'}
    if($null -eq $freeze.sourceHashes -or @($freeze.sourceHashes.PSObject.Properties).Count -eq 0){throw 'Candidate freeze requires explicit source hashes.'}
}
if($Collection -eq 'learning'){
    if(-not (Test-Path -LiteralPath $ProtocolFile)){throw 'Learning requires an explicit protocol JSON with the selected phase seed range.'}
    $protocolSnapshot=Read-JsonSnapshot $ProtocolFile
    $protocol=$protocolSnapshot.value
    $range=$protocol.$Phase
    $declaredRoles=@("expert","player")
    if($null -ne $protocol.roles){$declaredRoles=@($protocol.roles);if($declaredRoles.Count -lt 1 -or $declaredRoles.Count -gt 2 -or @($declaredRoles | Select-Object -Unique).Count -ne $declaredRoles.Count -or @($declaredRoles | Where-Object {$_ -notin @("expert","player")}).Count){throw "Invalid declared protocol roles."}}
    if($protocol.version -notin @(7,11,12,15,16,18) -or $null -eq $range){throw 'An explicit schema7/11/12/15/16/18 protocol and selected phase are required; historical protocols use their retained source snapshots.'}
    if($protocol.version -eq 11 -and ($null -eq $protocol.baselines -or $protocol.baselines.expert -ne "EXPERT_V10" -or $protocol.baselines.player -ne "PLAYER_V8" -or (@($protocol.opponents | Sort-Object) -join ",") -ne "EXPERT_V10,PLAYER_V8")){throw "Round11 requires its frozen ExpertV10/IndependentV8 baselines and opponent families."}
    if($protocol.version -eq 12 -and ($null -eq $protocol.baselines -or $protocol.baselines.expert -ne "EXPERT_V11" -or $protocol.baselines.player -ne "PLAYER_V11" -or (@($protocol.opponents | Sort-Object) -join ",") -ne "EXPERT_V11,PLAYER_V11")){throw "Round12 requires its frozen ExpertV11/IndependentV11 baselines and opponent families."}
    if($protocol.version -in @(15,16) -and ($null -eq $protocol.baselines -or $protocol.baselines.expert -ne "EXPERT_V14" -or $protocol.baselines.player -ne "PLAYER_V11" -or (@($protocol.opponents | Sort-Object) -join ",") -ne "EXPERT_V14,PLAYER_V11")){throw "Protocols15/16 require their frozen ExpertV14/IndependentV11 baselines and opponent families."}
    if($protocol.version -eq 18 -and ($null -eq $protocol.baselines -or $protocol.baselines.expert -ne "EXPERT_V17" -or $protocol.baselines.player -ne "PLAYER_V11" -or (@($protocol.opponents | Sort-Object) -join ",") -ne "EXPERT_V17,PLAYER_V11")){throw "Protocol18 requires corrected accepted ExpertV17/IndependentV11 baselines and opponent families."}
    if(($range.firstSeed -isnot [long] -and $range.firstSeed -isnot [int]) -or ($range.seeds -isnot [long] -and $range.seeds -isnot [int])){throw 'Seed range must contain integers.'}
    if($range.firstSeed -lt 0 -or $range.seeds -lt 1 -or $range.seeds -gt 1000 -or ([long]$range.firstSeed+[long]$range.seeds) -gt [int]::MaxValue){throw 'Invalid declared seed range.'}
}
if($Collection -eq 'arena'){
    if([string]::IsNullOrWhiteSpace($CorpusRoot) -or -not (Test-Path -LiteralPath $ProtocolFile -PathType Leaf)){throw 'Arena requires an external corpus and protocol JSON.'}
    $protocolSnapshot=Read-JsonSnapshot $ProtocolFile
    $arenaProtocol=$protocolSnapshot.value
    if($arenaProtocol.schemaVersion -ne 1 -or $arenaProtocol.profile -ne 'ROMANIAN4_SERIES10'){throw 'Unsupported arena protocol.'}
    if($arenaProtocol.partition -ne 'development' -or $arenaProtocol.id -notmatch '^[a-z0-9-]+$' -or $arenaProtocol.firstSeed -lt 0 -or $arenaProtocol.seeds -lt 1 -or $arenaProtocol.seeds -gt 1000){throw 'Invalid development arena interval.'}
}
if($CasesFile -and ($Collection -ne "learning" -or $protocol.version -notin @(7,11,12,15,16,18) -or -not (Test-Path -LiteralPath $CasesFile -PathType Leaf))){throw "Case selection requires a current Round7/Round11/Round12/protocol15/16/18 learning protocol and an existing selection file."}
if($ResearchCandidate){
    if($Collection -notin @('regression','learning')){throw 'Research overlays require the regression or learning collection.'}
    $researchCatalogFile=Join-Path $sourceRoot 'config/whist-research-candidates.json'
    $researchCatalog=Get-Content -LiteralPath $researchCatalogFile -Raw | ConvertFrom-Json
    if($researchCatalog.schemaVersion -ne 1 -or $researchCatalog.status -ne 'REJECTED_RESEARCH_ONLY'){throw 'Unsupported research catalog.'}
    $choices=@($researchCatalog.candidates | Where-Object id -eq $ResearchCandidate)
    if($choices.Count -ne 1){throw 'Unknown research candidate.'}
    $researchChoice=$choices[0]
    if($researchChoice.status -ne 'REJECTED_RESEARCH_ONLY' -or $Collection -notin $researchChoice.allowedCollections){throw 'Research collection is not declared by this candidate.'}
    if($Collection -eq 'learning' -and ($protocol.version -ne $researchChoice.protocolVersion -or ($Candidate -ne $researchChoice.candidatePrefix -and -not $Candidate.StartsWith($researchChoice.candidatePrefix+'-',[StringComparison]::Ordinal)))){throw 'Research learning requires its declared protocol version and candidate ID prefix.'}
    function Resolve-ContainedResearchPath([string]$Parent,[string]$Relative){
        if([IO.Path]::IsPathRooted($Relative) -or (($Relative -split '[\\/]') -contains '..')){throw 'Research path must be relative and cannot traverse parents.'}
        $parentAbsolute=[IO.Path]::GetFullPath($Parent).TrimEnd([IO.Path]::DirectorySeparatorChar)+[IO.Path]::DirectorySeparatorChar
        $absolute=[IO.Path]::GetFullPath((Join-Path $Parent $Relative))
        if(-not $absolute.StartsWith($parentAbsolute,[StringComparison]::OrdinalIgnoreCase)){throw 'Research path escaped its declared directory.'}
        return $absolute
    }
    $researchFiles=@($researchCatalog.sharedFiles)+@($researchChoice.files)
    $researchTargets=@{}
    foreach($entry in $researchFiles){
        if($entry.target -notmatch '^game-core/src/(main/java|test/java|test/resources)/' -or $entry.sha256 -notmatch '^[a-f0-9]{64}$' -or $researchTargets.ContainsKey($entry.target)){throw 'Invalid or duplicate research overlay target.'}
        $researchTargets[$entry.target]=$true
        $sourceFile=Resolve-ContainedResearchPath (Join-Path $sourceRoot 'whist-lab-app/ai-lab/experiments/bid-models') ($entry.source -replace '^whist-lab-app/ai-lab/experiments/bid-models/','')
        if(-not (Test-Path -LiteralPath $sourceFile -PathType Leaf) -or (Get-FileHash -LiteralPath $sourceFile -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256){throw "Research source digest differs: $($entry.source)"}
    }
    if(@($researchChoice.baselineGuards).Count -lt 1){throw 'Research candidate requires an accepted-baseline guard.'}
}
if($CandidateOverlay){
    Import-Module (Join-Path $sourceRoot 'whist-lab-app/ai-lab/tools/qualification/WhistCandidateSource.psm1') -Force
    if($Collection -notin @('regression','learning')){throw 'Candidate overlays require regression or learning.'}
    $candidateOverlayFile=[IO.Path]::GetFullPath($CandidateOverlay)
    $candidateSourceRoot=Split-Path -Parent $candidateOverlayFile
    $candidateOverlayBytes=[IO.File]::ReadAllBytes($candidateOverlayFile)
    $manifestStream=[IO.MemoryStream]::new($candidateOverlayBytes,$false)
    $manifestReader=[IO.StreamReader]::new($manifestStream,[Text.Encoding]::UTF8,$true)
    try{$candidateSpec=$manifestReader.ReadToEnd() | ConvertFrom-Json}finally{$manifestReader.Dispose()}
    $manifestHash=[Security.Cryptography.SHA256]::Create()
    try{$candidateOverlaySha=[Convert]::ToHexString($manifestHash.ComputeHash($candidateOverlayBytes)).ToLowerInvariant()}finally{$manifestHash.Dispose()}
    if($candidateSpec.schemaVersion -notin @(1,2) -or $candidateSpec.kind -ne 'CANDIDATE_EXPERIMENT' -or $candidateSpec.status -ne 'FROZEN_CANDIDATE_SOURCE' -or $candidateSpec.protocolVersion -ne 18 -or $candidateSpec.candidate -notmatch '^[a-z0-9-]+$' -or $candidateSpec.role -notin @('expert','player')){throw 'Invalid frozen schema18 candidate overlay.'}
    if($Candidate -and $Candidate -ne $candidateSpec.candidate){throw 'Candidate overlay identity differs from the requested candidate.'}
    $Candidate=$candidateSpec.candidate
    if($Collection -eq 'learning' -and ($protocol.version -ne 18 -or $declaredRoles.Count -ne 1 -or $declaredRoles[0] -ne $candidateSpec.role)){throw 'Candidate learning requires schema18 and its single declared role.'}
    function Resolve-ContainedCandidatePath([string]$Parent,[string]$Relative){
        if([IO.Path]::IsPathRooted($Relative) -or (($Relative -split '[\\/]') -contains '..')){throw 'Candidate paths must be relative without parent traversal.'}
        $parentAbsolute=[IO.Path]::GetFullPath($Parent).TrimEnd([IO.Path]::DirectorySeparatorChar)+[IO.Path]::DirectorySeparatorChar
        $absolute=[IO.Path]::GetFullPath((Join-Path $Parent $Relative))
        if(-not $absolute.StartsWith($parentAbsolute,[StringComparison]::OrdinalIgnoreCase)){throw 'Candidate path escaped its declared directory.'}
        return $absolute
    }
    $candidateBaseline = Assert-WhistCandidateAcceptedSource -AppRoot (Join-Path $sourceRoot 'whist-lab-app') -Specification $candidateSpec
    $candidateTargets=@{}
    if(@($candidateSpec.files).Count -lt 1){throw 'A candidate overlay needs explicit source files.'}
    foreach($entry in $candidateSpec.files){
        if($entry.target -notmatch '^(game-core/src/(main/java|test/java|test/resources)/|ai-lab/src/main/java/)' -or $entry.sha256 -notmatch '^[a-f0-9]{64}$' -or $candidateTargets.ContainsKey($entry.target)){throw 'Invalid or duplicate candidate overlay target.'}
        $candidateTargets[$entry.target]=$true
        $source=Resolve-ContainedCandidatePath $candidateSourceRoot $entry.source
        if(-not (Test-Path -LiteralPath $source -PathType Leaf) -or (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256){throw "Candidate source digest differs: $($entry.source)"}
    }
}
$stageRoot=Join-Path ([IO.Path]::GetTempPath()) ('ATF_J4_COLLECTION_'+[guid]::NewGuid().ToString('N'))
$stageApp=Join-Path $stageRoot 'whist-lab-app'
New-Item -ItemType Directory -Path $RunRoot,$stageApp | Out-Null
$inventory=@()
$sharedInputs = @('pom.xml', 'config/gradle-audit-collections.properties', 'config/test-collections.json', 'docs/game/README.md', 'docs/game/ATF_RulesRo.md', 'docs/game/ATF_RulesEn.md')
$sharedInputs += @(Get-ChildItem -LiteralPath (Join-Path $sourceRoot 'lib') -File |
    Where-Object Extension -In '.jar','.pom' | ForEach-Object { "lib/$($_.Name)" })
foreach ($relative in $sharedInputs) {
    $destination = Join-Path $stageRoot $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath (Join-Path $sourceRoot $relative) -Destination $destination
    $inventory += @{file="../$relative";sha256=(Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()}
}
if($Collection -eq 'cost'){[IO.File]::WriteAllBytes((Join-Path $RunRoot 'cost-profile.json'),$costSnapshot.bytes)}
if($Collection -in @("learning","arena")){[IO.File]::WriteAllBytes((Join-Path $RunRoot "protocol.json"),$protocolSnapshot.bytes)}
if($CasesFile){$stagedCasesFile=Join-Path $RunRoot "case-selection.json";Copy-Item -LiteralPath $CasesFile -Destination $stagedCasesFile}
$appSource=Join-Path $sourceRoot 'whist-lab-app'
foreach($file in Get-ChildItem -LiteralPath $appSource -Recurse -File){
    $relative=[IO.Path]::GetRelativePath($appSource,$file.FullName)
    if(($relative -split '[\\/]') | Where-Object {$_ -in @('.git','.gradle','build')}){continue}
    $target=Join-Path $stageApp $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Copy-Item -LiteralPath $file.FullName -Destination $target
}
if($ResearchCandidate){
    foreach($guard in $researchChoice.baselineGuards){
        $guardFile=Resolve-ContainedResearchPath $stageApp $guard.target
        if($guard.sha256 -notmatch '^[a-f0-9]{64}$' -or -not (Test-Path -LiteralPath $guardFile -PathType Leaf) -or (Get-FileHash -LiteralPath $guardFile -Algorithm SHA256).Hash.ToLowerInvariant() -ne $guard.sha256){throw "Accepted baseline changed; rebase and requalify research: $($guard.target)"}
    }
    foreach($entry in $researchFiles){
        $sourceFile=Resolve-ContainedResearchPath (Join-Path $sourceRoot 'whist-lab-app/ai-lab/experiments/bid-models') ($entry.source -replace '^whist-lab-app/ai-lab/experiments/bid-models/','')
        $target=Resolve-ContainedResearchPath $stageApp $entry.target
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
        Copy-Item -LiteralPath $sourceFile -Destination $target
        if((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256){throw "Staged research digest differs: $($entry.target)"}
    }
    Copy-Item -LiteralPath $researchCatalogFile -Destination (Join-Path $RunRoot 'research-catalog.json')
    @{researchCandidate=$ResearchCandidate;status=$researchChoice.status;scope='Explicit research overlay in temporary source only; no production activation or strength acceptance.';catalogSha256=(Get-FileHash -LiteralPath $researchCatalogFile -Algorithm SHA256).Hash.ToLowerInvariant();baselineGuards=$researchChoice.baselineGuards;files=$researchFiles} | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $RunRoot 'research-overlay.json') -Encoding utf8
}
if($CandidateOverlay){
    $stagedBaseline = Assert-WhistCandidateAcceptedSource -AppRoot $stageApp -Specification $candidateSpec
    foreach($entry in $candidateSpec.files){
        $source=Resolve-ContainedCandidatePath $candidateSourceRoot $entry.source
        $target=Resolve-ContainedCandidatePath $stageApp $entry.target
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
        Copy-Item -LiteralPath $source -Destination $target
        if((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256){throw "Staged candidate digest differs: $($entry.target)"}
    }
    $stagedCandidate = Assert-WhistStagedCandidateSource -AppRoot $stageApp -Specification $candidateSpec
    [IO.File]::WriteAllBytes((Join-Path $RunRoot 'candidate-overlay.json'),$candidateOverlayBytes)
    @{candidate=$Candidate;role=$candidateSpec.role;status='ISOLATED_CANDIDATE_SOURCE';scope='Explicit source overlay in TEMP only; no production activation or strength acceptance.';sourceManifest=$candidateOverlayFile;sourceRoot=$candidateSourceRoot;overlaySha256=$candidateOverlaySha;baselineGuards=$candidateSpec.baselineGuards;baselineInventory=$candidateBaseline;copiedBaselineInventory=$stagedBaseline;candidateInventory=$stagedCandidate;files=$candidateSpec.files} | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $RunRoot 'candidate-overlay-proof.json') -Encoding utf8
}
# Inventory the final staged bytes, including explicit overlays, before checking any source freeze.
$inventory+=@(foreach($file in Get-ChildItem -LiteralPath $stageApp -Recurse -File){
    $relative=[IO.Path]::GetRelativePath($stageApp,$file.FullName)
    @{file=$relative;sha256=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
})
if($Collection -eq 'learning' -and $Phase -eq 'heldout'){
    foreach($property in $freeze.sourceHashes.PSObject.Properties){
        if([IO.Path]::IsPathRooted($property.Name) -or ($property.Name -split '[\\/]') -contains '..' -or $property.Value -notmatch '^[a-f0-9]{64}$'){throw 'Invalid frozen source path or hash.'}
        $candidateFile=Join-Path $stageApp $property.Name
        if(-not (Test-Path -LiteralPath $candidateFile) -or (Get-FileHash -LiteralPath $candidateFile -Algorithm SHA256).Hash.ToLowerInvariant() -ne $property.Value){throw "Frozen source differs: $($property.Name)"}
    }
    [IO.File]::WriteAllBytes((Join-Path $RunRoot 'candidate-freeze.json'),$freezeSnapshot.bytes)
}
if($Collection -in @('learning','arena')){
    Import-Module (Join-Path $stageApp 'ai-lab/tools/qualification/WhistEvaluationInputs.psm1') -Force
    $sourceHashes=Get-WhistEvaluationSourceHashes -AppRoot $stageApp
    $request=@{candidate=$Candidate;phase=$Phase;firstSeed=$range.firstSeed;seeds=$range.seeds;sourceHashes=$sourceHashes;protocolSha256=(Get-FileHash -LiteralPath (Join-Path $RunRoot 'protocol.json') -Algorithm SHA256).Hash.ToLowerInvariant();run=$RunRoot;corpus=$CorpusRoot}
    if($Collection -eq 'arena'){$request.candidate='arena-'+$arenaProtocol.id;$request.phase='development';$request.firstSeed=$arenaProtocol.firstSeed;$request.seeds=$arenaProtocol.seeds}
    if($request.phase -eq 'heldout'){$request.freezeSha256=(Get-FileHash -LiteralPath (Join-Path $RunRoot 'candidate-freeze.json') -Algorithm SHA256).Hash.ToLowerInvariant()}
    $requestFile=Join-Path $RunRoot 'evaluation-request.json'
    $request | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $requestFile -Encoding utf8NoBOM
    & node (Join-Path $stageApp 'ai-lab/tools/qualification/whist-evaluation-registry.mjs') reserve $evaluationRegistryRoot $requestFile (Join-Path $RunRoot 'evaluation-reservation.json')
    if($LASTEXITCODE -ne 0){throw 'Canonical exposure reservation refused; no tournament was started.'}
    if($request.phase -eq 'heldout'){
        # Compatibility projection for frozen tournament readers; this is not the authority.
        $registryFolder=Join-Path $CorpusRoot 'holdout-use'
        New-Item -ItemType Directory -Force -Path $registryFolder | Out-Null
        $registry=Join-Path $registryFolder ("romanian4-$($range.firstSeed)-$([long]$range.firstSeed+[long]$range.seeds-1).json")
        if(Test-Path -LiteralPath $registry){
            $used=Get-Content -LiteralPath $registry -Raw | ConvertFrom-Json
            if($used.candidate -ne $Candidate){throw 'Local compatibility receipt belongs to another candidate.'}
            foreach($property in $freeze.sourceHashes.PSObject.Properties){if($used.sourceHashes.($property.Name) -ne $property.Value){throw 'Local receipt source changed.'}}
        }else{
            $receipt=Get-Content -LiteralPath (Join-Path $RunRoot 'evaluation-reservation.json') -Raw | ConvertFrom-Json
            $reservation=@{candidate=$Candidate;sourceHashes=$freeze.sourceHashes;reservationId=$receipt.reservationId;registryRoot=$evaluationRegistryRoot;reservedAt=$receipt.originallyReservedAt;run=$RunRoot} | ConvertTo-Json -Depth 8
            $stream=[IO.File]::Open($registry,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
            $writer=[IO.StreamWriter]::new($stream,[Text.UTF8Encoding]::new($false))
            try{$writer.Write($reservation)}finally{$writer.Dispose()}
        }
    }
}

$inventoryFile = Join-Path $RunRoot 'source-inventory.json'
$inventory | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $inventoryFile -Encoding utf8
$arguments=@('-p','algorithm-build','--offline','--no-daemon','--console=plain','--max-workers=1','-Dorg.gradle.jvmargs=-Xms32m -Xmx256m -XX:MaxMetaspaceSize=256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2',$selection[0].task)
if($Collection -eq 'learning'){if($protocol.version -eq 18){$arguments[-1]=':game-core:roundEighteenTest';$learningPrefix='whist.round18'}elseif($protocol.version -in @(15,16)){$arguments[-1]=':game-core:roundFifteenTest';$learningPrefix='whist.round15'}elseif($protocol.version -eq 12){$arguments[-1]=':game-core:roundTwelveTest';$learningPrefix='whist.round12'}elseif($protocol.version -eq 11){$arguments[-1]=':game-core:roundElevenTest';$learningPrefix='whist.round11'}else{$arguments[-1]=':game-core:roundSevenTest';$learningPrefix='whist.round7'}}
if ($Collection -eq 'documentation') {
    $fingerprint = (Get-FileHash -LiteralPath $inventoryFile -Algorithm SHA256).Hash.ToLowerInvariant()
    $arguments += @("-Pwhist.auditOutput=$RunRoot", "-Pwhist.auditSource=source-inventory SHA-256 $fingerprint")
}
if($Collection -eq 'cost'){$arguments+=@("-Pwhist.aiCost.profile=$RunRoot\cost-profile.json","-Pwhist.aiCost.output=$RunRoot")}
if($Collection -eq 'learning'){$arguments+=@("-P$learningPrefix.output=$RunRoot","-P$learningPrefix.corpus=$CorpusRoot","-P$learningPrefix.candidate=$Candidate","-P$learningPrefix.phase=$Phase","-P$learningPrefix.firstSeed=$($range.firstSeed)","-P$learningPrefix.seeds=$($range.seeds)")}
if($Collection -eq "learning" -and $declaredRoles.Count -eq 1){$arguments+=("-P$learningPrefix.role="+$declaredRoles[0])}
if($CasesFile){$arguments+=("-P$learningPrefix.casesFile="+$stagedCasesFile)}
if($Collection -eq 'learning' -and $null -ne $protocol.baselines){
    if($protocol.version -eq 7 -and ($protocol.baselines.expert -ne 'EXPERT_V6' -or $protocol.baselines.player -notin @('PLAYER_V6','PLAYER_V8'))){throw 'Unsupported frozen role baseline.'}
    $arguments+=("-P$learningPrefix.playerBaseline="+$protocol.baselines.player)
}
if($Collection -eq "learning" -and $null -eq $protocol.baselines){$arguments+=("-P$learningPrefix.playerBaseline=PLAYER_V6")}
if($Collection -eq 'arena'){$arguments+=@("-Pwhist.arena.protocol=$RunRoot\protocol.json","-Pwhist.arena.output=$RunRoot","-Pwhist.arena.corpus=$CorpusRoot","-Pwhist.componentEvidence=$RunRoot")}
if($CaptureComponents -or $Collection -in @('strategy','research')){$arguments+=("-Pwhist.componentEvidence=$RunRoot")}
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
    $scope = if ($Collection -eq 'documentation') { 'JVM documentation collection and canonical Extent validation.' }
        else { 'Native collection execution only; no inferred Extent, device or complete-game acceptance.' }
    @{collection=$Collection;startedAt=$began.ToString('o');endedAt=[DateTimeOffset]::UtcNow.ToString('o');executionMilliseconds=$watch.ElapsedMilliseconds;exitCode=$exitCode;status=$(if($exitCode -eq 0){'PASS'}else{'FAIL'});stage=$stageRoot;reportScope=$scope} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $RunRoot 'collection-result.json') -Encoding utf8
}
if($exitCode -ne 0){throw "Collection failed; preserved results: $RunRoot"}
Write-Output $RunRoot
