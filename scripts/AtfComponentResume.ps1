#requires -Version 7.0

function Write-AtfComponentJson([string]$File, $Document) {
    $temporary = $File + '.writing'
    [IO.File]::WriteAllText($temporary, (($Document | ConvertTo-Json -Depth 12) + [Environment]::NewLine))
    [IO.File]::Move($temporary, $File, $true)
}

function Get-AtfResumeComponentWorkspace([string]$Previous, [string]$TempRoot) {
    $document = Get-Content -LiteralPath (Join-Path $Previous 'component-workspace.json') -Raw | ConvertFrom-Json
    if ($document.schemaVersion -ne 1 -or !$document.path) { throw 'COMPONENT_RESUME_WORKSPACE_RECEIPT' }
    $work = [IO.Path]::GetFullPath($document.path)
    $entry = Get-Item -LiteralPath $work -Force
    if ((Split-Path -Parent $work) -ine $TempRoot -or
        (Split-Path -Leaf $work) -cnotmatch '^whist-component-suites-[a-f0-9]{32}$' -or
        !$entry.PSIsContainer -or ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw 'COMPONENT_RESUME_WORKSPACE_PERIMETER'
    }
    $entry.FullName
}

function Restore-AtfComponentSnapshot([string]$Previous, [string]$Work, [string]$SourceRoot, [string]$PinnedSourceInputs) {
    $document = Get-Content -LiteralPath (Join-Path $Previous 'component-workspace.json') -Raw | ConvertFrom-Json
    $inputs = Get-Content -LiteralPath (Join-Path $Previous 'source-inputs.json') -Raw
    $release = Get-Content -LiteralPath (Join-Path $Previous 'release-inputs.json') -Raw
    $rows = ConvertFrom-Json -InputObject $inputs -NoEnumerate
    if ($document.sourceFingerprint -cne (Get-AtfInputFingerprint $inputs) -or
        $document.releaseFingerprint -cne (Get-AtfInputFingerprint $release) -or !$rows.Count) {
        throw 'COMPONENT_RESUME_SNAPSHOT_RECEIPT'
    }
    if ($PinnedSourceInputs) {
        $pin = Get-Content -LiteralPath $PinnedSourceInputs -Raw | ConvertFrom-Json -NoEnumerate
        if ($pin -isnot [array] -or (ConvertTo-AtfInputJson @($pin | Select-Object path,sha256)) -cne $inputs) {
            throw 'QUALIFICATION-SOURCE-DIFFERS-FROM-PLAN'
        }
    }
    $snapshot = [pscustomobject]@{source=$inputs;release=$release;inputCount=$rows.Count}
    Assert-AtfQualificationSnapshot $snapshot (Join-Path $Work 'project') $SourceRoot
    $snapshot
}

function Get-AtfComponentMavenFingerprint([string]$Work, [string]$Repository, [string[]]$Modules) {
    $roots = @($Repository) + @($Modules | ForEach-Object {
        Join-Path $Work "project/$_/target/classes"
        Join-Path $Work "project/$_/target/test-classes"
    })
    $records = [Collections.Generic.List[string]]::new()
    foreach ($root in $roots) {
        if (!(Test-Path -LiteralPath $root -PathType Container)) { continue }
        if ((Get-Item -LiteralPath $root).Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw 'COMPONENT_MAVEN_INDIRECT'
        }
        foreach ($file in Get-ChildItem -LiteralPath $root -Recurse -File -Force) {
            if ($file.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'COMPONENT_MAVEN_INDIRECT' }
            if ($root -eq $Repository -and $file.Extension -cnotin @('.jar','.pom')) { continue }
            $relative = [IO.Path]::GetRelativePath($Work,$file.FullName).Replace('\','/')
            $records.Add($relative + ':' + (Get-FileHash -LiteralPath $file.FullName).Hash.ToLowerInvariant())
        }
    }
    $records.Sort([StringComparer]::Ordinal)
    if (!$records.Count) { throw 'COMPONENT_MAVEN_OUTPUTS_MISSING' }
    Get-AtfInputFingerprint ($records -join [char]10)
}

function Read-AtfPreparedComponentMaven([string]$Work, [string]$Identity, [string[]]$Modules) {
    $file = Join-Path $Work 'component-maven.json'
    if (!(Test-Path -LiteralPath $file -PathType Leaf)) { return }
    try {
        $receipt = Get-Content -LiteralPath $file -Raw | ConvertFrom-Json
        if ($receipt.schemaVersion -ne 1 -or $receipt.identity -cne $Identity -or
            $receipt.repo -cnotmatch '^maven-repository(?:-[a-f0-9]{8})?$') { return }
        $repository = Join-Path $Work $receipt.repo
        if (!(Test-Path -LiteralPath $repository -PathType Container) -or
            (Get-AtfComponentMavenFingerprint $Work $repository $Modules) -cne $receipt.buildFingerprint) { return }
        $classpath = Join-Path $Work 'component-classpath.txt'
        $classpathReady = (Test-Path -LiteralPath $classpath -PathType Leaf) -and
            (Get-FileHash -LiteralPath $classpath).Hash -ceq $receipt.classpathSha256
        [pscustomobject]@{repository=$repository;classpathReady=$classpathReady;receipt=$receipt}
    } catch {
        Write-Verbose "Discarding Maven preparation: $($_.Exception.Message)"
    }
}
