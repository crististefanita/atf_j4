#requires -Version 7.0
param(
    [string]$RunRoot = '',
    [string]$SourceRoot = (Split-Path -Parent $PSScriptRoot),
    [string[]]$Groups = @(),
    [string]$ReuseFrom = '',
    [string]$ResumeFrom = '',
    [string]$PinnedSourceInputs = '',
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenCache = (Join-Path $env:USERPROFILE '.m2/repository'),
    [string]$Maven = 'mvn',
    [string]$AndroidSdk = $(if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { $env:ANDROID_HOME }),
    [ValidateRange(2,2147483647)][int]$InputPartCharacters = 16384,
    [string]$TestChangeControl = '',
    [switch]$AllTestChangeControls,
    [string]$TestChangeBaseRef = $env:ATF_TEST_CHANGE_BASE_REF
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'AtfLocalPaths.ps1')
. (Join-Path $PSScriptRoot 'AtfComponentJvm.ps1')
Assert-AtfComponentJavaEnvironment
. (Join-Path $PSScriptRoot 'AtfQualificationSnapshot.ps1')
. (Join-Path $PSScriptRoot 'AtfQualificationTiming.ps1')
. (Join-Path $PSScriptRoot 'AtfTestChangeBatch.ps1')
. (Join-Path $PSScriptRoot 'AtfComponentResume.ps1')
$sourceRoot = [IO.Path]::GetFullPath($SourceRoot)
if ($ResumeFrom) {
    if ($ReuseFrom -or $TestChangeControl -or $AllTestChangeControls) {
        throw 'ResumeFrom cere o rulare normală de componentă și propria sursă validată de dovezi.'
    }
    $ResumeFrom = (Resolve-Path -LiteralPath $ResumeFrom).ProviderPath
    $ReuseFrom = $ResumeFrom
}
$proofCommit = ''
if ($TestChangeControl -or $AllTestChangeControls) {
    if (($TestChangeControl -and ($AllTestChangeControls -or $TestChangeControl -cnotmatch '^[A-Z][A-Z0-9-]{3,79}$')) -or
            !$TestChangeBaseRef -or $ReuseFrom -or
            @($Groups | Where-Object { $_ -cne 'atf-tests' }).Count) {
        throw 'Proba testChanges cere fie un ID, fie toate controalele, baza Git și selecția offline atf-tests fără reutilizare.'
    }
    $Groups = @('atf-tests')
    $proofCommit = & git --no-replace-objects -C $sourceRoot rev-parse --verify HEAD
    if ($LASTEXITCODE -ne 0 -or $proofCommit -cnotmatch '^[a-f0-9]{40}(?:[a-f0-9]{24})?$') {
        throw 'Proba testChanges cere un commit Git local.'
    }
    $dirty = & git -C $sourceRoot status --porcelain=v1 --untracked-files=all
    if ($LASTEXITCODE -ne 0 -or $dirty) { throw 'Proba testChanges cere un checkout curat.' }
}
if (-not $RunRoot) {
    $archive = Get-AtfEvidenceRoot
    $RunRoot = Join-Path $archive ('component-suites/' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
}
$RunRoot = [IO.Path]::GetFullPath($RunRoot)
if ($RunRoot.Equals($sourceRoot,[StringComparison]::OrdinalIgnoreCase) -or
    $RunRoot.StartsWith($sourceRoot + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
    throw 'Rapoartele componentelor trebuie păstrate în afara proiectului.'
}
if (Test-Path -LiteralPath $RunRoot) { throw 'Directorul rulării trebuie să fie nou.' }

function Invoke-MavenSelections($selections, $stage, $provenanceFile) {
    $java = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw 'Este necesar un JDK complet compatibil cu proiectul.' }
    $command = Get-Command $Maven -ErrorAction Stop
    $workspace = Split-Path -Parent $stage
    $policy = Get-Content -LiteralPath (Join-Path $stage 'config/atf-governance-policy.json') -Raw | ConvertFrom-Json
    $identity = Get-AtfInputFingerprint (@($fingerprint,$releaseFingerprint,$command.Source,
        (Get-FileHash -LiteralPath $java).Hash) -join [char]10)
    $prepared = Read-AtfPreparedComponentMaven $workspace $identity $policy.mavenModules
    Push-Location $stage
    try {
        Assert-AtfQualificationSnapshot $snapshot $stage $sourceRoot
        $classpathFile = Join-Path $workspace 'component-classpath.txt'
        if ($prepared) {
            $repository = $prepared.repository
            $mavenReceipt = $prepared.receipt
            foreach ($name in 'maven-cache','maven-compile') {
                Write-AtfQualificationDecision $timing "components.$name" 'REUSED' 'Verified Maven preparation and classpath.'
            }
        } else {
            $repository = Join-Path $workspace 'maven-repository'
            $retry = Test-Path -LiteralPath $repository
            if ($retry) { $repository += '-' + [guid]::NewGuid().ToString('N').Substring(0,8) }
            Invoke-AtfQualificationPhase $timing 'components.maven-cache' {
                New-Item -ItemType Directory -Path $repository | Out-Null
                if (Test-Path -LiteralPath $MavenCache -PathType Container) {
                    & robocopy $MavenCache $repository /E /XJ /R:1 /W:1 /XD atf .locks /XF *.lock *.tmp *.lastUpdated /NFL /NDL /NJH /NJS /NP *> (Join-Path $RunRoot 'copy-cache.log')
                    if ($LASTEXITCODE -gt 7) { throw 'Copierea cache-ului Maven a eșuat.' }
                }
            }
            $compileExit = Invoke-AtfQualificationPhase $timing 'components.maven-compile' {
                [string[]]$goals = @(if ($retry) { @('clean','install') } else { 'install' })
                & $Maven -B -o -ntp "-Dmaven.repo.local=$repository" '-DskipTests' @goals *> (Join-Path $RunRoot 'compile.log')
                $LASTEXITCODE
            } -ExitCode
            if ($compileExit -ne 0) { throw "Compilarea a eșuat: $RunRoot/compile.log" }
            Assert-AtfQualificationSnapshot $snapshot $stage $sourceRoot
            $mavenReceipt = [ordered]@{
                schemaVersion=1;identity=$identity;repo=(Split-Path -Leaf $repository)
                buildFingerprint=(Get-AtfComponentMavenFingerprint $workspace $repository $policy.mavenModules)
                classpathSha256=$null
            }
            Write-AtfComponentJson (Join-Path $workspace 'component-maven.json') $mavenReceipt
        }
        if ($prepared -and $prepared.classpathReady) {
            Write-AtfQualificationDecision $timing 'components.maven-classpath' 'REUSED' 'Verified classpath.'
        } else {
            if (Test-Path -LiteralPath $classpathFile) { Remove-Item -LiteralPath $classpathFile }
            $classpathExit = Invoke-AtfQualificationPhase $timing 'components.maven-classpath' {
                & $Maven -B -o -ntp "-Dmaven.repo.local=$repository" -pl atf-tests 'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath' '-DincludeScope=test' "-Dmdep.outputFile=$classpathFile" *> (Join-Path $RunRoot 'classpath.log')
                $LASTEXITCODE
            } -ExitCode
            if ($classpathExit -ne 0) { throw "Classpath-ul nu a fost rezolvat: $RunRoot/classpath.log" }
            $mavenReceipt.classpathSha256 = (Get-FileHash -LiteralPath $classpathFile).Hash
            Write-AtfComponentJson (Join-Path $workspace 'component-maven.json') $mavenReceipt
        }
        $entries = @($policy.mavenModules | ForEach-Object { Join-Path $stage "$_/target/test-classes" })
        $entries += @($policy.mavenModules | ForEach-Object { Join-Path $stage "$_/target/classes" })
        $classpath = ($entries + @((Get-Content -LiteralPath $classpathFile -Raw).Trim())) -join [IO.Path]::PathSeparator
        $suiteJvm = @(Get-AtfComponentJvmArguments -InputPartCharacters $InputPartCharacters -SourceRoot $sourceRoot)
        if ($AllTestChangeControls) {
            $proofRunner = 'atf.tests.execution.components.TestChangeProofExecution'
            $proofJvm = $suiteJvm + @('-cp', $classpath, $proofRunner)
            Invoke-AtfTestChangeBatch -RunRoot $RunRoot -ReadPlan {
                & $java @proofJvm --list $sourceRoot $TestChangeBaseRef $proofCommit 2>> (Join-Path $RunRoot 'test-change-plan.log')
                if ($LASTEXITCODE -ne 0) { throw 'Selecția testChanges a eșuat. Consultați test-change-plan.log.' }
            } -RunControl {
                param($id, $output)
                Invoke-AtfQualificationPhase $timing "components.test-change.$id" {
                    & $java @proofJvm $sourceRoot $stage $output $provenanceFile $TestChangeBaseRef $id $proofCommit *> (Join-Path $RunRoot "test-change-$id.log")
                    $LASTEXITCODE
                } -ExitCode
            } -CheckSnapshot {
                Invoke-AtfQualificationPhase $timing 'components.snapshot-recheck' {
                    Assert-AtfQualificationSnapshot $snapshot $stage $sourceRoot
                }
            }
            return
        }
        foreach ($gate in $selections) {
            $selection = if ($TestChangeControl) { $TestChangeControl } else { $gate.module }
            $destination = Join-Path $RunRoot $selection
            $arguments = if ($TestChangeControl) {
                @('atf.tests.execution.components.TestChangeProofExecution', $sourceRoot, $stage, $destination,
                    $provenanceFile, $TestChangeBaseRef, $TestChangeControl, $proofCommit)
            } else {
                @('atf.tests.execution.components.ComponentSuiteExecution', $gate.suite, $destination, $provenanceFile, $stage)
            }
            Push-Location (Join-Path $stage $gate.module)
            try {
                $exit = Invoke-AtfQualificationPhase $timing ("components.run." + $gate.module) {
                    & $java @suiteJvm -cp $classpath @arguments *> (Join-Path $RunRoot ($gate.module + '.log'))
                    $LASTEXITCODE
                } -ExitCode
            } finally { Pop-Location }
            $receipt = Join-Path $destination 'execution.json'
            $result = [pscustomobject]@{module=$gate.module;suite=$gate.suite;exitCode=$exit;receipt=if(Test-Path -LiteralPath $receipt){Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json}else{$null}}
            if (!$TestChangeControl) { Save-AtfComponentCheckpoint @($result) }
            $result
        }
    } finally { Pop-Location }
}

function Read-AtfGradleResults($Selections, [string]$Journal) {
    $checkpoint = Get-Content -LiteralPath $Journal -Raw | ConvertFrom-Json
    foreach ($item in $Selections) {
        $row = $checkpoint.results | Where-Object module -ceq $item.group
        $report = Join-Path $RunRoot "$($item.group)/component-report.html"
        if ($row -and (Test-Path -LiteralPath $report -PathType Leaf) -and
            (Get-FileHash -LiteralPath $report).Hash.ToLowerInvariant() -ceq $row.receipt.reportSha256) {
            $row
        } else {
            [pscustomobject]@{module=$item.group;suite=$item.selection;exitCode=1;receipt=$null}
        }
    }
}

$tempRoot = Get-AtfQualificationTempRoot $sourceRoot
$work = if ($ResumeFrom) { Get-AtfResumeComponentWorkspace $ResumeFrom $tempRoot } else {
    (New-Item -ItemType Directory -Path (Join-Path $tempRoot ('whist-component-suites-' + [guid]::NewGuid().ToString('N')))).FullName
}
$workspaceLock = Open-AtfQualificationWorkspace $work
try {
    New-Item -ItemType Directory -Path $RunRoot | Out-Null
    $timing = New-AtfQualificationTiming 'components' (Join-Path $RunRoot 'timing.json')
    $stage = Join-Path $work 'project'
    if ($ResumeFrom) {
        $snapshot = Invoke-AtfQualificationPhase $timing 'components.resume-validation' {
            Restore-AtfComponentSnapshot $ResumeFrom $work $sourceRoot $PinnedSourceInputs
        }
        [IO.File]::WriteAllText((Join-Path $RunRoot 'source-inputs.json'),$snapshot.source)
        [IO.File]::WriteAllText((Join-Path $RunRoot 'release-inputs.json'),$snapshot.release)
        Write-AtfQualificationDecision $timing 'components.snapshot-preflight' 'REUSED' 'Verified live and staged snapshot.'
    } else {
        New-Item -ItemType Directory -Path $stage | Out-Null
        $snapshot = Invoke-AtfQualificationPhase $timing 'components.snapshot-preflight' {
            New-AtfQualificationSnapshot $sourceRoot $stage $RunRoot $PinnedSourceInputs
        }
    }
    $catalog = Get-Content -LiteralPath (Join-Path $stage 'config/atf-test-suites.json') -Raw | ConvertFrom-Json
    $primaryComponents = @($catalog.suites | Where-Object {$_.role -eq 'primary' -and $_.layer -eq 'component'})
    $collectionProperties = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $stage 'config/gradle-app-collections.properties') -Raw)
    $gradleGroups = @($collectionProperties.Values | Sort-Object -Unique | ForEach-Object {
        $suffix = [Globalization.CultureInfo]::InvariantCulture.TextInfo.ToTitleCase($_)
        $task = 'testDebug' + $suffix + 'Components'
        [pscustomobject]@{group="whist-lab-app-$_";project='whist-lab-app/app';task=$task;path=":app:$task";selection=$task}
    })
    $uiTask = ':atf-card-table-ui:testDebugUnitTest'
    $gradleGroups += [pscustomobject]@{group='whist-lab-app-atf-card-table-ui';project='whist-lab-app/atf-card-table-ui';task='testDebugUnitTest';path=$uiTask;selection=$uiTask}
    $rulesTask = ':game-core:releaseRulesComponents'
    $gradleGroups += [pscustomobject]@{group='whist-lab-app-rules';project='whist-lab-app/game-core';task='releaseRulesComponents';path=$rulesTask;selection=$rulesTask}
    $knownGroups = @($primaryComponents.module) + @($gradleGroups.group)
    if (@($Groups | Where-Object {$_ -cnotin $knownGroups}).Count -or
            @($Groups | Sort-Object -Unique).Count -ne $Groups.Count) {
        throw 'Groups trebuie să conțină numai identificatori distincți din catalogul componentelor.'
    }
    $selectedGroups = @(if ($Groups.Count) { $Groups } else { $knownGroups })

    $fingerprint = Get-AtfInputFingerprint $snapshot.source
    $releaseFingerprint = Get-AtfInputFingerprint $snapshot.release
    $libraries = @($snapshot.source | ConvertFrom-Json | Where-Object {$_.path -like 'lib/*.jar'})
    $provenance = [ordered]@{schemaVersion=1;kind='live-source-components';apkQualification=$false;androidExecution=$false;observedUtc=[DateTime]::UtcNow.ToString('o');sourceFingerprint=$fingerprint;inputCount=$snapshot.inputCount;inputManifest='source-inputs.json';inputPartCharacters=$InputPartCharacters;libraries=$libraries
        releaseFingerprint=$releaseFingerprint;releaseInputManifest='release-inputs.json'}
    $provenanceReceipt = Join-Path $RunRoot 'source-provenance.json'
    $provenance | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $provenanceReceipt -Encoding utf8
    $provenanceFingerprint = (Get-FileHash -LiteralPath $provenanceReceipt -Algorithm SHA256).Hash.ToLowerInvariant()
    Write-AtfComponentJson (Join-Path $RunRoot 'component-workspace.json') ([ordered]@{
        schemaVersion=1;path=$work;sourceFingerprint=$fingerprint;releaseFingerprint=$releaseFingerprint
    })
    $completedComponents = [Collections.Generic.List[object]]::new()
    $checkpointFile = Join-Path $RunRoot 'component-checkpoint.json'
    function Save-AtfComponentCheckpoint([object[]]$Rows) {
        foreach ($row in $Rows) { $completedComponents.Add($row) }

        # Accepted only by ResumeFrom after revalidating both inventories and every reused report.
        $checkpoint = [ordered]@{
            schemaVersion=1;kind='live-source-components';apkQualification=$false
            sourceFingerprint=$fingerprint;provenanceReceiptSha256=$provenanceFingerprint;inputManifest='source-inputs.json'
            sourceStable=$true;inputCount=$snapshot.inputCount;releaseFingerprint=$releaseFingerprint;releaseInputManifest='release-inputs.json'
            status=if (@($completedComponents | Where-Object exitCode -ne 0).Count) {'FAIL'} else {'PASS'}
            results=$completedComponents.ToArray()
        }
        Write-AtfComponentJson $checkpointFile $checkpoint
    }
    $reused = @()
    if ($ReuseFrom) {
        $reuseTool = Join-Path $stage 'atf-tests/tools/prepare-component-reuse.mjs'
        $reuseExit = Invoke-AtfQualificationPhase $timing 'components.reuse-check' {
            $recovery = @(if ($ResumeFrom) { '--resume' })
            & node $reuseTool $RunRoot ([IO.Path]::GetFullPath($ReuseFrom)) @recovery @('--source-root',$stage) @selectedGroups | Out-Host
            $LASTEXITCODE
        } -ExitCode
        if ($reuseExit -ne 0) { throw 'Verificarea dovezilor anterioare a eșuat.' }
        $plan = Get-Content -LiteralPath (Join-Path $RunRoot 'component-reuse.json') -Raw | ConvertFrom-Json
        $reused = @($plan.decisions | Where-Object {$_.decision -in @('MATCH','REUSE')})
    }
    foreach ($group in $reused) {
        Write-AtfQualificationDecision $timing ("components.run." + $group.group) 'REUSED' $group.decision
    }
    $groupsToRun = @($selectedGroups | Where-Object {$_ -cnotin @($reused.group)})
    $mavenSelections = @($primaryComponents | Where-Object {$_.module -cin $groupsToRun})
    $gradleSelections = @($gradleGroups | Where-Object {$_.group -cin $groupsToRun})

    $gradleBatch = $null
    $results = @()
    if ($groupsToRun.Count) {
        $provenanceFile = Join-Path $work 'execution-provenance.json'
        Copy-Item -LiteralPath $provenanceReceipt -Destination $provenanceFile

        $savedTiming = $env:ATF_RELEASE_STAGE_TIMING_ROOT
        $savedJavaHome = $env:JAVA_HOME
        $savedAndroidHome = $env:ANDROID_HOME
        $savedAndroidSdkRoot = $env:ANDROID_SDK_ROOT
        try {
            $env:ATF_RELEASE_STAGE_TIMING_ROOT = $RunRoot
            if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
            if ($mavenSelections.Count) {
                if ($AllTestChangeControls) {
                    $proofRoot = Invoke-MavenSelections $mavenSelections $stage $provenanceFile
                    Write-Output "PASS: $proofRoot"
                    return
                }
                $results += @(Invoke-MavenSelections $mavenSelections $stage $provenanceFile)
            }
            if ($gradleSelections.Count) {
                $AndroidSdk = Get-AtfAndroidSdkRoot $AndroidSdk
                $env:ANDROID_HOME = [IO.Path]::GetFullPath($AndroidSdk)
                $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
                if (-not (Test-Path -LiteralPath (Join-Path $AndroidSdk 'platforms/android-36/android.jar') -PathType Leaf)) {
                    throw 'Este necesar Android SDK platform 36 pentru selecțiile Gradle ale aplicației.'
                }
                $gradle = Join-Path $stage 'whist-lab-app/gradlew.bat'
                $tasks = @($gradleSelections.path)
                $checkpointPlan = Join-Path $RunRoot 'gradle-checkpoint-plan.json'
                $taskBindings = [ordered]@{}
                foreach ($item in $gradleSelections) {
                    $taskBindings[$item.path] = @{group=$item.group;selection=$item.selection}
                }
                Write-AtfComponentJson $checkpointPlan @{tasks=$taskBindings}
                Save-AtfComponentCheckpoint @()
                $arguments = @('--no-daemon', '--offline', '--console=plain', '--continue', '--no-parallel') + $tasks +
                    @("-Pwhist.auditOutput=$RunRoot", "-Pwhist.auditSource=$fingerprint", "-Pwhist.auditCheckpoint=$checkpointPlan")
                if ($tasks.Count -eq 1 -and $tasks[0] -ceq $uiTask) { $arguments += '-Pcardui.only=component' }
                $log = Join-Path $RunRoot 'gradle-components.log'
                Push-Location (Join-Path $stage 'whist-lab-app')
                try {
                    $clock = [Diagnostics.Stopwatch]::StartNew()
                    $exit = Invoke-AtfQualificationPhase $timing 'components.gradle-batch' {
                        & $gradle @arguments *> $log
                        $LASTEXITCODE
                    } -ExitCode
                    $clock.Stop()
                } finally { Pop-Location }
                $gradleBatch = [ordered]@{tasks=$tasks;exitCode=$exit;elapsedSeconds=[Math]::Round($clock.Elapsed.TotalSeconds,3);log='gradle-components.log'}
                $batchResults = @(Invoke-AtfQualificationPhase $timing 'components.gradle-extent-evidence' {
                    Read-AtfGradleResults $gradleSelections $checkpointFile
                })
                if ($exit -ne 0 -and @($batchResults | Where-Object exitCode -ne 0).Count -eq 0) {
                    throw "Gradle a eșuat în afara selecțiilor validate. Reluați din checkpoint și consultați $log."
                }
                $results += $batchResults
            }
        } finally {
            $env:ATF_RELEASE_STAGE_TIMING_ROOT = $savedTiming
            $env:JAVA_HOME = $savedJavaHome
            $env:ANDROID_HOME = $savedAndroidHome
            $env:ANDROID_SDK_ROOT = $savedAndroidSdkRoot
        }
    }

    $snapshotDifferences = @(Invoke-AtfQualificationPhase $timing 'components.snapshot-recheck' {
        Get-AtfSnapshotDifferences $snapshot $stage $sourceRoot | ForEach-Object {
            ConvertTo-AtfPortableDiagnostic $_ $sourceRoot $stage $work $RunRoot
        }
    })
    $stable = $snapshotDifferences.Count -eq 0
    $passed = $stable -and ($results.Count + $reused.Count) -eq $selectedGroups.Count -and @($results | Where-Object {$_.exitCode -ne 0}).Count -eq 0
    if ($TestChangeControl) {
        $directory = Join-Path $RunRoot $TestChangeControl
        if (!$passed) { throw "Proba $TestChangeControl a eșuat; consultați $RunRoot/atf-tests.log și timing.json." }
        Move-Item -LiteralPath (Join-Path $directory 'proof.pending.json') -Destination (Join-Path $directory 'proof.json')
        Write-Output "PASS: $directory/proof.json"
        return
    }
    $androidComponentsReceipt = if ($results.Count) { 'source-component-run.json' } elseif ($reused.Count) {
        'snapshots/' + $reused[0].componentEvidenceOrigin + '/source-component-run.json'
    }
    $run = [ordered]@{
        schemaVersion=1;kind='live-source-components';apkQualification=$false
        sourceFingerprint=$fingerprint;provenanceReceiptSha256=$provenanceFingerprint;inputManifest='source-inputs.json'
        sourceStable=$stable;status=if($passed){'PASS'}else{'FAIL'};stage='isolated-staging-copy';inputCount=$snapshot.inputCount
        releaseFingerprint=$releaseFingerprint;releaseInputManifest='release-inputs.json';snapshotDifferences=$snapshotDifferences
        results=$results;reusedGroups=@($reused | ForEach-Object {$_.group});gradleBatch=$gradleBatch
        androidComponentsReceipt=$androidComponentsReceipt
    }
    Write-AtfComponentJson (Join-Path $RunRoot 'source-component-run.json') $run
    $lines = @('# Rapoartele componentelor pe sursa fixată','','## Pe scurt','','Selecțiile noi au raport Extent și receipt proprii. Rularea privește sursa fixată; calificarea APK-ului și testele native au intrări separate.','','| Selecție | Execuție | Raport | Metadate |','|---|---|---|---|')
    foreach ($group in $selectedGroups) {
        $origin = $reused | Where-Object {$_.group -ceq $group}
        $mode = if ($origin) { $origin.decision } else { 'RUN' }
        $reportPath = $group + '/component-report.html'
        $receiptPath = $group + '/execution.json'
        $lines += "| $group | $mode | [Extent]($reportPath) | [Execuție]($receiptPath) |"
    }
    $lines += @('','Manifestul surselor, proveniența și receipt-ul agregat leagă execuțiile noi de amprenta surselor. Planul de reutilizare indică decizia fiecărei suite și dosarul original. XML-ul JUnit rămâne temporar pentru verificare.')
    $lines | Set-Content -LiteralPath (Join-Path $RunRoot 'README.md') -Encoding utf8
    foreach ($logFile in Get-ChildItem -LiteralPath $RunRoot -File -Filter '*.log') {
        $content = [IO.File]::ReadAllText($logFile.FullName)
        $portable = ConvertTo-AtfPortableDiagnostic $content $sourceRoot $stage $work $RunRoot
        [IO.File]::WriteAllText($logFile.FullName, $portable, [Text.UTF8Encoding]::new($false))
    }
    if (-not $passed) { throw "Execuția, acoperirea HTML sau stabilitatea surselor a eșuat. Consultați $RunRoot/source-component-run.json și $stage." }
    Write-Output "PASS: $RunRoot/README.md"
} finally { $workspaceLock.Dispose() }
