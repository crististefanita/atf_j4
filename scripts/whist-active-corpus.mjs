import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {validateGame} from './whist-corpus.mjs';
const hash=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
function check(ok,message){if(!ok)throw new Error(message);}
export function traceHash(game){
 return hash(JSON.stringify([game.rules,game.hands.map(h=>[h.size,h.dealer,h.trump,h.turnedCard,h.deal,h.bids,h.tricks,h.scores]),game.finalScores]));
}
function role(controller){
 if(['point_greedy','point_covers','safe_greedy','safe_covers'].includes(controller))return 'independent';
 return /independent/i.test(controller)?'independent':/expert/i.test(controller)?'expert':'other';
}
export function lessons(game){
 check(game.partition==='development','Active learning accepts development only');
 const tokens=new Set();
 for(const hand of game.hands){
  if(hand.size<2||hand.size>8)continue;
  const bidSum=hand.bids.reduce((s,b)=>s+b.bid,0);
  for(const score of hand.scores){
   const seat=score.seat,r=role(game.players[seat].controller),delta=score.won-score.bid;
   const outcome=delta===0?'exact':delta<0?'under':'over';
   const shape=hand.deal[seat].cards.reduce((counts,c)=>(counts['SHCD'.indexOf(c.at(-1))]++,counts),[0,0,0,0]);
   const trump=hand.trump==='NT'?'NT':shape['SHCD'.indexOf(hand.trump)]===0?'void':shape['SHCD'.indexOf(hand.trump)]===1?'one':'many';
   const bid=score.bid===0?'zero':score.bid===hand.size?'all':score.bid===1?'one':'middle';
   const order=hand.bids.findIndex(b=>b.seat===seat);
   tokens.add([r,hand.size,outcome,bid,trump].join('/'));
   tokens.add([r,hand.size,'auction',order,bidSum<hand.size?'underbook':'overbook',outcome].join('/'));
   if(Math.abs(delta)>=2)tokens.add([r,hand.size,'large-error',outcome].join('/'));
   if(score.series!==0)tokens.add([r,hand.size,'series',Math.sign(score.series),outcome].join('/'));
   if(shape.filter(n=>n===0).length>=2)tokens.add([r,hand.size,'two-voids',outcome].join('/'));
  }
 }
 return [...tokens].sort();
}
/** Rarity cover followed by bounded one-for-one exchanges. No change to protected origins or cap. */
export function selectGames(entries,{maxGames=100,protectedIds=[]}={}){
 check(Number.isSafeInteger(maxGames)&&maxGames>0,'Positive integral maxGames required');
 const byId=new Map(entries.map(e=>[e.id,e]));for(const id of protectedIds)check(byId.has(id),'Missing protected regression origin '+id);
 const groups=new Map();for(const e of entries){const group=groups.get(e.traceHash)??[];group.push(e);groups.set(e.traceHash,group);}
 const candidates=[...groups.values()].map(group=>{group.sort((a,b)=>a.id.localeCompare(b.id));return {...group[0],aliases:group.map(x=>x.id),protected:group.some(x=>protectedIds.includes(x.id))};}).sort((a,b)=>a.traceHash.localeCompare(b.traceHash));
 const frequency=new Map();for(const e of candidates)for(const token of e.lessons)frequency.set(token,(frequency.get(token)??0)+1);
 const remaining=new Set(candidates),covered=new Set(),selected=[];
 const add=e=>{remaining.delete(e);selected.push({...e,newLessons:e.lessons.filter(t=>!covered.has(t))});for(const t of e.lessons)covered.add(t);};
 for(const e of candidates)if(e.protected)add(e);
 check(selected.length<=maxGames,'Protected games exceed active limit');
 while(selected.length<maxGames){
  let best=null,value=0;
  for(const e of remaining){const score=e.lessons.reduce((s,t)=>s+(covered.has(t)?0:1/Math.sqrt(frequency.get(t))),0);if(score>value+1e-12){value=score;best=e;}}
  if(!best)break;add(best);
 }
 const optimized=selected.map(e=>({...e})),counts=new Map();
 for(const e of optimized)for(const token of e.lessons)counts.set(token,(counts.get(token)??0)+1);
 let exchangeImprovements=0;
 while(counts.size<frequency.size){
  const chosen=new Set(optimized.map(e=>e.traceHash));
  const prospects=candidates.filter(e=>!chosen.has(e.traceHash)).map(e=>({entry:e,tokens:new Set(e.lessons),
   added:e.lessons.filter(t=>!counts.has(t)).length})).filter(e=>e.added>0);
  let best=null,gain=0;
  for(let i=0;i<optimized.length;i++)if(!optimized[i].protected){
   const lost=optimized[i].lessons.filter(t=>counts.get(t)===1);
   for(const p of prospects){const delta=p.added-lost.filter(t=>!p.tokens.has(t)).length;
    if(delta>gain){gain=delta;best={index:i,entry:p.entry};}}
  }
  if(!best)break;
  for(const t of optimized[best.index].lessons){const n=counts.get(t)-1;if(n)counts.set(t,n);else counts.delete(t);}
  for(const t of best.entry.lessons)counts.set(t,(counts.get(t)??0)+1);
  optimized[best.index]=best.entry;exchangeImprovements++;
 }
 const finalCovered=new Set();
 const finalSelection=optimized.map(e=>{const result={...e,newLessons:e.lessons.filter(t=>!finalCovered.has(t))};
  for(const t of e.lessons)finalCovered.add(t);return result;});
 return {selected:finalSelection,exchangeImprovements,uniqueTraces:candidates.length,lessonCount:frequency.size,
  coveredLessons:finalCovered.size,missingLessons:[...frequency.keys()].filter(t=>!finalCovered.has(t)).sort()};
}
export async function curate(archive,output,options={}){
 const began=performance.now();archive=path.resolve(archive);output=path.resolve(output);
 check(output!==archive&&!output.startsWith(archive+path.sep),'Active corpus must be separate from immutable archive');
 await fs.mkdir(output,{recursive:false});await fs.mkdir(path.join(output,'games'));
 const directory=path.join(archive,'games','development'),entries=[];
 for(const name of (await fs.readdir(directory)).filter(n=>n.endsWith('.json')).sort()){
  const bytes=await fs.readFile(path.join(directory,name)),g=JSON.parse(bytes);
  check(g.partition==='development','Partition mismatch '+name);check(name===g.id+'.json','Filename identity mismatch');
  validateGame(g);
  entries.push({id:g.id,file:'games/development/'+name,sha256:hash(bytes),bytes:bytes.length,traceHash:traceHash(g),lessons:lessons(g)});
  if(entries.length%250===0)console.log('Validated development archive: '+entries.length);
 }
 const selection=selectGames(entries,options),active=[];
 for(const e of selection.selected){
  const bytes=await fs.readFile(path.join(archive,e.file));check(hash(bytes)===e.sha256,'Archive changed during curation '+e.id);
  const file='games/'+e.id+'.json';await fs.writeFile(path.join(output,file),bytes,{flag:'wx'});
  active.push({...e,file,archiveFile:e.file});
 }
 const selectedHashes=new Set(active.map(e=>e.traceHash));
 const manifest={schemaVersion:1,partition:'development',purpose:'Small teaching set of observed outcomes; not optimal-action labels or evaluation evidence.',archive,archiveMutation:'NONE: redundant records stay in archival storage; ordinary learning reads this explicit active manifest only.',maxGames:options.maxGames??100,sourceGames:entries.length,sourceBytes:entries.reduce((n,e)=>n+e.bytes,0),activeGames:active.length,activeBytes:active.reduce((n,e)=>n+e.bytes,0),uniqueTraces:selection.uniqueTraces,lessonCount:selection.lessonCount,coveredLessons:selection.coveredLessons,missingLessons:selection.missingLessons,exchangeImprovements:selection.exchangeImprovements,heldout:'Never read or copied by curator',protectedIds:[...(options.protectedIds??[])],selection:'Protected regression origins, deterministic rarity-weighted set cover, then strictly improving one-for-one exchanges over2..8-card lessons. Protected traces and the cap never change; remaining gaps are disclosed.',elapsedMilliseconds:performance.now()-began,games:active};
 await fs.writeFile(path.join(output,'manifest.json'),JSON.stringify(manifest,null,2)+'\n',{flag:'wx'});
 const inventory=entries.map(e=>({id:e.id,file:e.file,sha256:e.sha256,traceHash:e.traceHash,disposition:active.some(a=>a.id===e.id)?'ACTIVE_COPY':selectedHashes.has(e.traceHash)?'ARCHIVE_DUPLICATE':'ARCHIVE_REDUNDANT_OR_CAP',reason:selectedHashes.has(e.traceHash)?'Exact trace represented in active set':'No additional selected lesson within declared cap'}));
 await fs.writeFile(path.join(output,'archive-inventory.json'),JSON.stringify(inventory,null,2)+'\n',{flag:'wx'});
 return manifest;
}
export async function loadActiveGame(folder,manifest,entry){
 check(manifest.schemaVersion===1&&manifest.partition==='development','Development active manifest required');
 check(manifest.games.length===manifest.activeGames,'Active inventory count mismatch');
 check(manifest.games.some(e=>e.id===entry.id&&e.sha256===entry.sha256&&e.file===entry.file),'Game absent from active manifest');
 check(entry.file==='games/'+entry.id+'.json'&&/^[a-zA-Z0-9_.-]+$/.test(entry.id),'Unsafe active game path');
 const bytes=await fs.readFile(path.join(folder,entry.file));check(hash(bytes)===entry.sha256,'Active game hash mismatch');
 const game=JSON.parse(bytes);check(game.id===entry.id&&game.partition==='development','Active identity/partition mismatch');validateGame(game);return game;
}
export async function resolveActivePointer(pointerFile){
 const pointer=JSON.parse(await fs.readFile(pointerFile));check(pointer.schemaVersion===1&&pointer.partition==='development','Development active pointer required');
 check(typeof pointer.folder==='string'&&path.isAbsolute(pointer.folder),'Absolute active folder required');
 const bytes=await fs.readFile(path.join(pointer.folder,'manifest.json'));check(hash(bytes)===pointer.manifestSha256,'Active manifest hash mismatch');
 const manifest=JSON.parse(bytes);check(manifest.partition==='development','Heldout cannot be an active learning pointer');return {folder:pointer.folder,manifest};
}
if(process.argv[1]?.replaceAll('\\','/').endsWith('/whist-active-corpus.mjs')){
 try{const [command,archive,output,limit,protectedFile]=process.argv.slice(2);if(command==='curate'&&archive&&output){const protectedIds=protectedFile?JSON.parse(await fs.readFile(protectedFile)):[];check(Array.isArray(protectedIds)&&protectedIds.every(x=>typeof x==='string'),'Protected IDs must be a JSON string array');const m=await curate(archive,output,{maxGames:Number(limit??100),protectedIds});console.log(JSON.stringify({...m,games:undefined},null,2));}
 else if((command==='verify'&&archive)||command==='verify-current'){const config=command==='verify-current'?JSON.parse(await fs.readFile(archive??new URL('../config/learning-corpus.json',import.meta.url))):null;const resolved=config?await resolveActivePointer(config.activePointer):{folder:archive,manifest:JSON.parse(await fs.readFile(path.join(archive,'manifest.json')))};const m=resolved.manifest;for(const e of m.games)await loadActiveGame(resolved.folder,m,e);console.log(JSON.stringify({status:'PASS',activeGames:m.activeGames,activeBytes:m.activeBytes,lessons:m.coveredLessons,missing:m.missingLessons.length}));}
 else throw new Error('Usage: whist-active-corpus.mjs curate <archive> <fresh-active> [maxGames] [protected-ids.json] | verify <active>');}catch(e){console.error(e.stack);process.exitCode=1;}
}
