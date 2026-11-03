import fs from 'node:fs/promises';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {FEATURE_NAMES,bidFeatures,bidDistribution} from './whist-bid-evidence.mjs';

export async function generateBidEvidence(modelFile,javaFile) {
    const bytes=await fs.readFile(modelFile),m=JSON.parse(bytes),digest=crypto.createHash('sha256').update(bytes).digest('hex');
    assert.deepEqual(m.featureNames,FEATURE_NAMES);assert.equal(m.predictiveGate,'PASS');assert.equal(m.strength,0.25);
    assert.equal(m.coefficients.length,21);assert.equal(m.sigma.length,9);
    assert.ok([...m.coefficients,...m.sigma].every(Number.isFinite)&&m.sigma.every(s=>s>=0.55));
    let text=await fs.readFile(new URL('templates/learned-bid-evidence.java',import.meta.url),'utf8');
    text=text.replace('@@MODEL_SHA256@@',digest).replace('@@COEFFICIENTS@@',m.coefficients.map(x=>x.toPrecision(17)).join(',')).replace('@@SIGMA@@',m.sigma.map(x=>x.toPrecision(17)).join(','));
    assert.ok(!text.includes('@@'));await fs.writeFile(javaFile,text,{flag:'wx'});return {status:'PASS',modelSha256:digest,javaSha256:crypto.createHash('sha256').update(text).digest('hex')};
}
export async function generateBidFixtures(modelFile,target) {
    const bytes=await fs.readFile(modelFile),model=JSON.parse(bytes);
    const inputs=[
        {cards:['KS','QS'],size:2,trump:'S',turned:'AS',position:0,prefix:0},
        {cards:['KH','7H'],size:2,trump:'S',turned:'8S',position:1,prefix:0},
        {cards:['QH','7H','8H'],size:3,trump:'S',turned:'8S',position:2,prefix:2},
        {cards:['AH','KH','7H'],size:3,trump:'NT',turned:null,position:0,prefix:0},
        {cards:['AS','KS','QS','7D'],size:4,trump:'S',turned:'7S',position:3,prefix:2},
        {cards:['7S','8S','AH','QH','JC','10C','KD','7D'],size:8,trump:'NT',turned:null,position:3,prefix:6}];
    const result={sourceModelSha256:crypto.createHash('sha256').update(bytes).digest('hex'),featureNames:FEATURE_NAMES,
        scope:'Independent JavaScript/Java implementation agreement; physical expectations have separate literal unit tests.',
        cases:inputs.map(input=>({...input,features:bidFeatures(input.cards,input.size,input.trump,input.turned,input.position,input.prefix),
            probabilities:bidDistribution(model,input.cards,input.size,input.trump,input.turned,input.position,input.prefix)}))};
    await fs.writeFile(target,JSON.stringify(result,null,2)+'\n',{flag:'wx'});return {cases:inputs.length};
}
if(process.argv[1]?.replaceAll('\\','/').endsWith('/whist-bid-evidence-codegen.mjs')){
    try{const args=process.argv.slice(2),fixtures=args[0]==='fixtures';const [model,target]=fixtures?args.slice(1):args;assert.ok(target);console.log(JSON.stringify(await (fixtures?generateBidFixtures(model,target):generateBidEvidence(model,target)),null,2));}
    catch(e){console.error(e.stack);process.exitCode=1;}
}
