import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import {collectBidExamples,bidDistribution,legalBids} from './whist-bid-evidence.mjs';

const deck=[...'SHCD'].flatMap(s=>[7,8,9,10,'J','Q','K','A'].map(r=>r+s));
function generator(text){let state=crypto.createHash('sha256').update(text).digest().readUInt32LE();return()=>{state=(Math.imul(state,1664525)+1013904223)>>>0;return state/4294967296;};}
function combinations(xs,k){if(!k)return [[]];return xs.flatMap((c,i)=>combinations(xs.slice(i+1),k-1).map(t=>[c,...t]));}
export function balancedRows(rows,cap){const groups=new Map();for(const row of rows){if(!groups.has(row.seed))groups.set(row.seed,[]);groups.get(row.seed).push(row);}const selected=[];for(let index=0;selected.length<cap;index++){let added=false;for(const seed of [...groups.keys()].sort()){if(groups.get(seed)[index]&&selected.length<cap){selected.push(groups.get(seed)[index]);added=true;}}if(!added)break;}return selected;}
export function diagnosticSeedKey(r){return JSON.stringify([r.seed,r.size,r.observerCards,r.trump,r.turned,r.position,r.prefix,r.bid]);}

/** Offline true-card diagnostic. Actual opponent cards are labels here, never runtime inputs. */
export async function diagnoseBidEvidence(pointer,modelFile,output){
    const model=JSON.parse(await fs.readFile(modelFile,'utf8')),{validation}=await collectBidExamples(pointer);
    const groups=[];
    for(let size=2;size<=8;size++){
        const rows=validation.filter(r=>r.size===size),selected=balancedRows(rows,32);
        let nll=0,uniformNll=0,weight=0,posteriorBrier=0,priorBrier=0;
        for(const r of rows){nll-=r.weight*Math.log(bidDistribution(model,r.cards,size,r.trump,r.turned,r.position,r.prefix)[r.bid]);uniformNll+=r.weight*Math.log(legalBids(size,r.position,r.prefix).length);weight+=r.weight;}
        for(const r of selected){
            const pool=deck.filter(c=>c!==r.turned&&!r.observerCards.includes(c)),possible=[];
            if(size===2)possible.push(...combinations(pool,2));
            else {const random=generator(diagnosticSeedKey(r));for(let sample=0;sample<512;sample++){const shuffled=[...pool];for(let i=0;i<size;i++){const at=i+Math.floor(random()*(shuffled.length-i));[shuffled[i],shuffled[at]]=[shuffled[at],shuffled[i]];}possible.push(shuffled.slice(0,size));}}
            const prior=new Map(pool.map(c=>[c,0])),posterior=new Map(pool.map(c=>[c,0]));let total=0;
            for(const holding of possible){const ratio=bidDistribution(model,holding,size,r.trump,r.turned,r.position,r.prefix)[r.bid]*legalBids(size,r.position,r.prefix).length;total+=ratio;for(const c of holding){prior.set(c,prior.get(c)+1);posterior.set(c,posterior.get(c)+ratio);}}
            for(const c of pool){const label=r.cards.includes(c)?1:0;priorBrier+=(prior.get(c)/possible.length-label)**2/pool.length;posteriorBrier+=(posterior.get(c)/total-label)**2/pool.length;}
        }
        groups.push({size,validationRows:rows.length,seedWeightedNll:nll/weight,uniformNll:uniformNll/weight,cardDiagnosticRows:selected.length,cardDiagnosticSeedClusters:new Set(selected.map(r=>r.seed)).size,
            priorCardBrier:priorBrier/selected.length,posteriorCardBrier:posteriorBrier/selected.length,
            brierImprovement:priorBrier/selected.length-posteriorBrier/selected.length});
    }
    const result={status:'DIAGNOSTIC_ONLY',sourceModelSha256:crypto.createHash('sha256').update(await fs.readFile(modelFile)).digest('hex'),
        validationSeedClusters:model.split.validationSeeds.length,groups,
        scope:'Observed-bid likelihood and actual-card labels evaluated only on development validation seed clusters. Round-robin contexts balance seeds. Candidate-world RNG uses observer/public facts, never the actual hidden hand label. Exact one-opponent marginal at size2;512 common candidate holdings per row at3..8. Card scores are descriptive, not game-strength promotion or a full joint multi-opponent posterior.'};
    await fs.writeFile(output,JSON.stringify(result,null,2)+'\n',{flag:'wx'});return result;
}
if(process.argv[1]?.replaceAll('\\','/').endsWith('/whist-bid-evidence-diagnostics.mjs')){
    try{const[pointer,model,out]=process.argv.slice(2);assert.ok(out,'Need pointer/model/output');const r=await diagnoseBidEvidence(pointer,model,out);console.log(JSON.stringify(r,null,2));}
    catch(e){console.error(e.stack);process.exitCode=1;}
}
