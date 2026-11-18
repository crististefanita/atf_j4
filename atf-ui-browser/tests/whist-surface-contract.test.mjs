import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {test} from 'node:test';
import {textScale} from '../src/main/web/viewport.mjs';
import {collectionFrame} from '../src/main/web/collection.mjs';

test('CSS text scale is logical and is independent of desktop device-pixel ratio', () => {
  for (const [text, expected] of [['16px',1],['24px',1.5],['32px',2],['8px',.5],['NaN',1],['0px',1],['Infinity',1]])
    assert.equal(textScale(text),expected,text);
});

test('the entrypoint and imported UI modules share a cache revision', () => {
  const web=fileURLToPath(new URL('../src/main/web/',import.meta.url));
  const html=fs.readFileSync(path.join(web,'index.html'),'utf8'),table=fs.readFileSync(path.join(web,'table.mjs'),'utf8');
  const revision=/table\.mjs\?v=([0-9.]+)/.exec(html)?.[1];assert.ok(revision);
  assert.ok(html.includes('table.css?v='+revision));
  for(const file of ['profile.mjs','viewport.mjs'])assert.ok(table.includes('./'+file+'?v='+revision),file);
});

test('production Web and native portable adapters share geometry, card marks and gesture transitions', t => {
  const root=fileURLToPath(new URL('../../',import.meta.url));
  const work=fs.mkdtempSync(path.join(os.tmpdir(),'whist-surface-'));
  t.after(()=>fs.rmSync(work,{recursive:true,force:true}));
  const roots=['atf-ui-browser/src/main/java','whist-lab-app/atf-card-table-ui/src/main/java',
    'whist-lab-app/card-table-names/src/main/java','whist-lab-app/game-core/src/main/java',
    'whist-lab-app/app/src/main/java','atf-ui-browser/whist/src/main/java'].map(dir=>path.join(root,dir));
  const fixture=path.join(root,'atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java');
  const sources=[fixture,...fs.readdirSync(roots[0]+'/ro/cardtable/browser').filter(file=>file.endsWith('.java'))
    .map(file=>roots[0]+'/ro/cardtable/browser/'+file)];
  const args=['-proc:none','-encoding','UTF-8','-sourcepath',roots.join(path.delimiter),'-d',work,...sources];
  const quote=value=>'"'+value.replaceAll('\\','/')+'"';
  const argFile=path.join(work,'javac.args');fs.writeFileSync(argFile,args.map(quote).join('\n'));
  execFileSync('javac',['-J-Xmx384m','@'+argFile],{encoding:'utf8',timeout:60000,windowsHide:true});
  const text=execFileSync('java',['-Xmx256m','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8','-cp',work,'ro.cardtable.browser.WhistSurfaceContract',
    path.join(root,'whist-lab-app/atf-card-table-ui/src/test/resources/hand-feedback.tsv')],
    {encoding:'utf8',timeout:60000,windowsHide:true});
  const result=JSON.parse(text);assert.equal(result.status,'PASS');assert.equal(result.cases.length,4);
  assert.equal(result.cases[1].faces,52);assert.equal(result.cases[2].commands,8);
  const curve=result.cases[3];assert.equal(curve.samples.length,11);
  for(let i=0;i<curve.samples.length;i++){
    const browser=collectionFrame(100,-60,curve.samples[i]),native=curve.frames[i];
    for(const [web,apk] of [['x','translationX'],['y','translationY'],['scale','scale'],['opacity','alpha']])
      assert.ok(Math.abs(browser[web]-native[apk])<.0001,JSON.stringify({i,web,apk,browser,native}));
  }
  console.log('Whist adapter observations: '+JSON.stringify(result));
});
