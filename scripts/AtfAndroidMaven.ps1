#requires -Version 7.0

function Copy-AtfMavenCache([string]$Cache, [string]$Repository, [string]$Log) {
    if (-not (Test-Path -LiteralPath $Cache -PathType Container)) { throw "MAVEN_CACHE_MISSING: $Cache" }
    if (Test-Path -LiteralPath $Repository) { throw "MAVEN_REPOSITORY_EXISTS: $Repository" }
    $null = New-Item -ItemType Directory -Path $Repository
    & robocopy $Cache $Repository /E /XJ /R:1 /W:1 /XD atf .locks /XF *.lock *.tmp *.lastUpdated /NFL /NDL /NJH /NJS /NP *> $Log
    if ($LASTEXITCODE -ge 8) { throw "MAVEN_CACHE_COPY_FAILED: $Log" }
}

function Get-AtfAndroidClasses([string]$Stage) {
    $module = Join-Path $Stage 'atf-test-android'
    foreach ($scope in @('classes','test-classes')) {
        $folder = Join-Path $module "target/$scope"
        if (-not (Test-Path -LiteralPath $folder -PathType Container)) { throw "ANDROID_CLASSES_MISSING: $scope" }
        $files = @(Get-ChildItem -LiteralPath $folder -Recurse -File)
        if (-not $files.Count) { throw "ANDROID_CLASSES_MISSING: $scope" }
        foreach ($file in $files) {
            $relative = [IO.Path]::GetRelativePath($module,$file.FullName).Replace('\','/')
            $hash = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            "$relative`t$hash"
        }
    }
}

function Get-AtfAndroidClassFingerprint([string]$Stage) {
    [string[]]$records = @(Get-AtfAndroidClasses $Stage)
    [Array]::Sort($records, [StringComparer]::Ordinal)
    Get-AtfTextFingerprint (($records -join "`n") + "`n")
}

function Get-AtfTextFingerprint([string]$Text) {
    $bytes = [Text.Encoding]::UTF8.GetBytes($Text)
    [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes)).ToLowerInvariant()
}

function Assert-AtfCompiledMethod([string]$Javap, [string]$ClassFile, [string]$Method) {
    if (-not (Test-Path -LiteralPath $ClassFile -PathType Leaf)) { throw "ANDROID_CLASS_MISSING: $ClassFile" }
    $members = & $Javap -p $ClassFile
    if ($LASTEXITCODE -ne 0 -or -not ($members -cmatch ('\b' + [regex]::Escape($Method) + '\('))) {
        throw "ANDROID_COMPILED_METHOD_MISSING: $Method ($ClassFile)"
    }
}

function Assert-AtfSingleJUnitPass([string]$Reports, [string]$Selection) {
    $class, $method = $Selection.Split('#',2)
    $files = @(Get-ChildItem -LiteralPath $Reports -File -Filter 'TEST-*.xml')
    if ($files.Count -ne 1) { throw 'ANDROID_JUNIT_EXPECTED_ONE_SUITE' }
    $document = [Xml.XmlDocument]::new()
    $document.XmlResolver = $null
    $settings = [Xml.XmlReaderSettings]::new()
    $settings.DtdProcessing = [Xml.DtdProcessing]::Prohibit
    $reader = [Xml.XmlReader]::Create($files[0].FullName,$settings)
    try { $document.Load($reader) } finally { $reader.Dispose() }
    $suite = $document.SelectSingleNode('/testsuite')
    $cases = $document.SelectNodes('/testsuite/testcase')
    if ($null -eq $suite -or $suite.GetAttribute('tests') -cne '1' -or
        @('failures','errors','skipped').Where({$suite.GetAttribute($_) -cne '0'}).Count -or
        $cases.Count -ne 1 -or $cases[0].GetAttribute('name') -cne $method -or
        ($cases[0].GetAttribute('classname').Split('.') | Select-Object -Last 1) -cne $class -or
        $cases[0].SelectSingleNode('failure|error|skipped')) {
        throw "ANDROID_JUNIT_NOT_EXACT_PASS: $Selection"
    }
}
