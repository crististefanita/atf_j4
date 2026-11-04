import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {validateGame} from './whist-corpus.mjs';
import {firstDivergence} from './whist-r13-diagnose.mjs';

const hash=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const emptyBucket=()=>({lost:0,gained:0,over:0,under:0,lostWithChangedOwnBid:0,lostWithSameOwnBid:0});
const counts=()=>({pairs:0,firstBidChanges:0,firstCardChanges:0,identicalGames:0,newlyLostExact:0,newlyGainedExact:0,lostWithChangedOwnBid:0,lostWithSameOwnBid:0,overshoots:0,undershoots:0,byHandSize:Object.fromEntries(Array.from({length:7},(_,i)=>[i+2,emptyBucket()]))});

/** Reconstruct only information available BEFORE this action. Other seats' holdings are never copied. */
export function decisionContextAt(game,index,observer){
    assert.ok(Number.isInteger(index)&&index>=0,'Action index');
    const n=game.rules.players;assert.ok(Number.isInteger(observer)&&observer>=0&&observer<n,'Observer seat');
    let at=0,totals=Array(n).fill(0),streaks=Array(n).fill(0);
    for(const hand of game.hands){
        const bids=Array(n).fill(null),won=Array(n).fill(0),remaining=Array(n).fill(hand.size),history=[];
        const own=[...hand.deal.find(p=>p.seat===observer).cards];let table=[];
        const view=(actor,kind)=>({observerSeat:observer,actorSeat:actor,kind,handNumber:hand.number,handSize:hand.size,dealer:hand.dealer,trump:hand.trump,turnedCard:hand.turnedCard,
            observerOwnHand:[...own],players:Array.from({length:n},(_,seat)=>({seat,remaining:remaining[seat],bid:bids[seat],won:won[seat],remainingNeed:bids[seat]===null?null:bids[seat]-won[seat],total:totals[seat],streak:streaks[seat]})),
            table:table.map(p=>({...p})),history:history.map(p=>({...p})),historyIncludesCurrentTable:true,seriesEnabled:game.rules.seriesEnabled,seriesBonus:game.rules.seriesBonus});
        for(const bid of hand.bids){if(at++===index)return view(bid.seat,'bid');bids[bid.seat]=bid.bid;}
        for(const trick of hand.tricks){table=[];for(const play of trick.plays){
            if(at++===index)return view(play.seat,'card');
            remaining[play.seat]--;if(play.seat===observer){const i=own.indexOf(play.card);assert.ok(i>=0,'Previously played own card');own.splice(i,1);}
            history.push({...play});table.push({...play});
        }won[trick.winner]++;}
        totals=hand.scores.map(s=>s.total);streaks=hand.scores.map(s=>s.streak);
    }
    throw new Error('Action index outside complete trace');
}

