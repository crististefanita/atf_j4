import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {fileURLToPath} from 'node:url';
import {deriveReleaseTestScope} from './release-test-scope.mjs';
import {componentEvidenceScope} from './component-evidence-scope.mjs';

test('user inventories accept LF and CRLF without admitting duplicate or incomplete identities', t => {
  const root=fs.mkdtempSync(path.join(os.tmpdir(),'whist-user-list-'));
  t.after(()=>fs.rmSync(root,{recursive:true,force:true}));
  const script=path.join(root,'inventory.ps1');
  fs.writeFileSync(script,String.raw`
param($Helper)
$ErrorActionPreference='Stop'
. $Helper
$rows=@();$crlf=[string][char]13+[char]10;$lf=[string][char]10;$tab=[char]9
foreach($ending in @('LF','CRLF')) {
 $nl=if($ending-eq'LF'){$lf}else{$crlf}
 foreach($terminated in @($false,$true)) {
  $text='Users:'+$nl+$tab+'UserInfo{0:Owner:13} running'+$nl+$tab+'UserInfo{10:Foreign profile:0}'
  if($terminated){$text+=$nl}
  $users=@(ConvertFrom-AtfAndroidUsers $text)
  $rows+=@{format=$ending;terminated=$terminated;ids=@($users.id);names=@($users.name)}
 }
}
$rejected=@()
foreach($invalid in @(('Users:'+$crlf+' UserInfo{0:Owner:13}'+$crlf+' UserInfo{0:Duplicate:0}'),
 ('Users:'+$crlf+' UserInfo{0:Owner:13}'+$crlf+' UserInfo{10:Truncated'),'Users:',"cmd: Can't find service: package")) {
 try {ConvertFrom-AtfAndroidUsers $invalid|Out-Null;$rejected+='ACCEPTED'}
 catch {$rejected+=$_.Exception.Message}
}
@{cases=$rows;rejected=$rejected}|ConvertTo-Json -Depth 5 -Compress
`);
  const output=execFileSync('pwsh',['-NoProfile','-File',script,
    fileURLToPath(new URL('../../scripts/AtfAndroidUserOwnership.ps1',import.meta.url))],{encoding:'utf8',timeout:30000,windowsHide:true});
  const result=JSON.parse(output);assert.equal(result.cases.length,4);
  for(const row of result.cases){assert.deepEqual(row.ids,[0,10]);assert.deepEqual(row.names,['Owner','Foreign profile']);}
  assert.deepEqual(result.rejected,Array(4).fill('ANDROID_USER_LIST_UNAVAILABLE'));
  console.log('User inventory observations: '+JSON.stringify(result));
});

test('ownership tooling selects governance while preserving unchanged game evidence',()=>{
  const groups=['atf-tests','atf-test-android','whist-lab-app-bidding','whist-lab-app-atf-card-table-ui'];
  const changed=new Map([['scripts/AtfAndroidUserOwnership.ps1','new']]);
  const selection=deriveReleaseTestScope(new Map(),changed,groups);
  assert.deepEqual(selection.runComponentGroups,['atf-tests']);assert.deepEqual(selection.requiredE2eGroups,[]);
  for(const group of groups.slice(1))assert.equal(componentEvidenceScope(group,changed,new Map()).decision,'REUSE');
  assert.equal(componentEvidenceScope('atf-tests',changed,new Map()).decision,'RUN');
});

