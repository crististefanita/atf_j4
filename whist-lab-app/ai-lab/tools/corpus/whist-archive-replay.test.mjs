import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import crypto from 'node:crypto';
import {replayArchive} from './whist-archive-replay.mjs';
import {createLegalGame} from '../testing/legal-game-fixture.mjs';
const sha=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');

async function fixture(action) {
    const folder=await fs.mkdtemp(path.join(os.tmpdir(),'whist-archive-component-'));
    try { await action(path.join(folder,'reference.json')); }
    finally {
        assert.equal(path.resolve(path.dirname(folder)),path.resolve(os.tmpdir()));
        assert.match(path.basename(folder),/^whist-archive-component-/);
        await fs.rm(folder,{recursive:true,force:true,maxRetries:5,retryDelay:100});
    }
}
test('explicit archive replay binds the exact bytes and preserves the replay scope',async()=>{
    await fixture(async file=>{
        const game={...createLegalGame(),partition:'reference'},bytes=Buffer.from(JSON.stringify(game));
        await fs.writeFile(file,bytes);
        const result=await replayArchive(file,sha(bytes).toUpperCase());
        assert.equal(result.scope,'ARCHIVE_REPLAY');assert.equal(result.sourceSha256,sha(bytes));
        assert.deepEqual(result.game,game);assert.equal(result.audit.status,'PASS');
        await fs.appendFile(file,' ');
        await assert.rejects(replayArchive(file,sha(bytes)),/SHA-256 mismatch/);
    });
});
test('archive replay refuses implicit inputs, a missing file and an absent or false seal',async()=>{
    await fixture(async file=>{
        for(const seal of [undefined,'','bad'])await assert.rejects(replayArchive(file,seal),/explicit file and SHA-256/);
        await assert.rejects(replayArchive(file,'0'.repeat(64)),{code:'ENOENT'});
        const bytes=Buffer.from(JSON.stringify(createLegalGame()));await fs.writeFile(file,bytes);
        await assert.rejects(replayArchive(file,'0'.repeat(64)),/SHA-256 mismatch/);
    });
});
test('a matching hash cannot certify malformed or illegal game data',async()=>{
    await fixture(async file=>{
        for(const mutate of [g=>{g.hands[0].bids[0].bid=9;},g=>{g.finalScores[0]++;}]) {
            const game=createLegalGame();mutate(game);const bytes=Buffer.from(JSON.stringify(game));
            await fs.writeFile(file,bytes);await assert.rejects(replayArchive(file,sha(bytes)));
        }
        await fs.writeFile(file,'{');await assert.rejects(replayArchive(file,sha('{')));
    });
});