/** Pure descriptive contrast. This reducer does not claim legality; the file-level entry point replays both games. */
export function summarizePair(before,after,heroSeat,metadata={}){
    assert.equal(before.hands.length,after.hands.length,'Matching hand schedules');
    const first=firstDivergence(before,after),summary=counts(),casebook=[];summary.pairs=1;
    if(first){assert.equal(first.before.seat,heroSeat,'First changed action must be the selected policy');assert.equal(first.after.seat,heroSeat,'First changed action actor');summary[first.after.kind==='bid'?'firstBidChanges':'firstCardChanges']++;}
    else summary.identicalGames++;
    let firstDifference=null;
    if(first){
        const beforeContext=decisionContextAt(before,first.index,heroSeat),afterContext=decisionContextAt(after,first.index,heroSeat);
        assert.deepEqual(beforeContext,afterContext,'First differing action must have the same available decision context');
        firstDifference={index:first.index,handNumber:first.before.hand,size:first.before.size,actorSeat:heroSeat,kind:first.before.kind,beforeAction:first.before.value,afterAction:first.after.value,context:beforeContext};
    }
    for(let h=0;h<before.hands.length;h++){
        const b=before.hands[h],a=after.hands[h];assert.equal(b.number,a.number);assert.equal(b.size,a.size);if(a.size<2||a.size>8)continue;
        const old=b.scores[heroSeat],current=a.scores[heroSeat],bucket=summary.byHandSize[a.size],wasExact=old.bid===old.won,isExact=current.bid===current.won;
        if(wasExact!==isExact)assert.ok(first,'Contract outcomes cannot change under identical action traces');
        if(wasExact&&!isExact){
            summary.newlyLostExact++;bucket.lost++;const changed=current.bid!==old.bid;
            summary[changed?'lostWithChangedOwnBid':'lostWithSameOwnBid']++;bucket[changed?'lostWithChangedOwnBid':'lostWithSameOwnBid']++;
            const over=current.won>current.bid;summary[over?'overshoots':'undershoots']++;bucket[over?'over':'under']++;
            if(!casebook.some(c=>c.lostContract.size===a.size))casebook.push({seed:String(metadata.seed??before.seed),invocationId:metadata.invocationId??null,gameIds:{before:before.id,after:after.id},observerSeat:heroSeat,
                firstDifference,lostContract:{handNumber:a.number,size:a.size,before:{bid:old.bid,won:old.won},after:{bid:current.bid,won:current.won},ownBidChanged:changed,direction:over?'OVERSHOOT':'UNDERSHOOT'},
                interpretation:firstDifference?.handNumber===a.number?'First action difference and later loss occur within the same hand; this does not prove the changed action is wrong.':'First action difference and later loss are separate observations in different hands; later own-bid changes may be downstream effects.',
                visibility:'Only public facts and the observing seat\'s own remaining cards before the first differing action; no actual opponent holdings, future plays or optimal-action label.'});
        }
        if(!wasExact&&isExact){summary.newlyGainedExact++;bucket.gained++;}
    }
    return {...summary,firstDifference,casebook};
}

function metrics(game,seat){
    const byHandSize={};let exact=0;
    for(const hand of game.hands){const row=hand.scores[seat],bucket=byHandSize[hand.size]??={hands:0,points:0,exact:0,absoluteBidError:0};bucket.hands++;bucket.points+=row.base+row.series;bucket.exact+=Number(row.bid===row.won);bucket.absoluteBidError+=Math.abs(row.bid-row.won);exact+=Number(row.bid===row.won);}
    const best=Math.max(...game.finalScores),ties=game.finalScores.filter(p=>p===best).length;
    return {points:game.finalScores[seat],winShare:game.finalScores[seat]===best?1/ties:0,exact,totals:game.finalScores,byHandSize};
}

