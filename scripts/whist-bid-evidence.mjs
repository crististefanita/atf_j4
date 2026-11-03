import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {validateGame} from './whist-corpus.mjs';

const hash = b => crypto.createHash('sha256').update(b).digest('hex');
const rank = c => ({A:14,K:13,Q:12,J:11}[c.slice(0,-1)] ?? Number(c.slice(0,-1)));
export const FEATURE_NAMES = ['bias','size','trumps','masterTrumps','sideAces','sideKings','sideQueens','sideJacks',
    'coveredKings','coveredQueens','trumpVoidInteraction','sideVoids','sideAceKing','longestSuit',
    'noTrump','position','priorBids','overbookedPrefix','sideAceDensity','coveredKingDensity','trumpDensity'];

/** Bid-time features only: later played cards must not promote an honor retrospectively. */
export function bidFeatures(cards,size,trump,turned,position,prefix) {
    assert.ok(size>=2&&size<=8&&cards.length===size&&new Set(cards).size===size);
    assert.ok(['S','H','C','D','NT'].includes(trump)&&position>=0&&position<4&&prefix>=0);
    assert.ok(!turned||!cards.includes(turned));
    const x=Array(FEATURE_NAMES.length).fill(0);x[0]=1;x[1]=size/8;
    const own=new Set(cards),counts=[...'SHCD'].map(s=>cards.filter(c=>c.at(-1)===s).length);
    let sideVoids=0,aces=0,kings=0;
    for(const s of 'SHCD') {
        const holding=cards.filter(c=>c.at(-1)===s),low=holding.filter(c=>rank(c)<=10).length;
        if(s===trump) {
            x[2]=holding.length/8;
            for(const c of holding){let outsideHigher=false;for(let r=rank(c)+1;r<=14;r++){
                const higher=String(({11:'J',12:'Q',13:'K',14:'A'})[r]??r)+s;
                if(!own.has(higher)&&higher!==turned)outsideHigher=true;
            }if(!outsideHigher)x[3]+=1/8;}
        } else {
            if(!holding.length)sideVoids++;
            for(const [value,index]of [[14,4],[13,5],[12,6],[11,7]])x[index]+=holding.filter(c=>rank(c)===value).length/8;
            if(own.has('K'+s)&&!own.has('A'+s)&&low>=1){x[8]+=1/8;kings++;}
            if(own.has('Q'+s)&&!own.has('A'+s)&&!own.has('K'+s)&&low>=2)x[9]+=1/8;
            if(own.has('A'+s)&&own.has('K'+s))x[12]+=1/4;
            if(own.has('A'+s))aces++;
        }
    }
    x[10]=sideVoids*x[2]/3;x[11]=sideVoids/(trump==='NT'?4:3);x[13]=Math.max(...counts)/8;
    x[14]=trump==='NT'?1:0;x[15]=position/3;x[16]=prefix/(3*size);x[17]=Math.max(0,prefix-size)/(2*size);
    x[18]=aces*size/64;x[19]=kings*size/64;x[20]=x[2]*size/8;return x;
}

export function legalBids(size,position,prefix) {
    return Array.from({length:size+1},(_,i)=>i).filter(b=>position!==3||b+prefix!==size);
}
export function bidDistribution(model,cards,size,trump,turned,position,prefix) {
    const x=bidFeatures(cards,size,trump,turned,position,prefix);
    const mean=Math.max(0,Math.min(size,8*x.reduce((s,v,i)=>s+v*model.coefficients[i],0)));
    const allowed=legalBids(size,position,prefix),sigma=model.sigma[size];
    const logs=allowed.map(b=>-0.5*((b-mean)/sigma)**2),top=Math.max(...logs),mass=logs.map(l=>Math.exp(l-top)),sum=mass.reduce((a,b)=>a+b,0);
    const result=Array(size+1).fill(0);
    allowed.forEach((b,i)=>result[b]=(1-model.strength)/allowed.length+model.strength*mass[i]/sum);
    return result;
}

function fit(rows) {
    const total=rows.reduce((s,r)=>s+r.weight,0),lambda=0.002*total;
    const coefficients=Array(FEATURE_NAMES.length).fill(0);
    coefficients[0]=rows.reduce((s,r)=>s+r.weight*r.bid/8,0)/total;
    const residual=rows.map(r=>r.bid/8-coefficients[0]),free=new Set([0,1,14,15,16,17]);
    for(let sweep=0;sweep<500;sweep++)for(let j=0;j<coefficients.length;j++){
        let numerator=0,denominator=j===0?0:lambda;
        rows.forEach((r,i)=>{numerator+=r.weight*r.x[j]*(residual[i]+r.x[j]*coefficients[j]);denominator+=r.weight*r.x[j]*r.x[j];});
        let next=denominator?numerator/denominator:0;if(!free.has(j))next=Math.max(0,next);
        const delta=next-coefficients[j];rows.forEach((r,i)=>{residual[i]-=r.x[j]*delta;});coefficients[j]=next;
    }
    const sigma=Array(9).fill(0.85);
    for(let size=2;size<=8;size++){
        let weightedSquare=3*0.85**2,weight=3;
        for(const r of rows)if(r.size===size){const mean=Math.max(0,Math.min(size,8*r.x.reduce((s,v,i)=>s+v*coefficients[i],0)));weightedSquare+=r.weight*(r.bid-mean)**2;weight+=r.weight;}
        sigma[size]=Math.max(0.55,Math.sqrt(weightedSquare/weight));
    }
    return {coefficients,sigma,strength:0.25,lambda,nonnegativeCardFeatures:true};
}
function measure(model,rows) {
    let weight=0,nll=0,uniform=0,correct=0;
    for(const r of rows){const p=bidDistribution(model,r.cards,r.size,r.trump,r.turned,r.position,r.prefix),allowed=legalBids(r.size,r.position,r.prefix);
        nll-=r.weight*Math.log(p[r.bid]);uniform+=r.weight*Math.log(allowed.length);weight+=r.weight;
        if(p.indexOf(Math.max(...p))===r.bid)correct+=r.weight;
    }
    return {rows:rows.length,seedWeight:weight,negativeLogLikelihood:nll/weight,uniformNll:uniform/weight,relativeNllImprovement:(uniform-nll)/uniform,modeAccuracy:correct/weight};
}

