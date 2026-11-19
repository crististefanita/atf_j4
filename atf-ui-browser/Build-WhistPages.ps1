#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$WorkRoot,
    [string]$CatalogSource,
    [string]$JsonLibrary
)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath($WorkRoot)
$jvm = & (Join-Path $PSScriptRoot 'Start-WhistBrowser.ps1') -WorkRoot $root `
    -CatalogSource $CatalogSource -JsonLibrary $JsonLibrary -BuildOnly
$run = Split-Path ($jvm.Split([IO.Path]::PathSeparator)[0])
$pages = Join-Path $PSScriptRoot 'pages'
$toolClasspath = Join-Path $run 'teavm.classpath'
$compilerLog = Join-Path $run 'teavm-dependencies.log'
& mvn -B -ntp -f (Join-Path $pages 'teavm-dependencies.xml') 'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath' `
    "-Dmdep.outputFile=$toolClasspath" *> $compilerLog
if ($LASTEXITCODE -ne 0) { throw "Could not resolve TeaVM. See $compilerLog" }
$classpath = @($jvm, (Get-Content -LiteralPath $toolClasspath -Raw).Trim()) -join [IO.Path]::PathSeparator
$classes = Join-Path $run 'pages-classes'
$site = Join-Path $run 'site'
$null = New-Item -ItemType Directory -Path $classes, $site
$sources = Get-ChildItem -LiteralPath (Join-Path $pages 'src/main/java') -Filter '*.java' -File -Recurse |
    ForEach-Object FullName
& javac --release 17 -encoding UTF-8 -cp $classpath -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Whist Pages adapter compilation failed.' }
$compilerLog = Join-Path $run 'teavm.log'
& java "-Djava.io.tmpdir=$run" '-XX:ActiveProcessorCount=3' -Xmx1g -cp (@($classes, $classpath) -join [IO.Path]::PathSeparator) `
    (Join-Path $pages 'CompileWhistPage.java') $site *> $compilerLog
if ($LASTEXITCODE -ne 0) { throw "Whist engine compilation failed. See $compilerLog" }
$web = Join-Path $PSScriptRoot 'src/main/web'
Get-ChildItem -LiteralPath $web -File | Copy-Item -Destination $site
Get-ChildItem -LiteralPath (Join-Path $pages 'web') -File | Copy-Item -Destination $site
$footerImage = Join-Path $PSScriptRoot '../whist-lab-app/app/src/main/res/drawable-nodpi/teo_dedication.png'
Copy-Item -LiteralPath $footerImage -Destination (Join-Path $site 'menu-footer.png')
$index = Join-Path $site 'index.html'
$markup = [IO.File]::ReadAllText($index).Replace('<html lang="ro">', '<html lang="ro" data-engine="whist">')
$markup = $markup.Replace('<title>Card Table</title>', '<title>Whist Lab</title>')
$cssRevision = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $site 'table.css')).Hash.ToLowerInvariant().Substring(0, 20)
$markup = $markup -replace 'table\.css\?v=[^" ]+', "table.css?v=$cssRevision"
$policy = "default-src 'self'; connect-src 'none'; style-src 'self' 'unsafe-inline'; worker-src 'self'; base-uri 'none'"
$markup = $markup.Replace('<meta charset="utf-8">', "<meta charset=`"utf-8`">`n  <meta http-equiv=`"Content-Security-Policy`" content=`"$policy`">")
[IO.File]::WriteAllText($index, $markup)
[IO.File]::WriteAllText((Join-Path $site '.nojekyll'), '')
$site
