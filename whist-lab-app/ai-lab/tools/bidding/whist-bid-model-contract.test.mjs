import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import {FEATURE_NAMES} from './whist-bid-evidence.mjs';
import {generateBidEvidence} from './whist-bid-evidence-codegen.mjs';

const modelFile=new URL('../../../../config/whist-bid-model.json', import.meta.url);
const yamlFile=new URL('../../../../config/whist-public-beliefs.yaml', import.meta.url);
const sha=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');

test('The bilingual rule dossier binds every fitted coefficient, dispersion and exact model bytes',async()=>{
    const bytes=await fs.readFile(modelFile),model=JSON.parse(bytes),yaml=await fs.readFile(yamlFile,'utf8');
    assert.equal(yaml.match(/model_sha256: "([a-f0-9]+)"/)[1],sha(bytes));
    const rows=[...yaml.matchAll(/name: "([^"]+)", coefficient: ([^,]+),/g)].map(m=>[m[1],Number(m[2])]);
    assert.deepEqual(rows.map(r=>r[0]),FEATURE_NAMES);
    assert.deepEqual(rows.map(r=>r[1]),model.coefficients);
    const sigmas=yaml.split('sigma_by_initial_hand_size:')[1].split('training:')[0];
    for(let size=2;size<=8;size++)assert.equal(Number(sigmas.match(new RegExp('\\n  '+size+': ([^\\r\\n]+)'))[1]),model.sigma[size]);
    assert.match(yaml,/editing YAML alone does not tune the engine/);
});

test('The frozen model and generator reproduce the corrected Java model byte for byte',async()=>{
    const temporary=await fs.mkdtemp(path.join(os.tmpdir(),'ATF_J4_BID_CODEGEN_'));
    const target=path.join(temporary,'LearnedBidEvidence.java');
    try{
        await generateBidEvidence(modelFile.pathname.replace(/^\/(\w:)/,'$1'),target);
        assert.equal(sha(await fs.readFile(target)),'135f001bd5a418d89d5a5fc307918086ad09c10bff0b26a13e5b4b187509bb2b');
    }finally{
        // Only files created by this invocation; no recursive removal or shared output.
        await fs.unlink(target).catch(e=>{if(e.code!=='ENOENT')throw e;});
        await fs.rmdir(temporary);
    }
});
