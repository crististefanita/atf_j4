#requires -Version 7.0
param(
    [string]$Repository = '',
    [string]$SourceRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$AndroidSdk = 'C:\tools\whist-android-sdk'
)
$ErrorActionPreference = 'Stop'
$source = [IO.Path]::GetFullPath($SourceRoot)
if (!$Repository) {
    $archive = if ($env:WHIST_EVIDENCE_ROOT) { $env:WHIST_EVIDENCE_ROOT } else { 'C:\work\whist_back' }
    $Repository = Join-Path $archive 'libraries/card-table-ui'
}
$repositoryRoot = [IO.Path]::GetFullPath($Repository)
if ($repositoryRoot.Equals($source, [StringComparison]::OrdinalIgnoreCase) -or
    $repositoryRoot.StartsWith($source + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Repository-ul AAR trebuie păstrat în afara surselor.'
}
$module = 'whist-lab-app/atf-card-table-ui'
$build = Get-Content -LiteralPath (Join-Path $source "$module/build.gradle") -Raw
$versionMatch = [regex]::Match($build, '(?m)^version\s*=\s*''([0-9]+\.[0-9]+\.[0-9]+)''\s*$')
if (!$versionMatch.Success) { throw 'Versiunea AAR trebuie declarată explicit în modul.' }
$version = $versionMatch.Groups[1].Value
$baseName = "card-table-ui-$version"
$names = @("$baseName.aar", "$baseName.pom", "$baseName.module", "$baseName-sources.jar")
$artifactPath = "ro/cardtable/card-table-ui/$version"
$final = Join-Path $repositoryRoot $artifactPath
$receiptName = 'card-ui-build.json'

$productionRoots = @("$module/build.gradle", "$module/src/main", 'whist-lab-app/settings.gradle',
    'whist-lab-app/build.gradle', 'whist-lab-app/gradle.properties', 'whist-lab-app/gradlew.bat',
    'whist-lab-app/gradle/wrapper')
$paths = [Collections.Generic.List[string]]::new()
foreach ($relative in $productionRoots) {
    $entry = Get-Item -LiteralPath (Join-Path $source $relative)
    $entries = @($entry)
    if ($entry.PSIsContainer) { $entries += @(Get-ChildItem -LiteralPath $entry.FullName -Recurse -Force) }
    foreach ($item in $entries) {
        if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw "Intrare AAR indirectă: $($item.FullName)"
        }
        if (!$item.PSIsContainer) {
            $paths.Add([IO.Path]::GetRelativePath($source, $item.FullName).Replace('\','/'))
        }
    }
}
$paths.Sort([StringComparer]::Ordinal)
$inputs = @($paths | ForEach-Object {
    [ordered]@{path=$_; sha256=(Get-FileHash -LiteralPath (Join-Path $source $_) -Algorithm SHA256).Hash.ToLowerInvariant()}
})
$inputJson = ConvertTo-Json -InputObject $inputs -Depth 4 -Compress
$fingerprint = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($inputJson))).ToLowerInvariant()

function Confirm-Artifact {
    $receiptPath = Join-Path $final $receiptName
    $receipt = Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
    if ($receipt.schemaVersion -ne 1 -or $receipt.coordinate -cne "ro.cardtable:card-table-ui:$version" -or
        $receipt.inputSha256 -cne $fingerprint) {
        throw "Coordonata $version există cu alte intrări. Păstrați artefactul original și actualizați versiunea modulului."
    }
    if ($receipt.files.Count -ne $names.Count) { throw 'Inventarul AAR are o cardinalitate diferită.' }
    foreach ($name in $names) {
        $record = @($receipt.files | Where-Object {$_.path -ceq $name})
        $file = Get-Item -LiteralPath (Join-Path $final $name)
        if ($record.Count -ne 1 -or $file.PSIsContainer -or ($file.Attributes -band [IO.FileAttributes]::ReparsePoint) -or
            (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash -ine $record[0].sha256) {
            throw "Checksum AAR diferit pentru $name."
        }
    }
    $receiptPath
}

if (Test-Path -LiteralPath $final) {
    $receipt = Confirm-Artifact
    Write-Output "REUSE: $receipt"
    return
}
if (!(Test-Path -LiteralPath (Join-Path $AndroidSdk 'platforms/android-36/android.jar') -PathType Leaf)) {
    throw 'Este necesar Android SDK platform 36 pentru construirea AAR.'
}
$work = Join-Path $repositoryRoot ('.card-ui-work/' + [guid]::NewGuid().ToString('N'))
$stage = Join-Path $work 'source'
$publishing = Join-Path $work 'published'
$inputManifest = Join-Path $work 'source-inputs.json'
New-Item -ItemType Directory -Path $stage | Out-Null
[IO.File]::WriteAllText($inputManifest, $inputJson)
& (Join-Path $PSScriptRoot 'Copy-AtfSourceInputs.ps1') -SourceRoot $source -Destination $stage -InputManifest $inputManifest
$androidHome = $env:ANDROID_HOME
$androidSdkRoot = $env:ANDROID_SDK_ROOT
Push-Location (Join-Path $stage 'whist-lab-app')
try {
    $env:ANDROID_HOME = [IO.Path]::GetFullPath($AndroidSdk)
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    & ./gradlew.bat --offline --no-daemon --console=plain '-Pcardui.only=package' "-Pcardui.repository=$publishing" ':atf-card-table-ui:publishReleasePublicationToIsolatedRepository' *> (Join-Path $work 'gradle.log')
    if ($LASTEXITCODE -ne 0) { throw "Împachetarea a eșuat. Consultați $work/gradle.log." }
} finally {
    $env:ANDROID_HOME = $androidHome
    $env:ANDROID_SDK_ROOT = $androidSdkRoot
    Pop-Location
}
$produced = Join-Path $publishing $artifactPath
$handoff = Join-Path $work 'handoff'
New-Item -ItemType Directory -Path $handoff | Out-Null
$files = foreach ($name in $names) {
    $built = Join-Path $produced $name
    Copy-Item -LiteralPath $built -Destination (Join-Path $handoff $name)
    [ordered]@{path=$name; sha256=(Get-FileHash -LiteralPath $built -Algorithm SHA256).Hash.ToLowerInvariant()}
}
[ordered]@{schemaVersion=1; coordinate="ro.cardtable:card-table-ui:$version"; inputSha256=$fingerprint; inputs=$inputs; files=@($files)} |
    ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $handoff $receiptName) -Encoding utf8
New-Item -ItemType Directory -Path (Split-Path -Parent $final) -Force | Out-Null
try {
    [IO.Directory]::Move($handoff, $final)
} catch [IO.IOException] {
    if (!(Test-Path -LiteralPath $final -PathType Container)) { throw }
    $receipt = Confirm-Artifact
    Write-Output "REUSE: $receipt"
    return
}
$receipt = Confirm-Artifact
Write-Output "BUILT: $receipt"