test('durable user ownership recovers interruptions and refuses foreign or uncertain identities', t => {
  const root=fs.mkdtempSync(path.join(os.tmpdir(),'whist-owner-'));
  t.after(()=>fs.rmSync(root,{recursive:true,force:true}));
  const script=path.join(root,'cases.ps1');
  fs.writeFileSync(script,String.raw`
param($Helper,$Root)
$ErrorActionPreference='Stop'
. $Helper
$results=@()
foreach($case in @('lost-id','bound-id','remove-timeout','package-unavailable','foreign-name','original-id','preexisting-id',
 'wrong-serial','wrong-root','ambiguous-name','unconfirmed','idempotent','busy','id-reused','package-changed','missing-original')) {
 $dir=New-Item -ItemType Directory (Join-Path $Root $case)
 $file=Join-Path $dir.FullName 'owner.json'
 $ctx=[pscustomobject]@{users=[Collections.Generic.List[object]]::new();events=[Collections.Generic.List[string]]::new();current=12;unavailable=$false;timeout=$false;installed=$true}
 $ctx.users.Add([pscustomobject]@{id=[long]0;name='Original'})
 $ctx.users.Add([pscustomobject]@{id=[long]9;name='Foreign'})
 if($case -eq 'preexisting-id'){$ctx.users.Add([pscustomobject]@{id=[long]12;name='Already present'})}
 $intent=New-AtfUserOwnership $file 'test-device' ('a'*32) $dir.FullName 0 $true $ctx.users.ToArray()
 $before=Get-Content $file -Raw|ConvertFrom-Json
 if($before.state -cne 'CREATING' -or $null -ne $before.testUserId){throw 'Intent was not durable before creation'}
 if($case -ne 'unconfirmed' -and $case -ne 'preexisting-id'){$ctx.users.Add([pscustomobject]@{id=[long]12;name=$intent.userName})}
 if($case -eq 'ambiguous-name'){$ctx.users.Add([pscustomobject]@{id=[long]13;name=$intent.userName})}
 $caseError='';$lock=$null
 try {
  if($case -eq 'original-id'){Set-AtfOwnedUserId $file 'test-device' $dir.FullName 0|Out-Null}
  if($case -eq 'preexisting-id'){Set-AtfOwnedUserId $file 'test-device' $dir.FullName 12|Out-Null}
  if($case -in @('bound-id','foreign-name','remove-timeout','id-reused')){Set-AtfOwnedUserId $file 'test-device' $dir.FullName 12|Out-Null}
  if($case -eq 'foreign-name'){$ctx.users[2].name='Someone else'}
  if($case -eq 'missing-original'){$ctx.users.RemoveAt(0)}
  if($case -eq 'id-reused'){$r=Read-AtfUserOwnership $file 'test-device' $dir.FullName;$r.state='REMOVED';Write-AtfUserOwnership $file $r}
  if($case -eq 'package-unavailable'){$ctx.unavailable=$true}
  if($case -eq 'remove-timeout'){$ctx.timeout=$true}
  if($case -eq 'package-changed'){$ctx.installed=$false}
  if($case -eq 'busy'){$lock=[IO.File]::Open($file+'.lock',[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)}
  $args=@{Path=$file;Serial=$(if($case-eq'wrong-serial'){'other'}else{'test-device'});RunRoot=$(if($case-eq'wrong-root'){$Root}else{$dir.FullName});LeaseId=('b'*32)
   ReadUsers={if($ctx.unavailable){throw 'PACKAGE_SERVICE_UNAVAILABLE'};$ctx.users.ToArray()}
   ReadCurrent={$ctx.current};SwitchOriginal={param($id)$ctx.events.Add('switch:'+ $id);$ctx.current=$id}
   RemoveUser={param($id)$ctx.events.Add('remove:'+ $id);$ctx.users.RemoveAt(($ctx.users.id.IndexOf([long]$id)));if($ctx.timeout){$ctx.timeout=$false;throw 'ORIGINAL_REMOVE_TIMEOUT'}}
   ReadOriginalPackage={param($id)$ctx.installed}}
  try {Complete-AtfOwnedUser @args|Out-Null} catch {
   if($case -eq 'remove-timeout'){
    if($_.Exception.Message -cne 'ORIGINAL_REMOVE_TIMEOUT'){throw}
    if((Read-AtfUserOwnership $file 'test-device' $dir.FullName).state -cne 'REMOVING'){throw 'Lost removal checkpoint'}
    Complete-AtfOwnedUser @args|Out-Null
   } elseif($case -eq 'package-unavailable'){
    if($ctx.events.Count){throw 'Mutated users while package service unavailable'}
    $ctx.unavailable=$false;Complete-AtfOwnedUser @args|Out-Null
   } else {throw}
  }
  if($case -eq 'idempotent'){Complete-AtfOwnedUser @args|Out-Null}
 } catch {$caseError=$_.Exception.Message} finally {if($lock){$lock.Dispose()}}
 $r=Get-Content $file -Raw|ConvertFrom-Json
 $results+=@{case=$case;error=$caseError;state=$r.state;events=$ctx.events.ToArray();remaining=$ctx.users.id;originalPackage=$ctx.installed}
}
$invalid=''
try {ConvertFrom-AtfAndroidUsers "cmd: Can't find service: package"|Out-Null}catch{$invalid=$_.Exception.Message}
@{rows=$results;unavailableList=$invalid}|ConvertTo-Json -Depth 8 -Compress
`);
  const helper=fileURLToPath(new URL('../../scripts/AtfAndroidUserOwnership.ps1',import.meta.url));
  const result=JSON.parse(execFileSync('pwsh',['-NoProfile','-NonInteractive','-File',script,helper,root],{encoding:'utf8',timeout:60000}));
  const accepted=new Set(['lost-id','bound-id','remove-timeout','package-unavailable','idempotent']);
  for(const row of result.rows){
    if(accepted.has(row.case)){
      assert.equal(row.error,'',JSON.stringify(row));assert.equal(row.state,'REMOVED');
      assert.deepEqual(row.events,['switch:0','remove:12']);assert.deepEqual(row.remaining,[0,9]);
    }else{assert.ok(row.error,JSON.stringify(row));assert.deepEqual(row.events,[],row.case);}
  }
  assert.equal(result.unavailableList,'ANDROID_USER_LIST_UNAVAILABLE');
  assert.equal(result.rows.length,16);
  console.log('Ownership case observations: '+JSON.stringify(result.rows));
  console.log('16 durable ownership outcomes: interruption, idempotence and refusal preserve original and foreign users');
});

