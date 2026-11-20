import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import test from 'node:test';

test('new library versions use two parts while immutable legacy coordinates remain readable',()=>{
  const module=fileURLToPath(new URL('../../scripts/CardTableLibraryVersion.psm1',import.meta.url)).replaceAll("'","''");
  function check(version,existing=false){
    return execFileSync('pwsh',['-NoLogo','-NoProfile','-Command',
      `$ErrorActionPreference='Stop';Import-Module '${module}';Assert-CardTableLibraryVersion -Version '${version}' -Existing $${existing}`],
      {encoding:'utf8',windowsHide:true,stdio:['ignore','pipe','pipe']});
  }
  for(const value of ['0.2','0.3','0.17'])assert.doesNotThrow(()=>check(value));
  for(const value of ['0.1.24','0.2.27']){assert.throws(()=>check(value));assert.doesNotThrow(()=>check(value,true));}
  for(const value of ['1.2','0.03','0.2.0.1','../0.2'])assert.throws(()=>check(value,true));
  console.log('Library version observations: new 0.x accepted; legacy new packages and malformed coordinates refused.');
});
