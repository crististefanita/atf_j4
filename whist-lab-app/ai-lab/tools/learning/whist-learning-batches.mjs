import fs from 'node:fs/promises';import path from 'node:path';import crypto from 'node:crypto';import assert from 'node:assert/strict';import {validateGame} from '../corpus/whist-corpus.mjs';
const digest=b=>crypto.createHash('sha256').update(b).digest('hex');
const stable=o=>JSON.stringify(Object.entries(o??{}).sort(([a],[b])=>a.localeCompare(b)));
export const key=p=>[p.role,p.opponent,Number(p.seed),p.heroSeat].join('/');
export const batchIntegrityId=(protocol,phase,output)=>'batch-integrity-'+phase+((protocol?.version??0)>=15?'-'+path.basename(output):'');
export function expectedCases(protocol,phase){const range=protocol[phase];assert.ok(range&&Number.isSafeInteger(range.firstSeed)&&Number.isSafeInteger(range.seeds)&&range.seeds>0&&range.seeds<=1000);const result=new Set();const roles=protocol.roles??['expert','player'];assert.ok(Array.isArray(roles)&&roles.length>0&&new Set(roles).size===roles.length&&roles.every(r=>['expert','player'].includes(r)),'Declared roles must be a nonempty unique expert/player list');for(const role of roles)for(const opponent of protocol.opponents)for(let seed=range.firstSeed;seed<range.firstSeed+range.seeds;seed++)for(let seat=0;seat<4;seat++)result.add(key({role,opponent,seed,heroSeat:seat}));return result;}
/** A completed failed assertion is never erased by a successful retry. Only unclosed captures may resume. */
export function closedRecord(r,expected,hashes){
 if(['FAIL','ERROR'].includes(r.status))throw new Error('Completed failing invocation must remain visible; no PASS retry merge');
 if(r.status!=='PASS')return false;
 assert.ok(expected.has(key(r.parameters)),'Unexpected paired case');assert.equal(stable(r.sourceHashes),stable(hashes),'Policy changed across execution batches');
 assert.ok(r.durationNanos>0&&r.startedAt&&r.endedAt,'Closed execution timing required');
 assert.equal(r.results.length,2);assert.deepEqual(r.results.map(x=>x.kind).sort(),['after','before']);
 assert.ok(r.checkpoints.length>0&&r.checkpoints.every(c=>c.comparisons.length>0&&c.comparisons.every(a=>a.passed===true)),'Captured assertions contradict PASS');return true;
}
async function writeFresh(file,bytes){const handle=await fs.open(file,'wx');try{await handle.writeFile(bytes);await handle.sync();}finally{await handle.close();}}
export async function composeBatches(sources,corpus,protocol,phase,output){
 const startedAt=new Date().toISOString(),begin=performance.now(),expected=expectedCases(protocol,phase),selected=new Map(),ignored=[],inventory=[];let hashes=null,gameAudits=0;
 for(const folder of sources){
  const sourceHashes=JSON.parse(await fs.readFile(path.join(folder,'candidate-source.json')));if(hashes)assert.equal(stable(hashes),stable(sourceHashes),'Policy changed across batches');else hashes=sourceHashes;
  for(const name of (await fs.readdir(path.join(folder,'invocations'))).filter(n=>n.startsWith(phase+'-')&&n.endsWith('.json')).sort()){
   const bytes=await fs.readFile(path.join(folder,'invocations',name));let r;try{r=JSON.parse(bytes);}catch(e){ignored.push({file:name,source:path.basename(folder),sha256:digest(bytes),reason:'CORRUPT_JSON'});continue;}
   if(!closedRecord(r,expected,hashes)){ignored.push({file:name,source:path.basename(folder),sha256:digest(bytes),reason:'UNCLOSED_'+r.status});continue;}
   assert.equal(name,r.invocationId+'.json','Invocation file identity');const identity=key(r.parameters);assert.ok(!selected.has(identity),'Duplicate completed pair; do not double count reruns');
   const gameHashes=[];let deal=null;
   for(const result of r.results){
    assert.match(result.gameId,/^[a-zA-Z0-9_.-]+$/);const gameBytes=await fs.readFile(path.join(corpus,'games',phase,result.gameId+'.json')),g=JSON.parse(gameBytes);validateGame(g);gameAudits++;
    assert.equal(g.partition,phase);assert.equal(g.id,result.gameId);assert.equal(g.seed,String(r.parameters.seed));assert.equal(g.source.heroSeat,r.parameters.heroSeat);assert.equal(stable(g.source.sourceHashes),stable(hashes));
    assert.deepEqual(result.totals,g.finalScores);assert.equal(result.points,g.finalScores[r.parameters.heroSeat]);
    const best=Math.max(...g.finalScores),ties=g.finalScores.filter(x=>x===best).length;assert.equal(result.winShare,result.points===best?1/ties:0);
    const next=digest(JSON.stringify(g.hands.map(h=>[h.dealer,h.trump,h.turnedCard,h.deal])));if(deal)assert.equal(next,deal,'Before/after must share every deal');else deal=next;
    gameHashes.push({id:g.id,sha256:digest(gameBytes)});
   }
   selected.set(identity,{name,r,bytes,source:folder,sha256:digest(bytes),games:gameHashes});
  }
 }
 assert.equal(selected.size,expected.size,'Missing completed pairs; output must not shrink the declared protocol');
 await fs.mkdir(output,{recursive:false});await fs.mkdir(path.join(output,'invocations'));await fs.mkdir(path.join(output,'quality-invocations'));
 for(const [identity,e]of [...selected].sort(([a],[b])=>a.localeCompare(b))){await writeFresh(path.join(output,'invocations',e.name),e.bytes);inventory.push({identity,invocationId:e.r.invocationId,source:path.basename(e.source),file:'invocations/'+e.name,sha256:e.sha256,games:e.games});}
 await writeFresh(path.join(output,'candidate-source.json'),JSON.stringify(hashes,null,2)+'\n');
 const manifest={schemaVersion:1,phase,completedPairs:selected.size,independentGameAudits:gameAudits,sourceHashes:hashes,ignoredUnclosedAttempts:ignored,selection:'Every declared seed/seat/role/opponent exactly once. PASS bytes copied unchanged only after independent game and source validation. Interrupted attempts remain in original storage.',receipts:inventory};
 await writeFresh(path.join(output,'batch-provenance.json'),JSON.stringify(manifest,null,2)+'\n');
 const comparisons=[{name:'Every declared paired case completed exactly once',expected:expected.size,actual:selected.size,passed:selected.size===expected.size},{name:'Every before and after game independently replays',expected:2*expected.size,actual:gameAudits,passed:gameAudits===2*expected.size}];
 const receipt={schemaVersion:1,runId:path.basename(output),invocationId:batchIntegrityId(protocol,phase,output),className:'scripts.whist-learning-batches',methodName:'composeOnlyClosedSourceBoundPairs',parameters:{collection:'regression',phase},startedAt,endedAt:new Date().toISOString(),durationNanos:Math.max(1,Math.round((performance.now()-begin)*1e6)),status:'PASS',executionKind:'NODE_COMPONENT_GATE',checkpoints:[{owner:'batches/'+phase,title:'Completed evidence after interrupted attempts',context:{rule:manifest.selection,completedPairs:selected.size,sourceBatches:sources.map(s=>path.basename(s)),ignoredAttempts:ignored.map(e=>({id:e.file,source:e.source,reason:e.reason})),scope:'Original interrupted attempts were not PASS. Only explicitly completed, unchanged captures are included in statistical evaluation.'},comparisons}],results:[]};
 await writeFresh(path.join(output,'quality-invocations','batch-integrity.json'),JSON.stringify(receipt,null,2)+'\n');return manifest;
}
if(process.argv[1]?.replaceAll('\\','/').endsWith('/whist-learning-batches.mjs')){try{const [corpus,protocolFile,phase,output,...sources]=process.argv.slice(2);assert.ok(sources.length>0,'Usage: whist-learning-batches.mjs <corpus> <protocol.json> <phase> <fresh-output> <source>...');const m=await composeBatches(sources,corpus,JSON.parse(await fs.readFile(protocolFile)),phase,output);console.log(JSON.stringify({completedPairs:m.completedPairs,independentGameAudits:m.independentGameAudits,ignored:m.ignoredUnclosedAttempts},null,2));}catch(e){console.error(e.stack);process.exitCode=1;}}
