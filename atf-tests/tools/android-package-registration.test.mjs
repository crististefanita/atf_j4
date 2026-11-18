import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {test} from 'node:test';

test('package registration reconciles observed bytes after timeout and preserves the original failure', t => {
  const root=fs.mkdtempSync(path.join(os.tmpdir(),'whist-registration-'));
  t.after(()=>fs.rmSync(root,{recursive:true,force:true}));
  const script=path.join(root,'cases.ps1');
  fs.writeFileSync(script,String.raw`
param([string]$Helper)
$ErrorActionPreference='Stop'
. $Helper
$rows=@()
foreach($case in @('success','timeout-completed','timeout-missing','timeout-wrong-hash','success-wrong-hash','observation-error')) {
  $calls=[Collections.Generic.List[string]]::new()
  $install={ $calls.Add('install'); if($case.StartsWith('timeout')){throw 'ORIGINAL_INSTALL_TIMEOUT'} }
  $observe={
    $calls.Add('observe')
    if($case-eq'timeout-missing'-or$case-eq'observation-error'){throw 'OBSERVATION_UNAVAILABLE'}
    [pscustomobject]@{path='/data/app/test/base.apk';sha256=$(if($case.EndsWith('wrong-hash')){'b'*64}else{'a'*64})}
  }
  try {
    $v=Invoke-AtfVerifiedPackageRegistration -Install $install -Observe $observe -ExpectedHash ('a'*64)
    $rows+=@{name=$case;passed=$true;recovered=$v.commandRecovered;hash=$v.sha256;calls=@($calls)}
  } catch {
    $rows+=@{name=$case;passed=$false;message=$_.Exception.Message;observation=$_.Exception.Data['registrationObservation'];calls=@($calls)}
  }
}
$calls=[Collections.Generic.List[string]]::new()
try {Invoke-AtfVerifiedPackageRegistration -Install {$calls.Add('install')} -Observe {$calls.Add('observe')} -ExpectedHash 'bad'} catch {
  $rows+=@{name='invalid-input';passed=$false;message=$_.Exception.Message;calls=@($calls)}
}
$rows|ConvertTo-Json -Depth 5 -Compress
`);
  const helper=path.resolve('scripts/AtfAndroidPackageRegistration.ps1');
  const rows=JSON.parse(execFileSync('pwsh',['-NoProfile','-File',script,helper],{encoding:'utf8',windowsHide:true,timeout:30000}));
  assert.equal(rows.length,7);
  for(const row of rows.slice(0,6)) assert.deepEqual(row.calls,['install','observe'],row.name);
  assert.equal(rows[0].passed,true);assert.equal(rows[0].recovered,false);
  assert.equal(rows[1].passed,true);assert.equal(rows[1].recovered,true);assert.equal(rows[1].hash,'a'.repeat(64));
  for(const row of rows.slice(2,4)) {assert.equal(row.passed,false);assert.equal(row.message,'ORIGINAL_INSTALL_TIMEOUT');assert.ok(row.observation);}
  assert.match(rows[4].message,/OBSERVED_IDENTITY/);assert.equal(rows[5].message,'OBSERVATION_UNAVAILABLE');
  assert.equal(rows[6].message,'ANDROID_PACKAGE_EXPECTED_HASH');assert.deepEqual(rows[6].calls,[]);
  console.log('7 controlled package-registration outcomes: one install, independent bytes, original cause retained');
});
