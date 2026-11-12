#requires -Version 7.0
param(
    [string]$RunRoot = '',
    [string]$SourceRoot = (Split-Path -Parent $PSScriptRoot),
    [string[]]$Groups = @(),
    [string]$ReuseFrom = '',
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenCache = (Join-Path $env:USERPROFILE '.m2/repository'),
    [string]$Maven = 'mvn',
    [string]$AndroidSdk = 'C:\tools\whist-android-sdk',
    [ValidateRange(2,2147483647)][int]$InputPartCharacters = 16384
)
$ErrorActionPreference = 'Stop'
$sourceRoot = [IO.Path]::GetFullPath($SourceRoot)
$catalog = Get-Content -LiteralPath (Join-Path $sourceRoot 'config/atf-test-suites.json') -Raw | ConvertFrom-Json
$primaryComponents = @($catalog.suites | Where-Object {$_.role -eq 'primary' -and $_.layer -eq 'component'})
$collectionProperties = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $sourceRoot 'config/gradle-app-collections.properties') -Raw)
$gradleGroups = @($collectionProperties.Values | Sort-Object -Unique | ForEach-Object {
    $suffix = [Globalization.CultureInfo]::InvariantCulture.TextInfo.ToTitleCase($_)
    $task = 'testDebug' + $suffix + 'Components'
    [pscustomobject]@{group="whist-lab-app-$_";project='whist-lab-app/app';task=$task;path=":app:$task";selection=$task}
})
$uiTask = ':atf-card-table-ui:testDebugUnitTest'
$gradleGroups += [pscustomobject]@{group='whist-lab-app-atf-card-table-ui';project='whist-lab-app/atf-card-table-ui';task='testDebugUnitTest';path=$uiTask;selection=$uiTask}
$knownGroups = @($primaryComponents.module) + @($gradleGroups.group)
if (@($Groups | Where-Object {$_ -cnotin $knownGroups}).Count -or
        @($Groups | Sort-Object -Unique).Count -ne $Groups.Count) {
    throw 'Groups trebuie să conțină numai identificatori distincți din catalogul componentelor.'
}
$selectedGroups = if ($Groups.Count) { @($Groups) } else { @($knownGroups) }
if (-not $RunRoot) {
    $archive = if ($env:WHIST_EVIDENCE_ROOT) { $env:WHIST_EVIDENCE_ROOT } else { 'C:\work\whist_back' }
    $RunRoot = Join-Path $archive ('component-suites/' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
}
$RunRoot = [IO.Path]::GetFullPath($RunRoot)
if ($RunRoot.Equals($sourceRoot,[StringComparison]::OrdinalIgnoreCase) -or
    $RunRoot.StartsWith($sourceRoot + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
    throw 'Rapoartele componentelor trebuie păstrate în afara proiectului.'
}
if (Test-Path -LiteralPath $RunRoot) { throw 'Directorul rulării trebuie să fie nou.' }

function Get-Inputs {
    return @(& (Join-Path $PSScriptRoot 'Get-AtfSourceInputs.ps1') -SourceRoot $sourceRoot)
}

function Invoke-MavenSelections($selections, $stage, $provenanceFile) {
    $java = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw 'Este necesar un JDK complet compatibil cu proiectul.' }
    Get-Command $Maven -ErrorAction Stop | Out-Null
    $repository = Join-Path $stage 'maven-repository'
    New-Item -ItemType Directory -Path $repository | Out-Null
    if (Test-Path -LiteralPath $MavenCache -PathType Container) {
        & robocopy $MavenCache $repository /E /R:1 /W:1 /XD atf .locks /XF *.lock *.tmp *.lastUpdated /NFL /NDL /NJH /NJS /NP *> (Join-Path $stage 'copy-cache.log')
        if ($LASTEXITCODE -gt 7) { throw 'Copierea cache-ului Maven a eșuat.' }
    }
    Push-Location $stage
    try {
        & $Maven -B -o -ntp "-Dmaven.repo.local=$repository" '-DskipTests' install *> (Join-Path $stage 'compile.log')
        if ($LASTEXITCODE -ne 0) { throw "Compilarea a eșuat: $stage/compile.log" }
        $classpathFile = Join-Path $stage 'component-classpath.txt'
        & $Maven -B -o -ntp "-Dmaven.repo.local=$repository" -pl atf-tests 'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath' '-DincludeScope=test' "-Dmdep.outputFile=$classpathFile" *> (Join-Path $stage 'classpath.log')
        if ($LASTEXITCODE -ne 0) { throw "Classpath-ul nu a fost rezolvat: $stage/classpath.log" }
        $policy = Get-Content -LiteralPath (Join-Path $stage 'config/atf-governance-policy.json') -Raw | ConvertFrom-Json
        $entries = @($policy.mavenModules | ForEach-Object { Join-Path $stage "$_/target/test-classes" })
        $entries += @($policy.mavenModules | ForEach-Object { Join-Path $stage "$_/target/classes" })
        $classpath = ($entries + @((Get-Content -LiteralPath $classpathFile -Raw).Trim())) -join [IO.Path]::PathSeparator
        foreach ($gate in $selections) {
            $destination = Join-Path $RunRoot $gate.module
            Push-Location (Join-Path $stage $gate.module)
            try {
                & $java '-Xmx768m' '-XX:ActiveProcessorCount=2' "-Datf.component.input-part-characters=$InputPartCharacters" -cp $classpath atf.tests.execution.components.ComponentSuiteExecution $gate.suite $destination $provenanceFile $sourceRoot *> (Join-Path $stage ($gate.module + '.log'))
                $exit = $LASTEXITCODE
            } finally { Pop-Location }
            $receipt = Join-Path $destination 'execution.json'
            [pscustomobject]@{module=$gate.module;suite=$gate.suite;exitCode=$exit;receipt=if(Test-Path -LiteralPath $receipt){Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json}else{$null}}
        }
    } finally { Pop-Location }
}

function Read-GradleSelection($item, $stage, $provenanceFingerprint) {
    $projectName = Split-Path -Leaf $item.project
    $generated = Join-Path $RunRoot ("whist-lab-app-$projectName-" + $item.task + '.html')
    $destination = Join-Path $RunRoot $item.group
    New-Item -ItemType Directory -Path $destination | Out-Null
    $report = Join-Path $destination 'component-report.html'
    if (Test-Path -LiteralPath $generated -PathType Leaf) {
        Move-Item -LiteralPath $generated -Destination $report
    }
    $xmlDirectory = Join-Path $stage (Join-Path $item.project ('build/test-results/' + $item.task))
    $xmlFiles = if (Test-Path -LiteralPath $xmlDirectory -PathType Container) {
        @(Get-ChildItem -LiteralPath $xmlDirectory -Filter 'TEST-*.xml' -File)
    } else { @() }
    $totals = [ordered]@{PASS=0;FAIL=0;ERROR=0;SKIP=0}
    foreach ($xmlFile in $xmlFiles) {
        [xml]$junit = Get-Content -LiteralPath $xmlFile.FullName -Raw
        $suites = @($junit.SelectNodes('/testsuite | /testsuites/testsuite'))
        if (-not $suites.Count) { throw "JUnit XML fără testsuite: $($xmlFile.Name)" }
        foreach ($suite in $suites) {
            $testCount = [int]$suite.GetAttribute('tests')
            $failCount = [int]$suite.GetAttribute('failures')
            $errorCount = [int]$suite.GetAttribute('errors')
            $skipCount = [int]$suite.GetAttribute('skipped')
            $totals.PASS += $testCount - $failCount - $errorCount - $skipCount
            $totals.FAIL += $failCount; $totals.ERROR += $errorCount; $totals.SKIP += $skipCount
        }
    }
    $testCount = $totals.PASS + $totals.FAIL + $totals.ERROR + $totals.SKIP
    $reportHash = if (Test-Path -LiteralPath $report -PathType Leaf) {
        (Get-FileHash -LiteralPath $report -Algorithm SHA256).Hash.ToLowerInvariant()
    } else { $null }
    $verification = Join-Path $stage (Join-Path $item.project ("build/component-verification/" + $item.task + ".ok"))
    $verified = (Test-Path -LiteralPath $verification -PathType Leaf) -and (Get-Content -LiteralPath $verification -Raw) -ceq 'PASS'
    $accepted = $verified -and $testCount -gt 0 -and $totals.PASS -eq $testCount -and [bool]$reportHash
    $coverageErrors = @()
    if (-not $verified) { $coverageErrors += 'Lipsește verificarea JUnit/Extent pentru task.' }
    $execution = [ordered]@{
        schemaVersion=1;kind='live-source-components';apkQualification=$false;selection=$item.selection
        tests=$testCount;outcomes=$totals;coverageErrors=$coverageErrors;containerFailures=@()
        provenanceSha256=$provenanceFingerprint;reportSha256=$reportHash
    }
    if ($reportHash) {
        $execution | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $destination 'execution.json') -Encoding utf8
    }
    [pscustomobject]@{
        module=$item.group;suite=$item.selection;exitCode=if($accepted){0}else{1}
        receipt=if($reportHash){$execution}else{$null}
    }
}

