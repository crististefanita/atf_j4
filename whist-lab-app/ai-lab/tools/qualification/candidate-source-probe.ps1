param(
    [ValidateSet('baseline','accepted','staged','ownership')][string]$Mode,
    [string]$AppRoot,
    [string]$Manifest,
    [switch]$Server
)
$ErrorActionPreference='Stop'
[Console]::InputEncoding=[Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)
Import-Module (Join-Path $PSScriptRoot 'WhistCandidateSource.psm1') -Force
function Invoke-CandidateProbe([string]$Operation,[string]$Root,[string]$SpecificationFile) {
    try {
        if ($Operation -notin @('baseline','accepted','staged','ownership') -or [string]::IsNullOrWhiteSpace($Root)) { throw 'Explicit probe operation and root required.' }
        $result = if ($Operation -eq 'baseline') { New-WhistCandidateBaseline -AppRoot $Root }
        elseif ($Operation -eq 'ownership') {
            $first = New-WhistCandidateBaseline -AppRoot $Root
            $first.baselineInventory.roots[0] = 'invalid-scope'
            $baseline = New-WhistCandidateBaseline -AppRoot $Root
            if ($baseline.baselineInventory.roots[0] -cne 'game-core/src/main/java') { throw 'Returned baseline mutated the validator scope.' }
            $spec = [pscustomobject]@{schemaVersion=2;baselineGuards=$baseline.baselineGuards;baselineInventory=$baseline.baselineInventory;files=@()}
            $proof = Assert-WhistCandidateAcceptedSource -AppRoot $Root -Specification $spec
            $proof.roots[0] = 'invalid-scope'
            $verified = Assert-WhistCandidateAcceptedSource -AppRoot $Root -Specification $spec
            if ($verified.roots[0] -cne 'game-core/src/main/java') { throw 'Returned proof mutated the validator scope.' }
            @{baselineIsolated=$true;proofIsolated=$true;sourceCount=$verified.sourceCount}
        }
        else {
            $specification = Get-Content -LiteralPath $SpecificationFile -Raw | ConvertFrom-Json
            if ($Operation -eq 'accepted') { Assert-WhistCandidateAcceptedSource -AppRoot $Root -Specification $specification }
            else { Assert-WhistStagedCandidateSource -AppRoot $Root -Specification $specification }
        }
        @{exit=0;result=$result}
    } catch {
        @{exit=1;result=@{status='FAIL';error=$_.Exception.Message}}
    }
}
if ($Server) {
    while ($null -ne ($line = [Console]::ReadLine())) {
        $request = $line | ConvertFrom-Json
        $reply = Invoke-CandidateProbe $request.mode $request.root $request.manifest
        $reply.requestId = $request.requestId
        [Console]::WriteLine(($reply | ConvertTo-Json -Depth 12 -Compress))
    }
} else {
    $reply = Invoke-CandidateProbe $Mode $AppRoot $Manifest
    $reply.result | ConvertTo-Json -Depth 12 -Compress
    exit $reply.exit
}