test('the recovery adapter uses the active lease and reports recovery without an application verdict', t => {
  const root=fs.mkdtempSync(path.join(os.tmpdir(),'whist-owner-adapter-'));
  t.after(()=>fs.rmSync(root,{recursive:true,force:true}));
  const checkout=path.join(root,'checkout'), scripts=path.join(checkout,'scripts');
  fs.mkdirSync(scripts,{recursive:true});
  for(const file of ['Invoke-AtfAndroidDisposableUser.ps1','AtfAndroidUserOwnership.ps1',
    'AtfAndroidPreflight.ps1','AtfAndroidPackageRegistration.ps1','AtfAndroidUserReadiness.ps1','AtfDeviceLease.ps1',
    'AtfLocalPaths.ps1']){
    fs.copyFileSync(fileURLToPath(new URL('../../scripts/'+file,import.meta.url)),path.join(scripts,file));
  }
  const module=path.join(checkout,'atf-test-android/tools/AndroidDeviceSetup.psm1');
  fs.mkdirSync(path.dirname(module),{recursive:true});
  fs.writeFileSync(module,String.raw`
function Invoke-AtfAdbCommand($AdbExecutable,$Arguments,$ServerSocket,$TimeoutSeconds){
 $state=Get-Content $env:ATF_OWNERSHIP_MOCK -Raw|ConvertFrom-Json
 $command=$Arguments[3..($Arguments.Count-1)] -join ' '
 $state.commands+=@($command)
 $exitCode=0
 $output=switch -Regex($command){
  '^pm list users$' {if($state.failUsers){"cmd: Can't find service: package"}else{"Users:";foreach($user in $state.users){" UserInfo{$($user.id):$($user.name):0} running"}};break}
  '^pm create-user ATFWhist-' {$state.users+=@([pscustomobject]@{id=12;name=$command.Split(' ')[2]});'Success: created user id 12';break}
  '^pm list packages --user 12$' {$state.failUsers=$state.cleanupUnavailable;$exitCode=1;'ORIGINAL_PREPARATION_FAILURE';break}
  '^am get-current-user$' {[string]$state.current;break}
  '^am switch-user 0$' {$state.current=0;'Success';break}
  '^pm remove-user 12$' {$state.users=@($state.users|Where-Object id -NE 12);'Success';break}
  '^pm list packages --user 0 ro.whistlab.app$' {if($state.serviceUnavailable){"cmd: Can't find service: package"}else{'package:ro.whistlab.app'};break}
  default {throw "Unexpected mock command: $command"}
 }
 $state|ConvertTo-Json -Depth 8|Set-Content $env:ATF_OWNERSHIP_MOCK
 [pscustomobject]@{ExitCode=$exitCode;Output=($output -join "\n")}
}
Export-ModuleMember -Function Invoke-AtfAdbCommand
`.replace('($output -join "\\n")','($output -join "`n")'));
  fs.mkdirSync(path.join(checkout,'config'),{recursive:true});
  fs.writeFileSync(path.join(checkout,'config/release-platform-rag-policy.json'),
    JSON.stringify({ragEvaluator:{evidenceFreshness:{androidPreflightMaxAgeSeconds:300}}}));
  fs.writeFileSync(path.join(scripts,'check-android-release-device.ps1'),String.raw`
param($AdbExecutable,$Serial,$AdbServerSocket,$LeasePath,$LeaseId,$ExpectedApi,$ExpectedDisplayProfile,$Receipt)
[pscustomobject]@{schemaVersion=2;status='PASS';serial=$Serial;leaseId=$LeaseId;adbServerSocket=$AdbServerSocket;
 expectedApi=$ExpectedApi;expectedDisplayProfile=$ExpectedDisplayProfile;observedAtUtc=[DateTimeOffset]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ss.fffZ');
 observed=[pscustomobject]@{bootCompleted=$true;api=[int]$ExpectedApi;displayProfile=$ExpectedDisplayProfile}}
`);
  const harness=path.join(root,'adapter.ps1');
  fs.writeFileSync(harness,String.raw`
param($Scripts,$Root)
$ErrorActionPreference='Stop'
. (Join-Path $Scripts 'AtfAndroidUserOwnership.ps1')
$rows=@()
foreach($case in @('recovery','wrong-lease-run','service-unavailable','prepare-failure','prepare-cleanup-unavailable')){
 $run=Join-Path $Root $case;$null=New-Item -ItemType Directory $run
 $journal=Join-Path $run 'ownership.json';$lease=Join-Path $run 'lease.json'
 $preparing=$case.StartsWith('prepare-')
 if(-not $preparing){$record=New-AtfUserOwnership $journal 'mock-device' ('a'*32) $run 0 $true @([pscustomobject]@{id=[long]0;name='Original'})}
 $state=Join-Path $run 'device.json';$env:ATF_OWNERSHIP_MOCK=$state
 $users=@(@{id=0;name='Original'},@{id=9;name='Foreign'})
 if(-not $preparing){$users+=@(@{id=12;name=$record.userName})}
 @{users=$users;current=$(if($preparing){0}else{12});commands=@();failUsers=$false;
   cleanupUnavailable=($case -eq 'prepare-cleanup-unavailable');serviceUnavailable=($case -eq 'service-unavailable')}|ConvertTo-Json -Depth 8|Set-Content $state
 @{active=$true;leaseId=('b'*32);serial='mock-device';run=$(if($case -eq 'wrong-lease-run'){$Root}else{$run})}|ConvertTo-Json|Set-Content $lease
 $caseError=''
 try {
  $parameters=@{Phase='Recover';RunRoot=$run;Serial='mock-device';AdbExecutable='MOCK-NO-ADB';LeasePath=$lease;
   LeaseId=('b'*32);OwnershipJournal=$journal}
  if($preparing){
   $parameters.Phase='Prepare';$parameters.ApkPath=Join-Path $run 'candidate.apk'
   [IO.File]::WriteAllText($parameters.ApkPath,'controlled APK bytes')
   $parameters.ApkSha256=(Get-FileHash $parameters.ApkPath).Hash.ToLowerInvariant()
  }
  & (Join-Path $Scripts 'Invoke-AtfAndroidDisposableUser.ps1') @parameters
  & (Join-Path $Scripts 'Invoke-AtfAndroidDisposableUser.ps1') @parameters
 } catch {$caseError=$_.Exception.Message}
 $device=Get-Content $state -Raw|ConvertFrom-Json
 $receipt=Join-Path $run ('data-isolation-recovery-'+('b'*32)+'.json')
 $rows+=@{case=$case;error=$caseError;commands=$device.commands;remaining=@($device.users.id);
   readyExists=(Test-Path (Join-Path $run 'data-isolation-ready.json'));cleanupExists=(Test-Path (Join-Path $run 'data-isolation-cleanup.json'));
   receipt=$(if(Test-Path $receipt){Get-Content $receipt -Raw|ConvertFrom-Json}else{$null})}
 if($preparing){$rows[-1].journal=Get-Content $journal -Raw|ConvertFrom-Json}
}
ConvertTo-Json -InputObject @($rows) -Depth 10 -Compress
`);
  const rows=JSON.parse(execFileSync('pwsh',['-NoProfile','-NonInteractive','-File',harness,scripts,root],
    {encoding:'utf8',timeout:60000}));
  const success=rows.find(row=>row.case==='recovery');
  assert.equal(success.error,'');assert.deepEqual(success.remaining,[0,9]);
  assert.equal(success.commands.filter(cmd=>cmd==='pm remove-user 12').length,1);
  assert.equal(success.receipt.status,'RECOVERED');assert.equal(success.receipt.applicationVerdict,'NOT_RUN');
  assert.equal(success.receipt.creationLeaseId,'a'.repeat(32));assert.equal(success.receipt.recoveryLeaseId,'b'.repeat(32));
  for(const row of rows){
    assert.equal(row.readyExists,false);assert.equal(row.cleanupExists,false);
    if(['wrong-lease-run','service-unavailable'].includes(row.case)){
      assert.ok(row.error,JSON.stringify(row));assert.deepEqual(row.remaining,[0,9,12]);
      assert.ok(!row.commands.some(cmd=>/remove-user|switch-user/.test(cmd)));
    }
  }
  assert.equal(rows.find(row=>row.case==='wrong-lease-run').commands.length,0);
  assert.equal(rows.find(row=>row.case==='service-unavailable').error,'ANDROID_USER_PACKAGE_LIST_UNAVAILABLE');
  for(const row of rows.filter(row=>row.case.startsWith('prepare-'))){
    assert.match(row.error,/ORIGINAL_PREPARATION_FAILURE/);
    assert.equal(row.receipt,null);assert.match(row.journal.preparationFailure,/ORIGINAL_PREPARATION_FAILURE/);
  }
  const cleaned=rows.find(row=>row.case==='prepare-failure');
  assert.deepEqual(cleaned.remaining,[0,9]);assert.equal(cleaned.journal.state,'REMOVED');
  const pending=rows.find(row=>row.case==='prepare-cleanup-unavailable');
  assert.deepEqual(pending.remaining,[0,9,12]);assert.equal(pending.journal.state,'CREATED');
  assert.equal(pending.journal.cleanupFailure,'ANDROID_USER_LIST_UNAVAILABLE');
  console.log('Adapter case observations: '+JSON.stringify(rows));
  console.log('Recovery adapter: active lease, idempotent owned cleanup and no fabricated application PASS');
});