New-Item -ItemType Directory -Path $RunRoot | Out-Null
$before = Get-Inputs
$serialized = ConvertTo-Json -InputObject $before -Depth 5 -Compress
$fingerprint = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($serialized))).ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $RunRoot 'source-inputs.json'),$serialized,[Text.UTF8Encoding]::new($false))
$libraries = @($before | Where-Object {$_.path -like 'lib/*.jar'})
$provenance = [ordered]@{schemaVersion=1;kind='live-source-components';apkQualification=$false;androidExecution=$false;observedUtc=[DateTime]::UtcNow.ToString('o');sourceRoot=$sourceRoot;sourceFingerprint=$fingerprint;inputCount=$before.Count;inputManifest='source-inputs.json';inputPartCharacters=$InputPartCharacters;libraries=$libraries}
$provenanceReceipt = Join-Path $RunRoot 'source-provenance.json'
$provenance | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $provenanceReceipt -Encoding utf8
$provenanceFingerprint = (Get-FileHash -LiteralPath $provenanceReceipt -Algorithm SHA256).Hash.ToLowerInvariant()
$reused = @()
if ($ReuseFrom) {
    $reuseTool = Join-Path $PSScriptRoot '../atf-tests/tools/prepare-component-reuse.mjs'
    & node $reuseTool $RunRoot ([IO.Path]::GetFullPath($ReuseFrom)) @selectedGroups
    if ($LASTEXITCODE -ne 0) { throw 'Verificarea dovezilor anterioare a eșuat.' }
    $plan = Get-Content -LiteralPath (Join-Path $RunRoot 'component-reuse.json') -Raw | ConvertFrom-Json
    $reused = @($plan.decisions | Where-Object {$_.decision -in @('MATCH','REUSE')})
}
$groupsToRun = @($selectedGroups | Where-Object {$_ -cnotin @($reused.group)})
$mavenSelections = @($primaryComponents | Where-Object {$_.module -cin $groupsToRun})
$gradleSelections = @($gradleGroups | Where-Object {$_.group -cin $groupsToRun})