export async function collectBidExamples(pointerFile) {
    const pointer=JSON.parse(await fs.readFile(pointerFile,'utf8')),manifestFile=path.join(pointer.folder,'manifest.json'),manifestBytes=await fs.readFile(manifestFile);
    assert.equal(hash(manifestBytes),pointer.manifestSha256);const manifest=JSON.parse(manifestBytes);
    assert.equal(manifest.partition,'development');assert.equal(manifest.activeGames,manifest.games.length);
    const byIdentity=new Map(),inventory=[];
    for(const entry of manifest.games){const bytes=await fs.readFile(path.join(pointer.folder,entry.file));assert.equal(hash(bytes),entry.sha256);
        const game=JSON.parse(bytes);assert.equal(game.partition,'development');validateGame(game);inventory.push({id:game.id,sha256:hash(bytes)});
        for(const hand of game.hands)if(hand.size>=2){let prefix=0;
            for(let position=0;position<4;position++){const observed=hand.bids[position],cards=[...hand.deal[observed.seat].cards].sort();
                const row={seed:game.seed,size:hand.size,cards,trump:hand.trump,turned:hand.turnedCard,position,prefix,bid:observed.bid,
                    observerCards:[...hand.deal[(observed.seat+1)%4].cards].sort()};
                row.x=bidFeatures(cards,row.size,row.trump,row.turned,position,prefix);
                assert.ok(legalBids(row.size,position,prefix).includes(row.bid));
                const key=JSON.stringify([game.seed,hand.number,observed.seat,cards,hand.trump,hand.turnedCard,position,prefix,row.bid]);
                if(!byIdentity.has(key))byIdentity.set(key,row);prefix+=observed.bid;
            }
        }
    }
    const rows=[...byIdentity.values()],counts=new Map();for(const r of rows)counts.set(r.seed,(counts.get(r.seed)??0)+1);
    for(const r of rows)r.weight=1/counts.get(r.seed);
    const validationSeed=s=>crypto.createHash('sha256').update(s).digest()[0]%5===0;
    const training=rows.filter(r=>!validationSeed(r.seed)),validation=rows.filter(r=>validationSeed(r.seed));
    assert.ok(new Set(training.map(r=>r.seed)).size>=20&&new Set(validation.map(r=>r.seed)).size>=5,'Need enough whole seed clusters');
    return {pointer,inventory,training,validation};
}

export async function learnBidEvidence(pointerFile,output) {
    const {pointer,inventory,training,validation}=await collectBidExamples(pointerFile);
    const model=fit(training);const result={schemaVersion:1,id:'observed-bid-evidence-r15',featureNames:FEATURE_NAMES,...model,
        sourceManifestSha256:pointer.manifestSha256,sourceGames:inventory,split:{trainingSeeds:[...new Set(training.map(r=>r.seed))].sort(),validationSeeds:[...new Set(validation.map(r=>r.seed))].sort(),rule:'SHA256(seed) byte0 modulo5 equals0 selects validation; complete seed clusters never split.'},
        training:measure(model,training),validation:measure(model,validation),interpretation:'Observed bidding behavior, not optimal bids or a causal honor valuation. Runtime hand hypotheses are not actual private hands.',
        runtimeLimits:{originalContextOnly:true,softPriorFloor:0.75,worldRatioRange:[0.25,4],essFractionMinimum:0.5,firstBidUniformFallback:true}};
    result.predictiveGate=result.validation.relativeNllImprovement>=0.02?'PASS':'FAIL';
    await fs.mkdir(output,{recursive:false});await fs.writeFile(path.join(output,'model.json'),JSON.stringify(result,null,2)+'\n',{flag:'wx'});
    await fs.writeFile(path.join(output,'validation-examples.json'),JSON.stringify(validation.slice(0,24),null,2)+'\n',{flag:'wx'});
    return result;
}
if(process.argv[1]?.replaceAll('\\','/').endsWith('/whist-bid-evidence.mjs')){
    try{const [pointer,output]=process.argv.slice(2);assert.ok(output,'Usage: whist-bid-evidence.mjs <active-pointer.json> <fresh-output>');
        const m=await learnBidEvidence(pointer,output);console.log(JSON.stringify({predictiveGate:m.predictiveGate,trainingSeeds:m.split.trainingSeeds.length,validationSeeds:m.split.validationSeeds.length,training:m.training,validation:m.validation},null,2));if(m.predictiveGate!=='PASS')process.exitCode=2;
    }catch(e){console.error(e.stack);process.exitCode=1;}
}
