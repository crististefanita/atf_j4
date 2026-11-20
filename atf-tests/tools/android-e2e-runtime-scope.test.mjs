import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {test} from 'node:test';
import {digest} from './evaluate-release-rag.mjs';
import {admitWebOnlyRuntimeChanges,e2eSourcePath} from './android-e2e-provenance.mjs';
import {planAndroidE2e} from './prepare-android-e2e.mjs';
import {nativeFixture,flow,request,gameFile,setSources} from './android-e2e-provenance-fixture.mjs';

const native={path:gameFile,sha256:digest('native')};
const web={path:'atf-ui-browser/src/main/web/table.mjs',sha256:digest('old Web')};
test('the browser module remains outside the declared Android build graph',async()=>{
  for(const file of ['settings.gradle','app/build.gradle','game-core/build.gradle','atf-card-table-ui/build.gradle'])
    assert.doesNotMatch(await fs.readFile(new URL('../../whist-lab-app/'+file,import.meta.url),'utf8'),/atf-ui-browser|ro\.cardtable\.browser/,file);
});

for(const change of ['add','modify','remove'])test('Web-only '+change+' preserves the Android runtime fingerprint',()=>{
  const original=change==='add'?[native]:[native,web];
  const current=change==='remove'?[native]:[native,{...web,sha256:digest('new Web')}];
  const admission=admitWebOnlyRuntimeChanges(original,current);
  assert.equal(admission.kind,'UNCHANGED_ANDROID_RUNTIME_WITH_WEB_ONLY_INPUTS');
  assert.deepEqual(admission.changedInputs,[web.path]);
  assert.equal(admission.androidRuntimeFingerprint,admitWebOnlyRuntimeChanges([native],[native,web]).androidRuntimeFingerprint);
});

for(const file of ['whist-lab-app/atf-card-table-ui/lib/table.aar','whist-lab-app/app/build.gradle',
  'config/whist-hard-policy.yaml','atf-ui-browser-shadow/config.json'])test('a Web change cannot conceal native or unknown input '+file,()=>{
  assert.throws(()=>admitWebOnlyRuntimeChanges([native],[native,web,{path:file,sha256:digest('native change')}]),/ANDROID_E2E_RUNTIME_CHANGED/);
});

test('a browser-only inventory cannot impersonate an Android runtime',()=>{
  assert.throws(()=>admitWebOnlyRuntimeChanges([web],[{...web,sha256:digest('new')}]),/ANDROID_E2E_RUNTIME_CHANGED/);
});

test('browser package rotation preserves native reports and their original source identities',async t=>{
  const oldJar='atf-ui-browser/lib/atf-ui-browser-0.4.jar',nextJar='atf-ui-browser/lib/atf-ui-browser-0.5.jar';
  const f=await nativeFixture(t,{extraInputs:[{path:oldJar,sha256:digest('old package')}]});
  const original=await fs.readFile(path.join(f.candidate,e2eSourcePath(flow)));
  await setSources(f.candidate,f.current,f.files.filter(row=>row.path!==oldJar).concat({path:nextJar,sha256:digest('new package')}));
  const [row]=await planAndroidE2e(f.candidate,f.current,request);
  assert.equal(row.decision,'REUSE',row.reason);
  assert.deepEqual(row.provenance.runtimeAdmission.changedInputs,[oldJar,nextJar]);
  assert.equal(row.provenance.executionSourceCommit,f.manifest.source.sourceCommit);
  assert.deepEqual(await fs.readFile(path.join(f.candidate,e2eSourcePath(flow))),original);
  console.log('Web-only source admission: '+JSON.stringify(row.provenance.runtimeAdmission));
});
