import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {validateGame} from './whist-corpus.mjs';

export const featureNames = ['bias','size','noTrump','sideAce','sideKing','sideQueen','sideJack',
    'trumpAce','trumpKing','trumpQueen','trumpJack','lowTrump','coveredKing','coveredQueen',
    'singletonKing','longSideSuit','aceKing','kingQueen','maxSideLength','priorBidRatio','priorZeroCount','bidOrder'];
export function features(hand, trump, size, priorSum, priorZeros, order) {
    const suits = Array.from({length:4},()=>[]);
    for (const code of hand) suits[code/13|0].push(code%13+2);
    const x = Array(featureNames.length).fill(0); x[0]=1; x[1]=size; x[2]=trump===4?1:0;
    for (let s=0;s<4;s++) {
        const ranks=suits[s];
        for (const r of ranks) {
            if (s===trump) {if(r>=11)x[7+14-r]++;else x[11]++;}
            else if(r>=11)x[3+14-r]++;
        }
        if(s===trump)continue;
        const small=ranks.filter(r=>r<=10).length;
        if(ranks.includes(13)&&!ranks.includes(14)&&small>=1)x[12]++;
        if(ranks.includes(12)&&!ranks.includes(14)&&!ranks.includes(13)&&small>=2)x[13]++;
        if(ranks.length===1&&ranks[0]===13)x[14]++;
        x[15]+=Math.max(0,ranks.length-3);
        if(ranks.includes(14)&&ranks.includes(13))x[16]++;
        if(ranks.includes(13)&&ranks.includes(12))x[17]++;
        x[18]=Math.max(x[18],ranks.length);
    }
    x[19]=priorSum/size;x[20]=priorZeros;x[21]=order;
    return x;
}
export function predict(coefficients,x,size) {
    return Math.max(0,Math.min(size,coefficients.reduce((n,b,i)=>n+b*x[i],0)));
}
export function ridge(rows,lambda=10) {
    const n=featureNames.length, matrix=Array.from({length:n},()=>Array(n+1).fill(0));
    for(const {x,y} of rows)for(let i=0;i<n;i++) {
        for(let j=0;j<n;j++)matrix[i][j]+=x[i]*x[j];matrix[i][n]+=x[i]*y;
    }
    for(let i=1;i<n;i++)matrix[i][i]+=lambda;
    for(let c=0;c<n;c++) {
        let pivot=c;for(let r=c+1;r<n;r++)if(Math.abs(matrix[r][c])>Math.abs(matrix[pivot][c]))pivot=r;
        [matrix[c],matrix[pivot]]=[matrix[pivot],matrix[c]];
        assert.ok(Math.abs(matrix[c][c])>1e-10,'Singular training design');
        const divisor=matrix[c][c];for(let j=c;j<=n;j++)matrix[c][j]/=divisor;
        for(let r=0;r<n;r++)if(r!==c){const f=matrix[r][c];for(let j=c;j<=n;j++)matrix[r][j]-=f*matrix[c][j];}
    }
    return matrix.map(r=>r[n]);
}
const card=code=>'SHCD'.indexOf(code.at(-1))*13+({A:14,K:13,Q:12,J:11}[code.slice(0,-1)]??Number(code.slice(0,-1)))-2;
const digest=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
export async function trainForecast(corpus,output,sourceInventoryFile=null) {
    await fs.mkdir(output,{recursive:false});
    const unique=new Map(),sources=[];
    const declared=sourceInventoryFile?JSON.parse(await fs.readFile(sourceInventoryFile)):null;
    if(declared)assert.ok(Array.isArray(declared)&&declared.length&&new Set(declared.map(x=>x.id)).size===declared.length);
    const expected=declared?new Map(declared.map(x=>[x.id,x.sha256])):null;
    const names=declared?declared.map(x=>{assert.match(x.id,/^[a-zA-Z0-9_.-]+$/);return x.id+'.json';}):
        (await fs.readdir(path.join(corpus,'games/development'))).filter(n=>n.endsWith('.json'));
    for(const name of names.sort()) {
        const bytes=await fs.readFile(path.join(corpus,'games/development',name)),g=JSON.parse(bytes);
        assert.equal(g.partition,'development');assert.equal(name,g.id+'.json');
        if(expected)assert.equal(digest(bytes),expected.get(g.id),'Frozen training source changed');
        validateGame(g);
        sources.push({id:g.id,sha256:digest(bytes)});
        for(const h of g.hands) {
            if(h.size<2||h.size>8)continue;
            let prior=0,zeros=0,order=0;
            for(const bid of h.bids) {
                const hand=h.deal[bid.seat].cards.map(card).sort((a,b)=>a-b),trump=h.trump==='NT'?4:'SHCD'.indexOf(h.trump);
                // The seed split groups every policy, seat rotation and hand from the same initial deal.
                const split=Number(BigInt(g.seed)%5n)===0?'validation':'train';
                const key=JSON.stringify([g.seed,h.number,bid.seat,hand,trump,prior,zeros,order,bid.bid]);
                unique.set(key,{x:features(hand,trump,h.size,prior,zeros,order),y:bid.bid,size:h.size,split,seed:String(g.seed)});
                prior+=bid.bid;zeros+=bid.bid===0?1:0;order++;
            }
        }
    }
    const rows=[...unique.values()],train=rows.filter(r=>r.split==='train'),validation=rows.filter(r=>r.split==='validation');
    assert.ok(train.length>100&&validation.length>100);
    const coefficients=ridge(train),metrics={};
    for(const r of validation) {
        const b=metrics[r.size]??{examples:0,absoluteError:0,squaredError:0,roundedAbsoluteError:0,expertLegacyAbsoluteError:0,independentLegacyAbsoluteError:0};
        const error=predict(coefficients,r.x,r.size)-r.y;b.examples++;b.absoluteError+=Math.abs(error);b.squaredError+=error*error;metrics[r.size]=b;
        b.roundedAbsoluteError+=Math.abs(Math.round(predict(coefficients,r.x,r.size))-r.y);
        const x=r.x,trumps=x.slice(7,12).reduce((a,b)=>a+b,0),honors=x.slice(3,11).reduce((a,b)=>a+b,0),low=r.size-honors;
        const expert=Math.min(r.size,Math.round(x[3]+x[7]+.7*(x[4]+x[8])+.4*(x[5]+x[9])+.2*(x[6]+x[10])+.03*low+.2*trumps));
        const independent=Math.min(r.size,Math.round(.92*(x[3]+x[7])+.6*(x[4]+x[8])+.33*(x[5]+x[9])+.15*(x[6]+x[10])+.02*low+.19*trumps));
        b.expertLegacyAbsoluteError+=Math.abs(expert-r.y);b.independentLegacyAbsoluteError+=Math.abs(independent-r.y);
    }
    for(const b of Object.values(metrics)){b.mae=b.absoluteError/b.examples;b.rmse=Math.sqrt(b.squaredError/b.examples);b.roundedMae=b.roundedAbsoluteError/b.examples;b.expertLegacyMae=b.expertLegacyAbsoluteError/b.examples;b.independentLegacyMae=b.independentLegacyAbsoluteError/b.examples;}
    const model={schemaVersion:1,status:'RESEARCH_FORECAST_NOT_PROMOTED',featureNames,coefficients,ridgeLambda:10,
        trainingRows:train.length,validationRows:validation.length,split:'Development seeds divisible by5 validate; all remaining development seeds train. Heldout never read.',
        trainingSeedClusters:new Set(train.map(r=>r.seed)).size,validationSeedClusters:new Set(validation.map(r=>r.seed)).size,
        target:'Observed opponent bids, not achievable tricks or optimal labels. Deduplicate equal initial frame/prefix/observed bid across policy traces.',
        scope:'Cheap future-opponent bid forecast inside hypothetical worlds; known public bids remain authoritative. No private real opponent cards or outcomes are used.',
        metrics,sourceGames:sources.length};
    await fs.writeFile(path.join(output,'model.json'),JSON.stringify(model,null,2)+'\n',{flag:'wx'});
    await fs.writeFile(path.join(output,'source-inventory.json'),JSON.stringify(sources,null,2)+'\n',{flag:'wx'});
    return model;
}
if(typeof process!=='undefined'&&process.argv[1]?.replaceAll('\\','/').endsWith('/whist-bid-forecast.mjs')) {
    try {const [corpus,out,inventory]=process.argv.slice(2);assert.ok(corpus&&out,'Usage: whist-bid-forecast.mjs <corpus> <fresh-output> [frozen-source-inventory.json]');
        console.log(JSON.stringify(await trainForecast(corpus,out,inventory),null,2));
    }catch(e){console.error(e.stack);process.exitCode=1;}
}
