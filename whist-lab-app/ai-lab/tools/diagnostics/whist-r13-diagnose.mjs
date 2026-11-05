import fs from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';

function actions(game){return game.hands.flatMap(h=>[
    ...h.bids.map(b=>({hand:h.number,size:h.size,kind:'bid',seat:b.seat,value:b.bid})),
    ...h.tricks.flatMap(t=>t.plays.map(p=>({hand:h.number,size:h.size,kind:'card',seat:p.seat,value:p.card})))
]);}
export function firstDivergence(before,after){const a=actions(before),b=actions(after);assert.equal(a.length,b.length);
    for(let i=0;i<a.length;i++)if(JSON.stringify(a[i])!==JSON.stringify(b[i]))return {index:i,before:a[i],after:b[i]};return null;}

export async function diagnosePressure(root,corpus,output){
    const batches=[];
    for(const [name,folder]of [['initial-development','development-complete'],['blend-development','development-blend-complete'],['blend-fresh','evaluation-complete']]){
        const result={name,pairs:0,firstBidChanges:0,firstCardChanges:0,identicalGames:0,newlyLostExact:0,newlyGainedExact:0,
            lostWithChangedOwnBid:0,lostWithSameOwnBid:0,overshoots:0,undershoots:0,bySize:{}};
        for(const f of(await fs.readdir(path.join(root,folder,'invocations'))).filter(f=>f.endsWith('.json'))){
            const r=JSON.parse(await fs.readFile(path.join(root,folder,'invocations',f),'utf8'));assert.equal(r.status,'PASS');
            const games=[];for(const kind of ['before','after']){const id=r.results.find(x=>x.kind===kind).gameId;games.push(JSON.parse(await fs.readFile(path.join(corpus,'games',r.parameters.seed>=112000?'heldout':'development',id+'.json'),'utf8')));}
            const first=firstDivergence(...games);if(first){assert.equal(first.after.seat,r.parameters.heroSeat,'First divergence must originate in changed policy');result[first.after.kind==='bid'?'firstBidChanges':'firstCardChanges']++;}else result.identicalGames++;
            result.pairs++;const seat=r.parameters.heroSeat;
            for(let h=0;h<games[0].hands.length;h++){const before=games[0].hands[h],after=games[1].hands[h];if(after.size<2)continue;
                const b=before.scores[seat],a=after.scores[seat],bucket=result.bySize[after.size]??{lost:0,gained:0,over:0,under:0};
                if(b.bid===b.won&&a.bid!==a.won){result.newlyLostExact++;bucket.lost++;if(a.bid!==b.bid)result.lostWithChangedOwnBid++;else result.lostWithSameOwnBid++;
                    if(a.won>a.bid){result.overshoots++;bucket.over++;}else{result.undershoots++;bucket.under++;}}
                if(b.bid!==b.won&&a.bid===a.won){result.newlyGainedExact++;bucket.gained++;}result.bySize[after.size]=bucket;
            }
        }batches.push(result);
    }
    const value={status:'MEASURED_DIAGNOSTIC',batches,scope:'Consumed R13 data are diagnostic only. No model fitting on fresh evaluation. First divergence isolates the first direct policy effect; later bid changes can be consequences of earlier changed play. Terminal outcomes do not label a bid optimal.'};
    await fs.writeFile(output,JSON.stringify(value,null,2)+'\n',{flag:'wx'});return value;
}
if(process.argv[1]?.replaceAll('\\','/').endsWith('/whist-r13-diagnose.mjs')){
    try{const[root,corpus,out]=process.argv.slice(2);assert.ok(out);console.log(JSON.stringify(await diagnosePressure(root,corpus,out),null,2));}
    catch(e){console.error(e.stack);process.exitCode=1;}
}
