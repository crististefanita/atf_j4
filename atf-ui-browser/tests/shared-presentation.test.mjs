import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {test} from 'node:test';
import {withBrowser} from './fixtures.mjs';
import {verifyTrickPlayback,verifyCollectionPoses} from './trick-playback.mjs';
import {verifyCenterContinuity, verifySeatRetention} from './seat-presentation.mjs';

const web=process.env.ATF_BROWSER_WEB_ROOT??fileURLToPath(new URL('../src/main/web/',import.meta.url));
const {retainSeatFrames}=await import(pathToFileURL(path.join(web,'seat-presentation.mjs')));
const shell=fs.readFileSync(path.join(web,'table.mjs'),'utf8');
const fragment=(start,end)=>{
  const from=shell.indexOf(start),to=shell.indexOf(end,from);
  assert.ok(from>=0&&to>from,'The tested production boundary must exist');
  return shell.slice(from,to);
};

test('the delivered shared shell preserves trick structure and actor-before-card order',async()=>{
  const center=verifyCenterContinuity(shell);
  const frames=verifySeatRetention(retainSeatFrames);
  const turns=await verifyTrickPlayback(shell);
  const collection=await verifyCollectionPoses(fs.readFileSync(path.join(web,'collection.mjs'),'utf8'));
  await assert.rejects(()=>verifyTrickPlayback(shell.replaceAll('await animation.done;','void animation.done;')),
    /faces stay visible|next view cannot clear|only after/,'The independent hold oracle must reject an early clear');
  console.log('Shared trick observations: '+JSON.stringify({center,frames,turns,collection}));
});

