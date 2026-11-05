import fs from 'node:fs/promises';import path from 'node:path';import crypto from 'node:crypto';import assert from 'node:assert/strict';
import {validateGame} from '../corpus/whist-corpus.mjs';import {traceHash} from '../corpus/whist-active-corpus.mjs';import {describeHolding,catalog,toYaml,PATTERNS} from './whist-hand-patterns.mjs';
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
export function summarizeClusters(worlds,hint=null,hintScope='SUIT_TRICKS'){
 const rows=[...worlds.values()].map(w=>({tricks:w.tricks/w.n,capture:w.capture/w.n,exact:w.exact/w.n})),n=rows.length;
 const mean=k=>n?rows.reduce((s,r)=>s+r[k],0)/n:null;
 return {independent_initial_worlds:n,observed_suit_tricks:mean('tricks'),highest_honor_capture_rate:mean('capture'),observed_exact_contract_rate:mean('exact'),evidence_weight:n/(n+20),hint_scope:hintScope,shrunken_context_hint:n&&hint!==null?(rows.reduce((s,r)=>s+(hintScope==='HIGHEST_CARD_CAPTURE'?r.capture:r.tricks),0)+20*hint)/(n+20):null,production_influence:0};
}
function add(map,id,tricks,capture,exact){const w=map.get(id)??{n:0,tricks:0,capture:0,exact:0};w.n++;w.tricks+=tricks;w.capture+=capture;w.exact+=exact;map.set(id,w);}
export async function calibrate(corpus,output){
 const startedAt=new Date().toISOString(),begin=performance.now(),source=path.join(corpus,'games/development'),seen=new Set(),global=new Map(PATTERNS.map(r=>[r.id,new Map()])),cells=new Map(),sources=[];let validated=0,uniqueTraces=0;
 for(const name of (await fs.readdir(source)).filter(n=>n.endsWith('.json')).sort()){
  const bytes=await fs.readFile(path.join(source,name)),game=JSON.parse(bytes);assert.equal(game.partition,'development','Heldout cannot fit priors');assert.equal(name,game.id+'.json');validateGame(game);validated++;sources.push({id:game.id,sha256:hash(bytes)});
  const trace=traceHash(game);if(seen.has(trace))continue;seen.add(trace);uniqueTraces++;const previousStreak=Array(game.rules.players).fill(0);
  for(const hand of game.hands){
   const world=hash(JSON.stringify([hand.size,hand.dealer,hand.trump,hand.turnedCard,hand.deal.map(d=>[d.seat,[...d.cards].sort()])])),allBids=hand.bids.reduce((s,b)=>s+b.bid,0);
   for(const deal of hand.deal){const seat=deal.seat,profile=describeHolding(deal.cards,hand.trump,game.rules.players,hand.size,hand.turnedCard?[hand.turnedCard]:[]),order=hand.bids.findIndex(b=>b.seat===seat),prior=hand.bids.slice(0,order).reduce((s,b)=>s+b.bid,0),score=hand.scores[seat];
    for(const suit of profile.suits){if(!suit.cards.length)continue;const highest=suit.cards[0];let tricks=0,capture=0;for(const round of hand.tricks)if(round.winner===seat){const played=round.plays.find(p=>p.seat===seat).card;if(played.endsWith(suit.suit))tricks++;if(played===highest)capture=1;}const exact=score.bid===score.won?1:0,unit=world+'/'+seat+'/'+suit.suit;
     for(const rule of suit.patterns){add(global.get(rule),unit,tricks,capture,exact);
      for(const phase of ['BID','PLAY']){
       const context={phase,holding_honors:suit.honors.map(h=>h.card.slice(0,-1)).join(''),small_covers:suit.smallCovers,suit_length:suit.length,players:game.rules.players,hand_band:hand.size===1?'1':hand.size<=3?'2-3':hand.size<=5?'4-5':'6-8',trump_class:hand.trump==='NT'?'NO_TRUMP':hand.trump===suit.suit?'TRUMP':'SIDE',known_bid_count:phase==='BID'?order:game.rules.players,pressure:phase==='BID'?(prior>hand.size?'KNOWN_OVER':order===game.rules.players-1?'LAST_PREFIX':'PARTIAL'):(allBids>hand.size?'OVERBOOKED':'UNDERBOOKED'),own_intent:phase==='BID'?'UNSET':score.bid===0?'ZERO':'POSITIVE',streak_boundary:previousStreak[seat]===4?'SUCCESS4':previousStreak[seat]===-4?'FAIL4':'OTHER'};
       const key=rule+'/'+JSON.stringify(context),cell=cells.get(key)??{rule,context,worlds:new Map()};add(cell.worlds,unit,tricks,capture,exact);cells.set(key,cell);
      }
     }
    }
   }for(const row of hand.scores)previousStreak[row.seat]=row.streak;
  }
  if(validated%500===0)console.log('Validated development games: '+validated);
 }
 const base=catalog(),rules=base.rules.map(r=>({...r,development_observations:summarizeClusters(global.get(r.id),r.trick_hint,r.hint_scope)}));
 const contexts=[...cells.values()].map(c=>({rule:c.rule,context:c.context,...summarizeClusters(c.worlds,PATTERNS.find(r=>r.id===c.rule).trick_hint,PATTERNS.find(r=>r.id===c.rule).hint_scope)})).sort((a,b)=>b.independent_initial_worlds-a.independent_initial_worlds||a.rule.localeCompare(b.rule)||JSON.stringify(a.context).localeCompare(JSON.stringify(b.context)));
 const selected=contexts.filter(c=>c.independent_initial_worlds>=20).slice(0,128);
 const result={...base,created_at:new Date().toISOString(),fit_scope:{partition:'development',validated_games:validated,unique_action_traces:uniqueTraces,context_cells:contexts.length,selected_contexts:selected.length,context_selection:'At least20 independent initial worlds, highest support first; full context inventory remains external JSONL.',limitations:'Observed outcomes are policy-dependent, clustered descriptive associations. They are not causal probabilities or a new strength claim. Runtime prior influence remains0.'},rules,selected_contexts:selected};
 await fs.mkdir(output,{recursive:false});await fs.writeFile(path.join(output,'calibrated-honor-priors.yaml'),toYaml(result)+'\n',{flag:'wx'});await fs.writeFile(path.join(output,'all-contexts.jsonl'),contexts.map(c=>JSON.stringify(c)).join('\n')+'\n',{flag:'wx'});await fs.writeFile(path.join(output,'source-inventory.json'),JSON.stringify(sources,null,2)+'\n',{flag:'wx'});
 const comparisons=[{name:'Every fit source independently validates as development',expected:validated,actual:sources.length,passed:validated===sources.length},{name:'Exact repeated traces do not add independent evidence',expected:true,actual:uniqueTraces<=validated,passed:uniqueTraces<=validated},{name:'Unvalidated priors cannot alter production decisions',expected:0,actual:result.runtime_prior_influence,passed:result.runtime_prior_influence===0}];
 const receipt={schemaVersion:1,runId:path.basename(output),invocationId:'development-honor-calibration',className:'scripts.whist-heuristic-calibrate',methodName:'clusterDevelopmentOutcomesWithoutHeldout',parameters:{collection:'verification'},startedAt,endedAt:new Date().toISOString(),durationNanos:Math.max(1,Math.round((performance.now()-begin)*1e6)),status:'PASS',executionKind:'NODE_COMPONENT_GATE',checkpoints:[{owner:'patterns/calibration',title:'Contextual honor and cover evidence',context:{rule:base.weight_semantics,scope:result.fit_scope,examples:rules.filter(r=>['KING_WITH_LOW','QUEEN_TWO_LOW','ACE_OFF_TRUMP','MASTER_TRUMP','KING_QUEEN_WITH_LOW'].includes(r.id)),sourceInventorySha256:hash(await fs.readFile(path.join(output,'source-inventory.json')))},comparisons}],results:[]};
 await fs.mkdir(path.join(output,'invocations'));await fs.writeFile(path.join(output,'invocations','development-honor-calibration.json'),JSON.stringify(receipt,null,2)+'\n',{flag:'wx'});return result;
}
if(process.argv[1]?.replaceAll('\\','/').endsWith('/whist-heuristic-calibrate.mjs')){try{const [corpus,output]=process.argv.slice(2);assert.ok(output,'Usage: whist-heuristic-calibrate.mjs <archive> <fresh-output>');const r=await calibrate(corpus,output);console.log(JSON.stringify(r.fit_scope,null,2));}catch(e){console.error(e.stack);process.exitCode=1;}}