$stage = $null
$gradleBatch = $null
$results = @()
if ($groupsToRun.Count) {
    $stage = Join-Path ([IO.Path]::GetTempPath()) ('whist-component-suites-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $stage | Out-Null
    $provenanceFile = Join-Path $stage 'execution-provenance.json'
    Copy-Item -LiteralPath $provenanceReceipt -Destination $provenanceFile
    & (Join-Path $PSScriptRoot 'Copy-AtfSourceInputs.ps1') -SourceRoot $sourceRoot -Destination $stage -InputManifest (Join-Path $RunRoot 'source-inputs.json')

    $savedJavaHome = $env:JAVA_HOME
    $savedAndroidHome = $env:ANDROID_HOME
    $savedAndroidSdkRoot = $env:ANDROID_SDK_ROOT
    try {
        if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
        if ($mavenSelections.Count) {
            $results += @(Invoke-MavenSelections $mavenSelections $stage $provenanceFile)
        }
        if ($gradleSelections.Count) {
            $env:ANDROID_HOME = [IO.Path]::GetFullPath($AndroidSdk)
            $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
            if (-not (Test-Path -LiteralPath (Join-Path $AndroidSdk 'platforms/android-36/android.jar') -PathType Leaf)) {
                throw 'Este necesar Android SDK platform 36 pentru selecțiile Gradle ale aplicației.'
            }
            $gradle = Join-Path $stage 'whist-lab-app/gradlew.bat'
            $tasks = @($gradleSelections.path)
            $uiMode = if ($tasks.Count -eq 1 -and $tasks[0] -ceq $uiTask) { @("-Pcardui.only=component") } else { @() }
            $log = Join-Path $RunRoot 'gradle-components.log'
            Push-Location (Join-Path $stage 'whist-lab-app')
            try {
                $clock = [Diagnostics.Stopwatch]::StartNew()
                & $gradle --no-daemon --offline --console=plain --continue @uiMode @tasks "-Pwhist.auditOutput=$RunRoot" "-Pwhist.auditSource=$fingerprint" *> $log
                $exit = $LASTEXITCODE
                $clock.Stop()
            } finally { Pop-Location }
            $gradleBatch = [ordered]@{tasks=$tasks;exitCode=$exit;elapsedSeconds=[Math]::Round($clock.Elapsed.TotalSeconds,3);log='gradle-components.log'}
            $batchResults = @($gradleSelections | ForEach-Object { Read-GradleSelection $_ $stage $provenanceFingerprint })
            # A failure outside all selected tasks must not turn into a successful batch.
            if ($exit -ne 0 -and @($batchResults | Where-Object {$_.exitCode -ne 0}).Count -eq 0) {
                foreach ($result in $batchResults) { $result.exitCode = $exit }
            }
            $results += $batchResults
        }
    } finally {
        $env:JAVA_HOME = $savedJavaHome
        $env:ANDROID_HOME = $savedAndroidHome
        $env:ANDROID_SDK_ROOT = $savedAndroidSdkRoot
    }
}

$after = Get-Inputs
$stable = $serialized -ceq (ConvertTo-Json -InputObject $after -Depth 5 -Compress)
$passed = $stable -and ($results.Count + $reused.Count) -eq $selectedGroups.Count -and @($results | Where-Object {$_.exitCode -ne 0}).Count -eq 0
$androidComponentsReceipt = if ($results.Count) { 'source-component-run.json' } elseif ($reused.Count) {
    'snapshots/' + $reused[0].componentEvidenceOrigin + '/source-component-run.json'
}
$run = [ordered]@{
    schemaVersion=1;kind='live-source-components';apkQualification=$false
    sourceFingerprint=$fingerprint;provenanceReceiptSha256=$provenanceFingerprint;inputManifest='source-inputs.json'
    sourceStable=$stable;status=if($passed){'PASS'}else{'FAIL'};stage=$stage;inputCount=$before.Count
    results=$results;reusedGroups=@($reused | ForEach-Object {$_.group});gradleBatch=$gradleBatch
    androidComponentsReceipt=$androidComponentsReceipt
}
$run | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $RunRoot 'source-component-run.json') -Encoding utf8
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
if (-not $passed) { throw "Execuția, acoperirea HTML sau stabilitatea surselor a eșuat. Consultați $RunRoot/source-component-run.json și $stage." }
Write-Output "PASS: $RunRoot/README.md"
