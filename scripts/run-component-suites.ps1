#requires -Version 7.0
param(
    [string]$RunRoot = '',
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenCache = (Join-Path $env:USERPROFILE '.m2/repository'),
    [string]$Maven = 'mvn',
    [ValidateRange(2,2147483647)][int]$InputPartCharacters = 16384
)
$ErrorActionPreference = 'Stop'
$sourceRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
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
$policy = Get-Content -LiteralPath (Join-Path $sourceRoot 'config/atf-governance-policy.json') -Raw | ConvertFrom-Json
& (Join-Path $PSScriptRoot 'Copy-AtfSourceInputs.ps1') -SourceRoot $sourceRoot -Destination $stage -InputManifest (Join-Path $RunRoot 'source-inputs.json')
$repository = Join-Path $stage 'maven-repository'
New-Item -ItemType Directory -Path $repository | Out-Null
if (Test-Path -LiteralPath $MavenCache -PathType Container) {
    & robocopy $MavenCache $repository /E /R:1 /W:1 /XD atf .locks /XF *.lock *.tmp *.lastUpdated /NFL /NDL /NJH /NJS /NP *> (Join-Path $stage 'copy-cache.log')
    if ($LASTEXITCODE -gt 7) { throw 'Copierea cache-ului Maven a eșuat.' }
}
$savedJavaHome = $env:JAVA_HOME
$results = [System.Collections.Generic.List[object]]::new()
try {
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
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
        $catalog = Get-Content -LiteralPath (Join-Path $stage 'config/atf-test-suites.json') -Raw | ConvertFrom-Json
        foreach ($gate in $catalog.suites | Where-Object {$_.role -eq 'primary' -and $_.layer -eq 'component'}) {
            $destination = Join-Path $RunRoot $gate.module
            Push-Location (Join-Path $stage $gate.module)
            try {
                & $java '-Xmx768m' '-XX:ActiveProcessorCount=2' "-Datf.component.input-part-characters=$InputPartCharacters" -cp $classpath atf.tests.execution.components.ComponentSuiteExecution $gate.suite $destination $provenanceFile $sourceRoot *> (Join-Path $stage ($gate.module + '.log'))
                $exit = $LASTEXITCODE
            } finally { Pop-Location }
            $receipt = Join-Path $destination 'execution.json'
            $results.Add([pscustomobject]@{module=$gate.module;suite=$gate.suite;exitCode=$exit;receipt=if(Test-Path -LiteralPath $receipt){Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json}else{$null}})
        }
    } finally { Pop-Location }
} finally { $env:JAVA_HOME = $savedJavaHome }
$after = Get-Inputs
$stable = $serialized -ceq (ConvertTo-Json -InputObject $after -Depth 5 -Compress)
$passed = $stable -and $results.Count -gt 0 -and @($results | Where-Object {$_.exitCode -ne 0}).Count -eq 0
[ordered]@{schemaVersion=1;kind='live-source-components';apkQualification=$false;sourceFingerprint=$fingerprint;sourceStable=$stable;status=if($passed){'PASS'}else{'FAIL'};stage=$stage;results=$results} |
    ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $RunRoot 'run.json') -Encoding utf8
$lines = @('# Rapoartele componentelor pe sursa live','','## Pe scurt','','Această execuție verifică suitele primare Maven de componentă. Nu califică un APK și nu reexecută Android; proveniența și comparațiile sunt în fiecare HTML.','','| Modul | Raport | Metadate |','|---|---|---|')
foreach ($result in $results) { $lines += '| ' + $result.module + ' | [Extent](' + $result.module + '/component-report.html) | [Execuție](' + $result.module + '/execution.json) |' }
$lines += @('','Metadatele confruntă identitățile și rezultatele JUnit cu receipt-urile HTML; fișierele XML și imaginile auxiliare nu sunt livrabile ale acestei execuții. Gradle și acceptanța nativă au colecții separate.')
$lines | Set-Content -LiteralPath (Join-Path $RunRoot 'README.md') -Encoding utf8
if (-not $passed) { throw "Execuția, acoperirea HTML sau stabilitatea surselor a eșuat. Consultați $RunRoot/run.json și $stage." }
Write-Output "PASS: $RunRoot/README.md"
