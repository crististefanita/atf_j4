import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {decisionExamples} from './whist-corpus.mjs';

/** A development-only teaching set, NOT new evidence that the observed move is optimal. */
export async function buildCasebook(candidate,corpus,output){
  const names=(await fs.readdir(path.join(candidate,'invocations'))).filter(n=>n.startsWith('development-')&&n.endsWith('.json')).sort();
  const receipts=[];
  for(const name of names){const bytes=await fs.readFile(path.join(candidate,'invocations',name)),r=JSON.parse(bytes);if(r.status!=='PASS'||r.results?.length!==2||!r.invocationId.startsWith('development-'))throw new Error('Only completed development gameplay receipts may teach');receipts.push({r,sha256:crypto.createHash('sha256').update(bytes).digest('hex')});}
  if(!receipts.length)throw new Error('No development cases');
  const selected=new Map();const add=(entry,reason,size=null)=>{const item=selected.get(entry.r.invocationId)??{entry,reasons:[],sizes:[]};item.reasons.push(reason);if(size)item.sizes.push(size);selected.set(entry.r.invocationId,item);};
  const delta=e=>e.r.results.find(r=>r.kind==='after').points-e.r.results.find(r=>r.kind==='before').points;
  const families=[...new Set(receipts.map(e=>e.r.parameters.role+'/'+e.r.parameters.opponent))];
  for(const family of families){const members=receipts.filter(e=>e.r.parameters.role+'/'+e.r.parameters.opponent===family).sort((a,b)=>delta(a)-delta(b)||a.r.invocationId.localeCompare(b.r.invocationId));add(members[0],'Largest score regression in '+family);add(members.at(-1),'Largest score gain in '+family);}
  for(let size=2;size<=8;size++){const scored=receipts.map(e=>({e,m:e.r.results.find(r=>r.kind==='after').byHandSize?.[size]})).filter(x=>x.m);scored.sort((a,b)=>b.m.absoluteBidError-a.m.absoluteBidError||a.e.r.invocationId.localeCompare(b.e.r.invocationId));if(scored.length)add(scored[0].e,'High observed contract error for '+size+'-card hands',size);}
  await fs.mkdir(output,{recursive:false});const index=[];
  for(const [id,{entry,reasons,sizes}]of selected){const r=entry.r,after=r.results.find(x=>x.kind==='after');if(!/^[a-z0-9_.-]+$/i.test(after.gameId))throw new Error('Unsafe game ID');const gameBytes=await fs.readFile(path.join(corpus,'games/development',after.gameId+'.json')),game=JSON.parse(gameBytes);if(game.partition!=='development')throw new Error('Heldout cannot become training material');
    const hero=r.parameters.heroSeat;const examples=[...decisionExamples(game)].filter(d=>d.seat===hero&&d.observation.handSize>=2);const focus=sizes.length?sizes:[2,3,4,5,6,7,8];const teaching=[];
    for(const size of focus)for(const kind of ['bid','card']){const example=examples.find(d=>d.observation.handSize===size&&d.kind===kind);if(example)teaching.push(example);}
    const record={schemaVersion:1,id,promotionStatus:'REVIEW_REQUIRED',purpose:'Understand a successful or failed policy change; labels are observed actions/outcomes, never a claim of perfect play.',reasons,receiptSha256:entry.sha256,gameSha256:crypto.createHash('sha256').update(gameBytes).digest('hex'),pairedResult:r.results,publicDecisionExamples:teaching};
    await fs.writeFile(path.join(output,id+'.json'),JSON.stringify(record,null,2)+'\n',{flag:'wx'});index.push({id,reasons,scoreDelta:delta(entry),publicExamples:teaching.length});
  }
  const manifest={schemaVersion:1,partition:'development',sourceInvocations:receipts.length,selectedExamples:index.length,selection:'Best and worst per fixed family, plus high-error examples for every size 2..8. Both gains and regressions retained.',regressionPromotion:'A case enters default regression only after an independent rule/optimality expectation is established. Never assert that a policy action is correct merely because it won.',cases:index};await fs.writeFile(path.join(output,'index.json'),JSON.stringify(manifest,null,2)+'\n',{flag:'wx'});return manifest;
}
if(process.argv[1]?.replaceAll('\\','/').endsWith('/whist-casebook.mjs')){const [candidate,corpus,output]=process.argv.slice(2);try{if(!output)throw new Error('Usage: node whist-casebook.mjs <candidate> <corpus> <fresh-output>');console.log(JSON.stringify(await buildCasebook(candidate,corpus,output),null,2));}catch(error){console.error(error.stack);process.exitCode=1;}}
