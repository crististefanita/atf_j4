#requires -Version 7.0
param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string[]]$File = @(),
    [switch]$Json,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenCache = (Join-Path $env:USERPROFILE '.m2/repository'),
    [string]$ScratchRoot = $(if ($env:WHIST_EVIDENCE_ROOT) { $env:WHIST_EVIDENCE_ROOT } else { 'C:\work\whist_back' })
)
$ErrorActionPreference = 'Stop'
$toolRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).ProviderPath
$project = (Resolve-Path -LiteralPath $ProjectRoot).ProviderPath
$parent = (Resolve-Path -LiteralPath $ScratchRoot).ProviderPath
foreach ($inputRoot in @($toolRoot, $project)) {
    $relative = [IO.Path]::GetRelativePath($inputRoot, $parent)
    if ($relative -notmatch '^\.\.(?:[\\/]|$)' -and -not [IO.Path]::IsPathRooted($relative)) {
        throw 'DOC-COMPILER-SCRATCH: use an existing directory outside both the tool and the inspected project.'
    }
}
$compiler = if ($JavaHome) { Join-Path $JavaHome 'bin/javac' } else { (Get-Command javac).Source }
$java = if ($JavaHome) { Join-Path $JavaHome 'bin/java' } else { (Get-Command java).Source }
[xml]$pom = Get-Content -LiteralPath (Join-Path $toolRoot 'pom.xml') -Raw
$jackson = [string]$pom.project.properties.'jackson.version'
if ($jackson -notmatch '^\d+\.\d+\.\d+$') { throw 'DOC-COMPILER-DEPS: missing pinned Jackson version.' }
$yamlPom = Join-Path $MavenCache "com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/$jackson/jackson-dataformat-yaml-$jackson.pom"
if (-not (Test-Path -LiteralPath $yamlPom -PathType Leaf)) { throw "DOC-COMPILER-DEPS: cache missing $yamlPom" }
[xml]$yaml = Get-Content -LiteralPath $yamlPom -Raw
$snake = [string]($yaml.project.dependencies.dependency | Where-Object { $_.groupId -eq 'org.yaml' -and $_.artifactId -eq 'snakeyaml' }).version
if ($snake -notmatch '^\d+(?:\.\d+){1,2}$') { throw 'DOC-COMPILER-DEPS: missing SnakeYAML version in the cached Jackson POM.' }
$jars = @(
    "com/fasterxml/jackson/core/jackson-annotations/$jackson/jackson-annotations-$jackson.jar"
    "com/fasterxml/jackson/core/jackson-core/$jackson/jackson-core-$jackson.jar"
    "com/fasterxml/jackson/core/jackson-databind/$jackson/jackson-databind-$jackson.jar"
    "com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/$jackson/jackson-dataformat-yaml-$jackson.jar"
    "org/yaml/snakeyaml/$snake/snakeyaml-$snake.jar"
) | ForEach-Object {
    $jar = Join-Path $MavenCache $_
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw "DOC-COMPILER-DEPS: cache missing $jar" }
    (Resolve-Path -LiteralPath $jar).ProviderPath
}
$classPath = $jars -join [IO.Path]::PathSeparator
$source = Join-Path $toolRoot 'atf-tests/src/main/java'
$entry = Join-Path $source 'atf/tests/documentation/cli/DocumentationGovernanceCli.java'
$work = Join-Path $parent ('atf-doc-governance-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $work
$result = 1
try {
    & $compiler '-encoding' 'UTF-8' '--release' '21' '-classpath' $classPath '-sourcepath' $source '-d' $work $entry
    if ($LASTEXITCODE -ne 0) { throw "DOC-COMPILER: javac exited with $LASTEXITCODE" }
    $arguments = @('--root', $project)
    foreach ($path in $File) { $arguments += @('--file', $path) }
    if ($Json) { $arguments += '--json' }
    $request = ConvertTo-Json -InputObject $arguments -Compress -EscapeHandling EscapeNonAscii
    $request | & $java '-classpath' ($work + [IO.Path]::PathSeparator + $classPath) 'atf.tests.documentation.cli.DocumentationGovernanceCli' '--stdin'
    $result = $LASTEXITCODE
} finally {
    $actual = (Resolve-Path -LiteralPath $work).ProviderPath
    if ((Split-Path -Parent $actual) -ne $parent -or (Split-Path -Leaf $actual) -cnotmatch '^atf-doc-governance-[a-f0-9]{32}$') {
        throw 'DOC-COMPILER-SCRATCH: refusing to clean an unexpected compiler directory.'
    }
    [IO.Directory]::Delete($actual, $true)
}
exit $result