test('the real Web score renderer matches native text roles and preserves actions at narrow and enlarged-text profiles',async t=>{
  const executable=process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
  assert.ok(fs.existsSync(executable),'Supply ATF_BROWSER_EXECUTABLE for the Chromium component renderer');
  const work=fs.mkdtempSync(path.join(os.tmpdir(),'score-adapter-'));
  t.after(()=>fs.promises.rm(work,{recursive:true,force:true,maxRetries:15,retryDelay:200}));
  const root=fileURLToPath(new URL('../../',import.meta.url));
  const contract=path.join(root,'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/ScoreTypography.java');
  const probe=fileURLToPath(new URL('./ScoreTypographyProbe.java',import.meta.url));
  const compile=['-proc:none','-encoding','UTF-8','-d',work];
  if(process.env.ATF_BROWSER_RUNTIME_JAR)compile.push('-cp',process.env.ATF_BROWSER_RUNTIME_JAR);
  else compile.push(contract);
  execFileSync('javac',[...compile,probe],{windowsHide:true,timeout:30000});
  const classpath=[work,process.env.ATF_BROWSER_RUNTIME_JAR].filter(Boolean).join(path.delimiter);
  const tokens=JSON.parse(execFileSync('java',['-cp',classpath,'ScoreTypographyProbe'],{encoding:'utf8',windowsHide:true,timeout:30000}));
  assert.equal(tokens['--score-text-title'],'1.5625rem','Native score title remains 25 logical sp');
  assert.equal(tokens['--score-text-quiet'],'0.875rem','Native quiet actions retain their 14 sp role');
  const css=fs.readFileSync(path.join(web,'table.css'),'utf8');
  const score=fragment('function whistScore(', 'function writeSeatFact(');
  const grid=fragment('function scoreGrid(', 'function showHistory(');
  const profile=fs.readFileSync(path.join(web,'profile.mjs'),'utf8');
  const dialogs=profile.slice(profile.indexOf('export function createProfileDialogs('),profile.indexOf('export function deviceProfile(')).replace('export function','function');
  const observations=[];
  await withBrowser(executable,path.join(work,'profile'),async({send,evaluate,version})=>{
    await send('Page.enable');
    for(const language of ['ro','en'])for(const width of [280,414,760])for(const font of [16,24,32]){
      await send('Emulation.setDeviceMetricsOverride',{width,height:896,deviceScaleFactor:1,mobile:true});
      const labels=language==='ro'?{scoringDetails:'Calculul punctajului',history:'Istoric punctaj',mainMenu:'Meniul principal',next:'Mâna următoare',score:'Scorul partidei'}
        :{scoringDetails:'Scoring details',history:'Score history',mainMenu:'Main menu',next:'Next hand',score:'Match score'};
      const observed=await evaluate(`(()=>{
        document.documentElement.style.fontSize=${JSON.stringify(font+'px')};
        for(const [name,value] of Object.entries(${JSON.stringify(tokens)}))document.documentElement.style.setProperty(name,value);
        document.head.replaceChildren(Object.assign(document.createElement('meta'),{name:'viewport',content:'width=device-width, initial-scale=1'}),
          Object.assign(document.createElement('style'),{textContent:${JSON.stringify(css)}}));
        document.body.replaceChildren();
        const stage=document.createElement('main');stage.id='table';stage.dataset.surface='whist';
        // Fixture palette only; this contract measures typography and callbacks, not native color parity.
        stage.style.setProperty('--whist-accent','#306f4e');stage.style.setProperty('--whist-surface','#fffef8');
        stage.style.setProperty('--stage-width',${JSON.stringify(width+'px')});document.body.append(stage);
        const labels=${JSON.stringify(labels)},clicks=[];
        function element(tag,text='',type=''){const node=document.createElement(tag);node.className=type;node.textContent=text;return node;}
        function button(label,action,type=''){const node=element('button',label,type);node.onclick=action;return node;}
        const automation={set:value=>clicks.push(['automatic',value])},errorBox=element('p'),refreshAutomationGate=()=>{};
        const copy=(key,fallback)=>labels[key]??fallback;
        const menu=()=>clicks.push(['home']),showHistory=()=>clicks.push(['history']);
        ${dialogs}
        const realDialogs=createProfileDialogs(element,button,()=>{},copy);
        const profileDialogs={message:(...values)=>{clicks.push(['details',...values]);realDialogs.message(...values);}};
        const control=move=>button(move.label,()=>clicks.push([move.key]),'primary');
        ${grid}
        ${score}
        whistScore({heading:'HAND 8',prompt:'Completed',score:{title:labels.score,
          table:{headings:['Player','Bid','Won','Hand','Total'],rows:Array.from({length:6},(_,i)=>['Player '+i,'1','1','6',['120','-12345','500','1000','-5','0'][i]])}},
          scoreDetails:{summary:'8 / 19',rules:['Exact contract'],winner:'Winner',awards:'Series +10'},history:{},
          actions:[{key:'next',label:labels.next}]});
        const size=node=>parseFloat(getComputedStyle(node).fontSize);
        const buttons=[...stage.querySelectorAll('button')];
        const result={heading:size(stage.querySelector('h1')),summary:size(stage.querySelector('.whist-score-status')),
          hand:size(stage.querySelector('.whist-score-hand')),winner:size(stage.querySelector('.whist-score-winner')),
          award:size(stage.querySelector('.whist-score-awards')),column:size(stage.querySelector('th')),value:size(stage.querySelector('td')),
          buttons:buttons.map(node=>({label:node.textContent,size:size(node),quiet:node.classList.contains('quiet'),
            height:node.getBoundingClientRect().height,overflow:node.scrollWidth>node.clientWidth+1})),
          extraWidth:stage.scrollWidth-stage.clientWidth,
          columns:[...stage.querySelectorAll('.whist-score-grid :is(th,td):not(:first-child)')].map(cell=>{
            const range=document.createRange();range.selectNodeContents(cell);const css=getComputedStyle(cell);
            return {text:cell.textContent,ink:range.getBoundingClientRect().width,
              available:cell.clientWidth-parseFloat(css.paddingLeft)-parseFloat(css.paddingRight)};})};
        const scroll=stage.querySelector('.score-table-scroll');scroll.focus();scroll.scrollLeft=scroll.scrollWidth;
        const edge=scroll.getBoundingClientRect(),last=scroll.querySelector('tbody tr td:last-child').getBoundingClientRect();
        result.lastColumnVisible=last.left>=edge.left-1&&last.right<=edge.right+1;
        result.tableFocusable=document.activeElement===scroll;scroll.scrollLeft=0;
        buttons.forEach(node=>node.click());result.clicks=clicks;
        result.detailsFont=size(realDialogs.dialog);result.detailsTitle=size(realDialogs.dialog.querySelector('h2'));realDialogs.dialog.close();
        realDialogs.liveScore({summary:'8 / 19',liveTitle:labels.score},scoreGrid({headings:['Player','Total'],rows:[['Player 0','120']]},true),()=>{});
        result.liveFont=size(realDialogs.dialog);result.liveSummary=size(realDialogs.dialog.querySelector('.whist-score-status'));
        realDialogs.dialog.close();realDialogs.message('Generic rules',['Unchanged']);
        result.genericFont=size(realDialogs.dialog);realDialogs.dialog.close();return result;
      })()`);
      assert.equal(observed.heading,font*1.5625);
      assert.equal(observed.summary,font*.8125);
      assert.equal(observed.hand,font*.75);
      assert.equal(observed.winner,font*1.125);
      assert.equal(observed.award,font*.875);
      assert.equal(observed.column,font*.6875);
      assert.equal(observed.value,font*.8125);
      assert.equal(observed.detailsFont,font);
      assert.equal(observed.detailsTitle,font*1.25);
      assert.equal(observed.liveFont,font);
      assert.equal(observed.liveSummary,font*.75);
      assert.equal(observed.genericFont,font,'Unrelated dialogs retain their existing text size');
      assert.deepEqual(observed.buttons.map(row=>row.label),[labels.scoringDetails,labels.next,labels.history,labels.mainMenu]);
      for(const row of observed.buttons){assert.equal(row.size,font*(row.quiet ? .875 : 1));assert.ok(row.height>=48);assert.equal(row.overflow,false,row.label);}
      assert.deepEqual(observed.clicks,[['automatic',false],['details',labels.scoringDetails,['Exact contract'],'game-score-rules'],['next'],['history'],['home']]);
      assert.ok(observed.extraWidth<=1,JSON.stringify({contract:'Score page horizontal viewport',language,width,font,...observed}));
      assert.equal(observed.lastColumnVisible,true,'The final score column must remain reachable in its own region');
      assert.equal(observed.tableFocusable,true,'The overflowing score region must accept keyboard focus');
      for(const column of observed.columns)assert.ok(column.ink<=column.available+1,
        JSON.stringify({contract:'Readable score columns without overlapping ink',language,width,font,column}));
      observations.push({language,width,font,version,...observed});
      if(process.env.ATF_COMPONENT_CAPTURE_DIR&&width===414&&[16,32].includes(font)){
        fs.mkdirSync(process.env.ATF_COMPONENT_CAPTURE_DIR,{recursive:true});
        const png=await send('Page.captureScreenshot',{format:'png'});
        fs.writeFileSync(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,`score-${language}-${font}.png`),Buffer.from(png.data,'base64'));
        if(font===32){
          await evaluate("(()=>{const region=document.querySelector('.score-table-scroll');region.scrollLeft=region.scrollWidth;})()");
          const right=await send('Page.captureScreenshot',{format:'png'});
          fs.writeFileSync(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,`score-${language}-${font}-right.png`),Buffer.from(right.data,'base64'));
        }
      }
    }
    console.log('Score text adapter observations: '+JSON.stringify(observations));
  });
});
