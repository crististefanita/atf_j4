import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import {traceHash,lessons,selectGames,curate,loadActiveGame,resolveActivePointer} from './whist-active-corpus.mjs';
const sample='C:/work/whist_back/training-corpus/games/development/android-round-v2-21049369348797.json';
const game=JSON.parse(await fs.readFile(sample));
const e=(id,tokens,sha=id)=>({id,traceHash:sha,lessons:tokens});
test('semantic trace dedup ignores IDs and controller names, but preserves different actions',()=>{
 const copy=structuredClone(game);copy.id='renamed';copy.players[0].controller='Other version';copy.source={changed:true};
 assert.equal(traceHash(game),traceHash(copy));copy.hands[0].bids[0].bid++;assert.notEqual(traceHash(game),traceHash(copy));
});
test('learning refuses heldout and retains both successes and mistakes for 2..8 cards',()=>{
 assert.throws(()=>lessons({...game,partition:'heldout'}),/development only/);
 const tokens=lessons(game);for(let size=2;size<=8;size++)assert.ok(tokens.some(t=>t.split('/')[1]===String(size)));
 assert.ok(tokens.some(t=>t.includes('/exact/')));assert.ok(tokens.some(t=>t.includes('/under/')||t.includes('/over/')));
 assert.ok(tokens.every(t=>t.split('/')[1]!=='1'));
});

test('arena profile names retain their actual player family in lesson coverage',()=>{
 const baseline=structuredClone(game),arena=structuredClone(game);
 for(const p of baseline.players)p.controller='IndependentPlayer';
 for(const [i,p] of arena.players.entries())p.controller=['point_greedy','point_covers','safe_greedy','safe_covers'][i];
 assert.deepEqual(lessons(arena),lessons(baseline));
 for(const p of baseline.players)p.controller='Expert';
 for(const [i,p] of arena.players.entries())p.controller=i%2?'expert_safety':'expert_points';
 assert.deepEqual(lessons(arena),lessons(baseline));
});
test('rarity cover removes repeated lessons without depending on input order',()=>{
 const entries=[e('a',['common']),e('b',['common','rare']),e('c',['common','other']),e('alias',['common','rare'],'b')];
 const a=selectGames(entries),b=selectGames([...entries].reverse());assert.deepEqual(a,b);
 assert.equal(a.uniqueTraces,3);assert.equal(a.coveredLessons,3);assert.equal(a.selected.length,2);
});
test('protected regression origins survive dedup even when they add no unique lesson',()=>{
 const r=selectGames([e('a',['common']),e('b',['common']),e('alias',['common'],'a')],{protectedIds:['alias','b']});
 assert.equal(r.selected.length,2);assert.ok(r.selected.some(e=>e.aliases.includes('alias')));assert.ok(r.selected.every(e=>e.protected));
});
test('caps disclose uncovered lessons and cannot silently drop protected origins',()=>{
 const input=[e('a',['a']),e('b',['b'])];const r=selectGames(input,{maxGames:1});assert.equal(r.missingLessons.length,1);
 assert.throws(()=>selectGames(input,{maxGames:1,protectedIds:['a','b']}),/exceed/);
 assert.throws(()=>selectGames(input,{protectedIds:['missing']}),/Missing protected/);
 for(const limit of [0,-1,1.5,NaN])assert.throws(()=>selectGames(input,{maxGames:limit}));
});
test('active corpus is development-only, immutable-source, hash-checked, path-safe and independently replayable',async()=>{
 const folder=await fs.mkdtemp(path.join(os.tmpdir(),'ATF_J4_CURATOR_TEST_'));
 const archive=path.join(folder,'archive'),active=path.join(folder,'active');
 await fs.mkdir(path.join(archive,'games/development'),{recursive:true});await fs.mkdir(path.join(archive,'games/heldout'));
 const source=path.join(archive,'games/development',game.id+'.json'),bytes=Buffer.from(JSON.stringify(game));
 await fs.writeFile(source,bytes);await fs.writeFile(path.join(archive,'games/heldout','DO_NOT_READ.json'),'intentionally invalid heldout: reading this fails');
 const m=await curate(archive,active,{maxGames:2});assert.equal(m.sourceGames,1);assert.equal(m.activeGames,1);
 assert.deepEqual(await fs.readFile(source),bytes);assert.equal((await loadActiveGame(active,m,m.games[0])).id,game.id);
 const activeFile=path.join(active,m.games[0].file);await fs.appendFile(activeFile,' ');
 await assert.rejects(loadActiveGame(active,m,m.games[0]),/hash mismatch/);
 await fs.writeFile(activeFile,bytes);
 await assert.rejects(loadActiveGame(active,{...m,activeGames:2},m.games[0]),/count mismatch/);
 const hostile={...m.games[0],file:'../outside.json'};await assert.rejects(loadActiveGame(active,{...m,games:[hostile]},hostile),/Unsafe/);
 await assert.rejects(curate(archive,path.join(archive,'nested')),/separate/);
 await assert.rejects(curate(archive,active),/EEXIST/);
});

test('current pointer binds the development manifest bytes and refuses heldout or relative targets',async()=>{
 const folder=await fs.mkdtemp(path.join(os.tmpdir(),'ATF_J4_POINTER_TEST_')),manifest=path.join(folder,'manifest.json'),pointer=path.join(folder,'current.json');
 const bytes=Buffer.from(JSON.stringify({schemaVersion:1,partition:'development'}));await fs.writeFile(manifest,bytes);
 const p={schemaVersion:1,partition:'development',folder,manifestSha256:crypto.createHash('sha256').update(bytes).digest('hex')};await fs.writeFile(pointer,JSON.stringify(p));assert.equal((await resolveActivePointer(pointer)).folder,folder);
 await fs.appendFile(manifest,' ');await assert.rejects(resolveActivePointer(pointer),/manifest hash mismatch/);await fs.writeFile(manifest,bytes);
 await fs.writeFile(pointer,JSON.stringify({...p,partition:'heldout'}));await assert.rejects(resolveActivePointer(pointer),/Development active pointer/);
 await fs.writeFile(pointer,JSON.stringify({...p,folder:'relative'}));await assert.rejects(resolveActivePointer(pointer),/Absolute active folder/);
});
