import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {test} from 'node:test';

test('user readiness excludes BOOTING and locked storage with a controlled deadline and one unlock request', t => {
  const root=fs.mkdtempSync(path.join(os.tmpdir(),'whist-user-ready-'));
  t.after(()=>fs.rmSync(root,{recursive:true,force:true}));
  const script=path.join(root,'cases.ps1');
  fs.writeFileSync(script,String.raw`
param([string]$Helper)
$ErrorActionPreference='Stop'
. $Helper
$rows=@()
foreach($case in @('ready','boot-unlock','boot-timeout','locked-timeout','unknown','read-error','unlock-error','late-ready')) {
  $ctx=[pscustomobject]@{ticks=0;calls=[Collections.Generic.List[string]]::new();states=[Collections.Generic.Queue[string]]::new()}
  $states=switch($case){'ready'{@('RUNNING_UNLOCKED')};'boot-unlock'{@('BOOTING','RUNNING_LOCKED','RUNNING_UNLOCKING','RUNNING_UNLOCKED')};'boot-timeout'{@('BOOTING')};'unknown'{@('STOPPING')};'late-ready'{@('RUNNING_UNLOCKED')};default{@('RUNNING_LOCKED')}}
  foreach($state in $states){$ctx.states.Enqueue($state)}
  $read={
    if($case-eq'read-error'){throw 'ORIGINAL_READ_FAILURE'}
    if($case-eq'late-ready'){$ctx.ticks=5}
    $state=if($ctx.states.Count-gt1){$ctx.states.Dequeue()}else{$ctx.states.Peek()}
    $ctx.calls.Add('read:'+$state);$state
  }
  $unlock={$ctx.calls.Add('unlock');if($case-eq'unlock-error'){throw 'ORIGINAL_UNLOCK_FAILURE'}}
  $pause={$ctx.calls.Add('pause');$ctx.ticks++}
  $now={([DateTimeOffset]'2026-01-01T00:00:00Z').AddSeconds($ctx.ticks)}
  try {$v=Wait-AtfAndroidUserReady -Read $read -Unlock $unlock -Pause $pause -Now $now -TimeoutSeconds 5;$rows+=@{name=$case;pass=$true;state=$v.state;calls=@($ctx.calls)}}
  catch{$rows+=@{name=$case;pass=$false;message=$_.Exception.Message;calls=@($ctx.calls)}}
}
$rows|ConvertTo-Json -Depth 4 -Compress
`);
  const rows=JSON.parse(execFileSync('pwsh',['-NoProfile','-File',script,path.resolve('scripts/AtfAndroidUserReadiness.ps1')],
    {encoding:'utf8',windowsHide:true,timeout:30000}));
  assert.equal(rows.length,8);
  assert.equal(rows[0].pass,true);assert.deepEqual(rows[0].calls,['read:RUNNING_UNLOCKED']);
  assert.equal(rows[1].pass,true);assert.equal(rows[1].state,'RUNNING_UNLOCKED');
  assert.deepEqual(rows[1].calls,['read:BOOTING','pause','read:RUNNING_LOCKED','unlock','pause','read:RUNNING_UNLOCKING','pause','read:RUNNING_UNLOCKED']);
  for(const row of [rows[2],rows[3],rows[7]]){assert.equal(row.pass,false);assert.match(row.message,/ANDROID_USER_BOOT_TIMEOUT/);}
  assert.equal(rows[3].calls.filter(x=>x==='unlock').length,1);
  assert.match(rows[4].message,/ANDROID_USER_STATE: STOPPING/);
  assert.equal(rows[5].message,'ORIGINAL_READ_FAILURE');assert.equal(rows[6].message,'ORIGINAL_UNLOCK_FAILURE');
  console.log('8 controlled Android user readiness outcomes; BOOTING is never application readiness');
});
