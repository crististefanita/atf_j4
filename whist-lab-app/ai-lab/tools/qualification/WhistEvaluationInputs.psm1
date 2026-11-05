Set-StrictMode -Version Latest

function Get-WhistEvaluationSourceHashes {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$AppRoot)
    $root=[IO.Path]::GetFullPath($AppRoot)
    $hashes=@{}
    foreach($file in Get-ChildItem -LiteralPath $root -Recurse -File){
        $relative=[IO.Path]::GetRelativePath($root,$file.FullName).Replace('\','/')
        if(($relative -split '/') | Where-Object {$_ -in @('build','.gradle','.git','node_modules')}){continue}
        $included=$relative -match '^(game-core|ai-lab)/src/(main|test)/' -or
            $relative -match '^(game-core|ai-lab)/build\.gradle$' -or
            $relative -match '^algorithm-build/(settings\.gradle|build\.gradle|gradle\.properties)$' -or
            $relative -match '^gradle/wrapper/(gradle-wrapper\.jar|gradle-wrapper\.properties)$' -or
            $relative -match '^gradlew(\.bat)?$' -or
            $relative -match '^ai-lab/tools/qualification/(whist-evaluation-registry\.mjs|half-open-interval\.mjs|WhistEvaluationInputs\.psm1)$'
        if(-not $included){continue}
        if($file.Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Evaluation sources must be physical files.'}
        $hashes[$relative]=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    }
    if($hashes.Count -eq 0){throw 'Executable evaluation source inventory is empty.'}
    return $hashes
}

Export-ModuleMember -Function Get-WhistEvaluationSourceHashes
