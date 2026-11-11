#requires -Version 7.0
param(
    [string]$RunRoot = '',
    [string]$SourceRoot = (Split-Path -Parent $PSScriptRoot),
    [string[]]$Groups = @(),
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
$collections = @($collectionProperties.Values | Sort-Object -Unique)
$knownGroups = @($primaryComponents.module) + @($collections | ForEach-Object {"whist-lab-app-$_"}) + @('whist-lab-app-atf-card-table-ui')
if (@($Groups | Where-Object {$_ -cnotin $knownGroups}).Count -or
        @($Groups | Sort-Object -Unique).Count -ne $Groups.Count) {
    throw 'Groups trebuie să conțină numai identificatori distincți din catalogul componentelor.'
}
function Select-Group([string]$Name) {
    $Groups.Count -eq 0 -or $Name -cin $Groups
}
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
$java = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw 'Este necesar un JDK complet compatibil cu proiectul.' }
if (-not (Test-Path -LiteralPath (Join-Path $AndroidSdk 'platforms/android-36/android.jar') -PathType Leaf)) { throw 'Este necesar Android SDK platform 36 pentru selecțiile Gradle ale aplicației.' }
Get-Command $Maven -ErrorAction Stop | Out-Null
$stage = Join-Path ([IO.Path]::GetTempPath()) ('whist-component-suites-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage,$RunRoot | Out-Null
function Get-Inputs {
    return @(& (Join-Path $PSScriptRoot 'Get-AtfSourceInputs.ps1') -SourceRoot $sourceRoot)
}
$before = Get-Inputs
$serialized = ConvertTo-Json -InputObject $before -Depth 5 -Compress
$fingerprint = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($serialized))).ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $RunRoot 'source-inputs.json'),$serialized,[Text.UTF8Encoding]::new($false))
$libraries = @($before | Where-Object {$_.path -like 'lib/*.jar'})
$provenance = [ordered]@{schemaVersion=1;kind='live-source-components';apkQualification=$false;androidExecution=$false;observedUtc=[DateTime]::UtcNow.ToString('o');sourceRoot=$sourceRoot;sourceFingerprint=$fingerprint;inputCount=$before.Count;inputManifest='source-inputs.json';inputPartCharacters=$InputPartCharacters;libraries=$libraries}
$provenanceFile = Join-Path $stage 'execution-provenance.json'
$provenance | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $provenanceFile -Encoding utf8
$provenanceReceipt = Join-Path $RunRoot 'source-provenance.json'
Copy-Item -LiteralPath $provenanceFile -Destination $provenanceReceipt
$provenanceFingerprint = (Get-FileHash -LiteralPath $provenanceReceipt -Algorithm SHA256).Hash.ToLowerInvariant()
$policy = Get-Content -LiteralPath (Join-Path $sourceRoot 'config/atf-governance-policy.json') -Raw | ConvertFrom-Json
& (Join-Path $PSScriptRoot 'Copy-AtfSourceInputs.ps1') -SourceRoot $sourceRoot -Destination $stage -InputManifest (Join-Path $RunRoot 'source-inputs.json')
$repository = Join-Path $stage 'maven-repository'
New-Item -ItemType Directory -Path $repository | Out-Null
if (Test-Path -LiteralPath $MavenCache -PathType Container) {
    & robocopy $MavenCache $repository /E /R:1 /W:1 /XD atf .locks /XF *.lock *.tmp *.lastUpdated /NFL /NDL /NJH /NJS /NP *> (Join-Path $stage 'copy-cache.log')
    if ($LASTEXITCODE -gt 7) { throw 'Copierea cache-ului Maven a eșuat.' }
}
$savedJavaHome = $env:JAVA_HOME
$savedAndroidHome = $env:ANDROID_HOME
$savedAndroidSdkRoot = $env:ANDROID_SDK_ROOT
$results = [System.Collections.Generic.List[object]]::new()
try {
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    $env:ANDROID_HOME = [IO.Path]::GetFullPath($AndroidSdk)
    $env:ANDROID_SDK_ROOT = [IO.Path]::GetFullPath($AndroidSdk)
    Push-Location $stage
    try {
        & $Maven -B -o -ntp "-Dmaven.repo.local=$repository" '-DskipTests' install *> (Join-Path $stage 'compile.log')
        if ($LASTEXITCODE -ne 0) { throw "Compilarea a eșuat: $stage/compile.log" }
        $classpathFile = Join-Path $stage 'component-classpath.txt'
        & $Maven -B -o -ntp "-Dmaven.repo.local=$repository" -pl atf-tests 'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath' '-DincludeScope=test' "-Dmdep.outputFile=$classpathFile" *> (Join-Path $stage 'classpath.log')
        if ($LASTEXITCODE -ne 0) { throw "Classpath-ul nu a fost rezolvat: $stage/classpath.log" }
        $entries = @($policy.mavenModules | ForEach-Object { Join-Path $stage "$_/target/test-classes" })
        $entries += @($policy.mavenModules | ForEach-Object { Join-Path $stage "$_/target/classes" })
        $classpath = ($entries + @((Get-Content -LiteralPath $classpathFile -Raw).Trim())) -join [IO.Path]::PathSeparator
        foreach ($gate in $primaryComponents | Where-Object {Select-Group $_.module}) {
            $destination = Join-Path $RunRoot $gate.module
            Push-Location (Join-Path $stage $gate.module)
            try {
                & $java '-Xmx768m' '-XX:ActiveProcessorCount=2' "-Datf.component.input-part-characters=$InputPartCharacters" -cp $classpath atf.tests.execution.components.ComponentSuiteExecution $gate.suite $destination $provenanceFile $sourceRoot *> (Join-Path $stage ($gate.module + '.log'))
                $exit = $LASTEXITCODE
            } finally { Pop-Location }
            $receipt = Join-Path $destination 'execution.json'
            $results.Add([pscustomobject]@{module=$gate.module;suite=$gate.suite;exitCode=$exit;receipt=if(Test-Path -LiteralPath $receipt){Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json}else{$null}})
        }
        $gradle = Join-Path $stage 'whist-lab-app/gradlew.bat'
        $appSelections = @($collections | ForEach-Object {
            $suffix = [Globalization.CultureInfo]::InvariantCulture.TextInfo.ToTitleCase($_)
            [pscustomobject]@{ collection = $_; task = "testDebug${suffix}Components" }
        })
        function Invoke-GradleComponentSelection {
            param(
                [string]$Group,
                [string]$ProjectPath,
                [string]$TaskName,
                [string]$GradleTask,
                [string]$Selection
            )
            & $gradle --no-daemon --offline --console=plain $GradleTask `
                "-Pwhist.auditOutput=$RunRoot" "-Pwhist.auditSource=$fingerprint" `
                *> (Join-Path $stage ($Group + '.log'))
            $exit = $LASTEXITCODE
            $projectName = Split-Path -Leaf $ProjectPath
            $generated = Join-Path $RunRoot ("whist-lab-app-" + $projectName + "-" + $TaskName + '.html')
            $destination = Join-Path $RunRoot $Group
            New-Item -ItemType Directory -Path $destination -Force | Out-Null
            $report = Join-Path $destination 'component-report.html'
            if (Test-Path -LiteralPath $generated -PathType Leaf) {
                Move-Item -LiteralPath $generated -Destination $report -Force
            }
            $xmlDirectory = Join-Path $stage (Join-Path $ProjectPath "build/test-results/$TaskName")
            $xmlFiles = if (Test-Path -LiteralPath $xmlDirectory -PathType Container) {
                @(Get-ChildItem -LiteralPath $xmlDirectory -Filter '*.xml' -File)
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
            $accepted = $exit -eq 0 -and $testCount -gt 0 -and $totals.FAIL -eq 0 -and
                $totals.ERROR -eq 0 -and $totals.SKIP -eq 0 -and [bool]$reportHash
            $recordedExit = if ($accepted) { 0 } elseif ($exit -eq 0) { 1 } else { $exit }
            $execution = [ordered]@{
                schemaVersion=1;kind='live-source-components';apkQualification=$false;selection=$Selection
                tests=$testCount;outcomes=$totals;coverageErrors=@();containerFailures=@()
                provenanceSha256=$provenanceFingerprint;reportSha256=$reportHash
            }
            if ($reportHash) {
                $execution | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $destination 'execution.json') -Encoding utf8
            }
            $results.Add([pscustomobject]@{
                module=$Group;suite=$Selection;exitCode=$recordedExit
                receipt=if($reportHash){$execution}else{$null}
            })
        }
        Push-Location (Join-Path $stage 'whist-lab-app')
        try {
            foreach ($selection in $appSelections) {
                $group = 'whist-lab-app-' + $selection.collection
                if (!(Select-Group $group)) { continue }
                Invoke-GradleComponentSelection -Group $group -ProjectPath 'whist-lab-app/app' `
                    -TaskName $selection.task -GradleTask (':app:' + $selection.task) `
                    -Selection $selection.task
            }
            if (Select-Group 'whist-lab-app-atf-card-table-ui') {
                Invoke-GradleComponentSelection -Group 'whist-lab-app-atf-card-table-ui' `
                    -ProjectPath 'whist-lab-app/atf-card-table-ui' -TaskName 'testDebugUnitTest' `
                    -GradleTask ':atf-card-table-ui:testDebugUnitTest' `
                    -Selection ':atf-card-table-ui:testDebugUnitTest'
            }
        } finally { Pop-Location }
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $savedJavaHome
    $env:ANDROID_HOME = $savedAndroidHome
    $env:ANDROID_SDK_ROOT = $savedAndroidSdkRoot
}
$after = Get-Inputs
$stable = $serialized -ceq (ConvertTo-Json -InputObject $after -Depth 5 -Compress)
$passed = $stable -and $results.Count -gt 0 -and @($results | Where-Object {$_.exitCode -ne 0}).Count -eq 0
$run = [ordered]@{schemaVersion=1;kind='live-source-components';apkQualification=$false;sourceFingerprint=$fingerprint;provenanceReceiptSha256=$provenanceFingerprint;inputManifest='source-inputs.json';sourceStable=$stable;status=if($passed){'PASS'}else{'FAIL'};stage=$stage;inputCount=$before.Count;results=$results}
$run | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $RunRoot 'source-component-run.json') -Encoding utf8
$lines = @('# Rapoartele componentelor pe sursa fixată','','## Pe scurt','','Această execuție verifică suitele primare Maven și colecțiile Gradle declarate de catalogul aplicației. Nu califică un APK și nu folosește un dispozitiv; fiecare selecție are Extent și receipt proprii.','','| Selecție | Raport | Metadate |','|---|---|---|')
foreach ($result in $results) {
    $reportPath = $result.module + '/component-report.html'
    $receiptPath = $result.module + '/execution.json'
    $lines += '| ' + $result.module + ' | [Extent](' + $reportPath + ') | [Execuție](' + $receiptPath + ') |'
}
$lines += @('','`source-inputs.json`, `source-provenance.json` și `source-component-run.json` leagă toate selecțiile de aceeași amprentă și de receipt-ul nativ de proveniență. XML-ul JUnit rămâne temporar pentru confruntare; nu se păstrează ca raport duplicat în dosarul final. E2E rămâne separat și neexecutat.')
$lines | Set-Content -LiteralPath (Join-Path $RunRoot 'README.md') -Encoding utf8
if (-not $passed) { throw "Execuția, acoperirea HTML sau stabilitatea surselor a eșuat. Consultați $RunRoot/source-component-run.json și $stage." }
Write-Output "PASS: $RunRoot/README.md"
