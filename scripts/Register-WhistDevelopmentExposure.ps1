param(
    [Parameter(Mandatory)][long]$Seed,
    [Parameter(Mandatory)][string]$RunRoot,
    [Parameter(Mandatory)][ValidateSet('perspectives','policy-match','strategy-profiles')][string]$Kind
)
$ErrorActionPreference='Stop'
$sourceRoot=Split-Path -Parent $PSScriptRoot
$appRoot=Join-Path $sourceRoot 'whist-lab-app'
$policy=Get-Content -LiteralPath (Join-Path $sourceRoot 'config/whist-evaluation-policy.json') -Raw | ConvertFrom-Json
if($policy.schemaVersion -ne 1 -or -not [IO.Path]::IsPathRooted($policy.registryRoot)){throw 'Invalid installation-wide evaluation registry policy.'}
Import-Module (Join-Path $appRoot 'ai-lab/tools/qualification/WhistEvaluationInputs.psm1') -Force
$hashes=Get-WhistEvaluationSourceHashes -AppRoot $appRoot
$request=@{candidate='diagnostic-'+$Kind;phase='development';firstSeed=$Seed;seeds=1;sourceHashes=$hashes;protocolSha256=(Get-FileHash -LiteralPath $PSCommandPath -Algorithm SHA256).Hash.ToLowerInvariant();run=[IO.Path]::GetFullPath($RunRoot);corpus=$null}
$requestFile=Join-Path $RunRoot 'evaluation-request.json'
$request | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $requestFile -Encoding utf8NoBOM
& node (Join-Path $appRoot 'ai-lab/tools/qualification/whist-evaluation-registry.mjs') reserve $policy.registryRoot $requestFile (Join-Path $RunRoot 'evaluation-reservation.json')
if($LASTEXITCODE -ne 0){throw 'Development exposure registration failed; no match was started.'}
