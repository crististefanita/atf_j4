function Get-AtfJvmCompilerClasspath {
    param([string]$ToolRoot, [string]$MavenCache, [switch]$Yaml, [switch]$Core)
    [xml]$pom = Get-Content -LiteralPath (Join-Path $ToolRoot 'pom.xml') -Raw
    $jackson = [string]$pom.project.properties.'jackson.version'
    if ($jackson -notmatch '^\d+\.\d+\.\d+$') { throw 'ATF-COMPILER-DEPS: missing pinned Jackson version.' }
    $jars = @(
        "com/fasterxml/jackson/core/jackson-annotations/$jackson/jackson-annotations-$jackson.jar"
        "com/fasterxml/jackson/core/jackson-core/$jackson/jackson-core-$jackson.jar"
        "com/fasterxml/jackson/core/jackson-databind/$jackson/jackson-databind-$jackson.jar"
    )
    if ($Yaml) {
        $yamlPom = Join-Path $MavenCache "com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/$jackson/jackson-dataformat-yaml-$jackson.pom"
        [xml]$metadata = Get-Content -LiteralPath $yamlPom -Raw
        $snake = [string]($metadata.project.dependencies.dependency | Where-Object { $_.groupId -eq 'org.yaml' -and $_.artifactId -eq 'snakeyaml' }).version
        if ($snake -notmatch '^\d+(?:\.\d+){1,2}$') { throw 'ATF-COMPILER-DEPS: missing SnakeYAML version in the cached Jackson POM.' }
        $jars += "com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/$jackson/jackson-dataformat-yaml-$jackson.jar"
        $jars += "org/yaml/snakeyaml/$snake/snakeyaml-$snake.jar"
    }
    $paths = @($jars | ForEach-Object { Join-Path $MavenCache $_ })
    if ($Core) {
        $version = [string]$pom.project.properties.'atf.core.version'
        if ($version -notmatch '^\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?$') { throw 'ATF-COMPILER-DEPS: missing pinned Core version.' }
        $paths += Join-Path $ToolRoot "lib/atf-core-$version.jar"
    }
    $resolved = foreach ($path in $paths) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "ATF-COMPILER-DEPS: missing $path" }
        (Resolve-Path -LiteralPath $path).ProviderPath
    }
    $resolved -join [IO.Path]::PathSeparator
}

function Invoke-AtfJvmCli {
    param(
        [string]$ToolRoot, [string]$ProjectRoot, [string]$ScratchRoot, [string]$JavaHome,
        [string]$ClassPath, [string]$MainClass, [string[]]$Arguments, [ref]$Result
    )
    $parent = (Resolve-Path -LiteralPath $ScratchRoot).ProviderPath
    foreach ($inputRoot in @($ToolRoot, $ProjectRoot)) {
        $relative = [IO.Path]::GetRelativePath($inputRoot, $parent)
        if ($relative -notmatch '^\.\.(?:[\\/]|$)' -and -not [IO.Path]::IsPathRooted($relative)) {
            throw 'ATF-COMPILER-SCRATCH: use an existing directory outside both the tool and the inspected project.'
        }
    }
    $compiler = if ($JavaHome) { Join-Path $JavaHome 'bin/javac' } else { (Get-Command javac).Source }
    $java = if ($JavaHome) { Join-Path $JavaHome 'bin/java' } else { (Get-Command java).Source }
    $source = Join-Path $ToolRoot 'atf-tests/src/main/java'
    $entry = Join-Path $source ($MainClass.Replace('.', '/') + '.java')
    $work = Join-Path $parent ('atf-jvm-cli-' + [guid]::NewGuid().ToString('N'))
    $null = New-Item -ItemType Directory -Path $work
    try {
        & $compiler '-encoding' 'UTF-8' '--release' '21' '-classpath' $ClassPath '-sourcepath' $source '-d' $work $entry
        if ($LASTEXITCODE -ne 0) { throw "ATF-COMPILER: javac exited with $LASTEXITCODE" }
        $request = ConvertTo-Json -InputObject (@('--root', $ProjectRoot) + $Arguments) -Compress -EscapeHandling EscapeNonAscii
        $request | & $java '-classpath' ($work + [IO.Path]::PathSeparator + $ClassPath) $MainClass '--stdin'
        $Result.Value = $LASTEXITCODE
    } finally {
        $actual = (Resolve-Path -LiteralPath $work).ProviderPath
        if ((Split-Path -Parent $actual) -ne $parent -or (Split-Path -Leaf $actual) -cnotmatch '^atf-jvm-cli-[a-f0-9]{32}$') {
            throw 'ATF-COMPILER-SCRATCH: refusing to clean an unexpected compiler directory.'
        }
        [IO.Directory]::Delete($actual, $true)
    }
}