/** Every included pair must close successfully and independently replay. Not a replacement for the protocol's promotion gate. */
export async function diagnosePolicies(folder,corpus,partition='development'){
    assert.ok(['development','heldout','reference'].includes(partition),'Known corpus partition');
    const source=JSON.parse(await fs.readFile(path.join(folder,'candidate-source.json'),'utf8'));
    const summary=counts(),casebook=[],inventory=[],ids=new Set(),selections=new Set();let gameAudits=0;
    const files=(await fs.readdir(path.join(folder,'invocations'))).filter(n=>n.startsWith(partition+'-')&&n.endsWith('.json')).sort();
    assert.ok(files.length,'At least one closed pair is required');
    for(const name of files){
        const bytes=await fs.readFile(path.join(folder,'invocations',name)),receipt=JSON.parse(bytes);
        assert.equal(receipt.status,'PASS','Closed passing rule-audit receipt required');assert.ok(receipt.durationNanos>0&&Number.isFinite(Date.parse(receipt.startedAt))&&Number.isFinite(Date.parse(receipt.endedAt)),'Closed execution timing');
        assert.equal(name,receipt.invocationId+'.json','Receipt filename identity');assert.ok(!ids.has(receipt.invocationId),'Duplicate invocation');ids.add(receipt.invocationId);
        assert.deepEqual(receipt.sourceHashes,source,'Receipt policy source');assert.ok(receipt.checkpoints.length&&receipt.checkpoints.every(c=>c.comparisons.length&&c.comparisons.every(a=>a.passed===true)),'Captured assertions must agree with PASS');
        const seat=receipt.parameters.heroSeat,key=[receipt.parameters.role,receipt.parameters.opponent,receipt.parameters.seed,seat].join('/');assert.ok(!selections.has(key),'Duplicate paired selection');selections.add(key);
        assert.equal(receipt.results.length,2);assert.deepEqual(receipt.results.map(r=>r.kind).sort(),['after','before']);
        const games=[],gameInventory=[];
        for(const kind of ['before','after']){
            const result=receipt.results.find(r=>r.kind===kind);assert.match(result.gameId,/^[a-zA-Z0-9_.-]+$/,'Safe game identity');
            const gameBytes=await fs.readFile(path.join(corpus,'games',partition,result.gameId+'.json')),game=JSON.parse(gameBytes);validateGame(game);gameAudits++;
            assert.equal(game.id,result.gameId);assert.equal(game.partition,partition);assert.equal(game.seed,String(receipt.parameters.seed));assert.ok(Number.isInteger(seat)&&seat>=0&&seat<game.rules.players,'Selected hero seat');
            assert.equal(game.source.heroSeat,seat);assert.equal(game.source.invocationId,receipt.invocationId,'Game invocation identity');assert.deepEqual(game.source.sourceHashes,source,'Game policy source');
            for(const [metric,value]of Object.entries(metrics(game,seat)))assert.deepEqual(result[metric],value,'Captured '+metric+' must agree with replay');
            games.push(game);gameInventory.push({kind,id:game.id,sha256:hash(gameBytes)});
        }
        assert.deepEqual(games[0].rules,games[1].rules,'Paired rules');assert.deepEqual(games[0].hands.map(h=>[h.dealer,h.trump,h.turnedCard,h.deal]),games[1].hands.map(h=>[h.dealer,h.trump,h.turnedCard,h.deal]),'Every paired deal must match');
        const pair=summarizePair(...games,seat,{seed:receipt.parameters.seed,invocationId:receipt.invocationId});
        for(const name of Object.keys(summary))if(name!=='byHandSize')summary[name]+=pair[name];
        for(const [size,bucket]of Object.entries(summary.byHandSize))for(const name of Object.keys(bucket))bucket[name]+=pair.byHandSize[size][name];
        for(const entry of pair.casebook)if(!casebook.some(c=>c.lostContract.size===entry.lostContract.size))casebook.push(entry);
        inventory.push({invocationId:receipt.invocationId,receiptSha256:hash(bytes),games:gameInventory});
    }
    // Completed composition manifests expose the declared pair count when present; raw folders remain usable for diagnostics.
    const provenance=await fs.readFile(path.join(folder,'batch-provenance.json'),'utf8').then(JSON.parse).catch(error=>{if(error.code==='ENOENT')return null;throw error;});
    if(provenance){assert.equal(provenance.phase,partition);assert.equal(provenance.completedPairs,summary.pairs,'All composed pairs must be diagnosed');assert.deepEqual(provenance.sourceHashes,source);}
    return {schemaVersion:1,status:'MEASURED_DIAGNOSTIC',partition,...summary,independentGameAudits:gameAudits,casebook,
        casebookSelection:'First newly lost exact contract per hand size2..8 in sorted invocation order; at most7 examples, no severity or optimal-action selection.',
        scope:'Descriptive paired terminal outcomes, not causal proof or an action-quality oracle. Earlier changed play may cause later changed bids. Consumed heldout games are diagnostic only and must not be reused as fresh evidence or fitting data.',
        sourceIntegrity:{policyHashes:source,receipts:inventory}};
}

if(process.argv[1]?.replaceAll('\\','/').endsWith('/whist-policy-diagnostics.mjs')){
    try{const[folder,corpus,partition,output]=process.argv.slice(2);assert.ok(output,'Usage: whist-policy-diagnostics.mjs <paired-folder> <corpus> <partition> <fresh-output.json>');const result=await diagnosePolicies(folder,corpus,partition);await fs.writeFile(output,JSON.stringify(result,null,2)+'\n',{flag:'wx'});console.log(JSON.stringify({status:result.status,pairs:result.pairs,newlyLostExact:result.newlyLostExact,newlyGainedExact:result.newlyGainedExact,overshoots:result.overshoots,undershoots:result.undershoots,casebook:result.casebook.length,output},null,2));}
    catch(error){console.error(error.stack);process.exitCode=1;}
}
