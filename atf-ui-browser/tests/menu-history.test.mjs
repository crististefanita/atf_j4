import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
import test from 'node:test';
import {withBrowser} from './fixtures.mjs';
import {verifyMenuEntries,verifyHistoryMeasurements} from './menu-history-contract.mjs';

const project=fileURLToPath(new URL('../../',import.meta.url));
const web=process.env.ATF_BROWSER_WEB_ROOT??path.join(project,'atf-ui-browser/src/main/web');
const {menuFromEntries}=await import(pathToFileURL(path.join(web,'table-menu.mjs')));
const {applyHistoryAppearance}=await import(pathToFileURL(path.join(web,'records.mjs')));

test('invalid history appearance is rejected atomically and empty policy restores the baseline',()=>{
  const values=new Map([['unrelated','keep']]);const target={style:{setProperty:(k,v)=>values.set(k,v),removeProperty:k=>values.delete(k)}};
  const valid={'--history-heading-weight':'400','--history-divider-width':'2px','--history-divider-color':'#71846c'};
  applyHistoryAppearance(target,valid);const before=[...values];
  for(const invalid of [{...valid,'--history-heading-weight':'900'},{...valid,'--history-divider-width':'0px'},
    {...valid,'--history-divider-color':'transparent'},{...valid,unexpected:'value'}]){
    assert.throws(()=>applyHistoryAppearance(target,invalid),TypeError);assert.deepEqual([...values],before);
  }
  applyHistoryAppearance(target,{});assert.deepEqual([...values],[['unrelated','keep']]);
  console.log('History rejection observations: invalid styles fail atomically; baseline and unrelated properties remain intact.');
});

test('menu adapters preserve native labels order callbacks and reject ambiguous plans',()=>{
  for(const labels of [['Game history','Results','Main menu','Stop auto-play','Name'],
    ['Istoricul jocului','Rezultate','Meniul principal','Oprește jocul automat','Nume']]){
    const ids=['score-history','score-sheet','home','stop-auto','settings'];
    verifyMenuEntries(menuFromEntries,ids.map((id,i)=>({id,label:labels[i]})));
  }
  console.log('Menu adapter observations: native order and labels preserved; callbacks isolated; invalid plans refused.');
});

test('history appearance matches the native policy at narrow and enlarged text profiles',async t=>{
  const work=fs.mkdtempSync(path.join(os.tmpdir(),'menu-history-'));
  assert.ok(work.startsWith(path.resolve(os.tmpdir())+path.sep));
  t.after(()=>fs.promises.rm(work,{recursive:true,force:true,maxRetries:15,retryDelay:200}));
  const probe=fileURLToPath(new URL('./MenuHistoryProbe.java',import.meta.url));
  const compile=['-proc:none','-encoding','UTF-8','-d',work];
  if(process.env.ATF_BROWSER_RUNTIME_JAR)compile.push('-cp',process.env.ATF_BROWSER_RUNTIME_JAR);
  else compile.push(path.join(project,'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/HistoryAppearance.java'),
    path.join(project,'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/ScoreGridStyle.java'));
  execFileSync('javac',[...compile,probe],{windowsHide:true,timeout:30000});
  const cp=[work,process.env.ATF_BROWSER_RUNTIME_JAR].filter(Boolean).join(path.delimiter);
  const policies=JSON.parse(execFileSync('java',['-cp',cp,'MenuHistoryProbe'],{encoding:'utf8',windowsHide:true}));
  const records=fs.readFileSync(path.join(web,'records.mjs'),'utf8');
  const grid=records.slice(records.indexOf('export function historyGrid('),records.indexOf('export function rankingPanel(')).replaceAll('export function','function');
  const css=fs.readFileSync(path.join(web,'table.css'),'utf8');const observations=[];
  await withBrowser(process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',path.join(work,'browser'),async({send,evaluate})=>{
    await send('Page.enable');
    for(const language of ['ro','en'])for(const players of [3,4,5,6])for(const width of [280,414,760])for(const root of [16,24,32]){
      await send('Emulation.setDeviceMetricsOverride',{width,height:896,deviceScaleFactor:1,mobile:true});
      const labels=language==='ro'?['Mână','Jucător']:['Hand','Player'];
      const measured=await evaluate(`(()=>{
        document.head.replaceChildren(Object.assign(document.createElement('meta'),{name:'viewport',content:'width=device-width, initial-scale=1'}),Object.assign(document.createElement('style'),{textContent:${JSON.stringify(css)}}));
        document.documentElement.style.fontSize='${root}px';document.body.replaceChildren();
        function element(tag,text='',type=''){const node=document.createElement(tag);node.textContent=text;node.className=type;return node;}
        ${grid}
        const wrapper=element('div');wrapper.style.cssText='width:${width}px;max-width:100vw;min-width:0;overflow:auto';document.body.append(wrapper);
        const players=Array.from({length:${players}},(_,i)=>${JSON.stringify(labels[1])}+' '+i);
        const data={players,cardsLabel:${JSON.stringify(labels[0])},bidLabel:'Δ',pointsLabel:'Σ',appearance:${JSON.stringify(policies.regular)},
          rows:[{cards:1,status:'Complete',cells:players.map(()=>({bid:'−20',total:'−120',description:'Confirmed score',missed:false,series:0}))}]};
        const table=historyGrid(data,element);wrapper.append(table);
        const names=[...table.querySelectorAll('thead tr:first-child th')];
        const groups=[...table.querySelectorAll('.history-seat')];
        wrapper.scrollLeft=wrapper.scrollWidth;const edge=wrapper.getBoundingClientRect(),last=names.at(-1).getBoundingClientRect();
        const hex=color=>'#'+color.match(/\\d+/g).slice(0,3).map(n=>Number(n).toString(16).padStart(2,'0')).join('');
        const result={weights:names.map(n=>getComputedStyle(n).fontWeight),dividers:groups.map(n=>parseFloat(getComputedStyle(n).borderLeftWidth)),
          colors:groups.map(n=>hex(getComputedStyle(n).borderLeftColor)),lastPlayerVisible:last.right<=edge.right+1&&last.left>=edge.left-1};
        wrapper.scrollLeft=0;const legacy=historyGrid({...data,appearance:{}},element);wrapper.append(legacy);
        result.legacyWeight=getComputedStyle(legacy.querySelector('thead th')).fontWeight;
        const first=table.querySelector('tbody tr').textContent;result.signedValues=first.includes('−120')&&first.includes('−20');legacy.remove();
        result.viewport={width:innerWidth,scale:visualViewport.scale};return result;
      })()`);
      verifyHistoryMeasurements(measured,policies.regular);assert.equal(measured.legacyWeight,'700');assert.equal(measured.signedValues,true);
      assert.deepEqual(measured.viewport,{width,scale:1});observations.push({language,players,width,root,...measured});
      if(process.env.ATF_COMPONENT_CAPTURE_DIR&&players===4&&width===414&&[16,32].includes(root)){
        fs.mkdirSync(process.env.ATF_COMPONENT_CAPTURE_DIR,{recursive:true});const png=await send('Page.captureScreenshot',{format:'png'});
        fs.writeFileSync(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,`history-${language}-${root}.png`),Buffer.from(png.data,'base64'));
      }
    }
  });
  console.log('History appearance observations: '+JSON.stringify(observations));
});
