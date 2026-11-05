import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {pathToFileURL} from 'node:url';

const namespace = 'romanian4-seeded-deals';
const sha = value => crypto.createHash('sha256').update(value).digest('hex');
const canonical = value => JSON.stringify(sort(value));
function sort(value) {
    if (Array.isArray(value)) return value.map(sort);
    if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map(k => [k, sort(value[k])]));
    return value;
}
function requireThat(condition, message) { if (!condition) throw new Error(message); }
function range(firstSeed, seeds) {
    requireThat(Number.isSafeInteger(firstSeed) && firstSeed >= 0 && Number.isSafeInteger(seeds)
        && seeds > 0 && firstSeed + seeds <= 2147483647, 'Invalid seed interval');
    return {firstSeed, seeds};
}
function hash(value, label) { requireThat(typeof value === 'string' && /^[a-f0-9]{64}$/.test(value), `Invalid ${label} hash`); return value; }
function identity(request) {
    requireThat(typeof request.candidate === 'string' && /^[a-z0-9-]+$/.test(request.candidate), 'Invalid candidate identity');
    requireThat(['development','heldout'].includes(request.phase), 'Invalid evaluation phase');
    requireThat(request.sourceHashes && typeof request.sourceHashes === 'object' && !Array.isArray(request.sourceHashes)
        && Object.keys(request.sourceHashes).length > 0, 'Source inventory required');
    for (const value of Object.values(request.sourceHashes)) hash(value, 'source');
    return {...range(request.firstSeed, request.seeds), candidate:request.candidate, phase:request.phase,
        protocolSha256:hash(request.protocolSha256,'protocol'),
        freezeSha256:request.phase === 'heldout' ? hash(request.freezeSha256,'freeze') : null,
        sourceHashes:sort(request.sourceHashes)};
}
function overlaps(a,b) { return a.firstSeed < b.firstSeed+b.seeds && b.firstSeed < a.firstSeed+a.seeds; }

async function physicalRoot(root) {
    const absolute = path.resolve(root);
    await fs.mkdir(absolute,{recursive:true});
    let current = absolute;
    while (true) {
        const entry = await fs.lstat(current);
        requireThat(entry.isDirectory() && !entry.isSymbolicLink(), 'Registry requires physical directory ancestors');
        const parent = path.dirname(current);
        if (parent === current) break;
        current = parent;
    }
    return absolute;
}
async function locked(root, action, waitMs = 5000) {
    requireThat(Number.isFinite(waitMs) && waitMs >= 0 && waitMs <= 60000, 'Invalid lock deadline');
    root = await physicalRoot(root);
    const lock = path.join(root,'.lock'), started = performance.now();
    while (true) {
        try { await fs.mkdir(lock); break; }
        catch (error) {
            if (error.code !== 'EEXIST') throw error;
            requireThat(performance.now()-started < waitMs, 'Registry lock deadline exceeded; inspect interrupted owner, never steal its lock');
            let entry;
            try { entry = await fs.lstat(lock); }
            catch (changed) { if (changed.code === 'ENOENT') continue; throw changed; }
            requireThat(entry.isDirectory() && !entry.isSymbolicLink(), 'Invalid registry lock');
            await new Promise(resolve => setTimeout(resolve, Math.min(25,waitMs)));
        }
    }
    try {
        requireThat(!(await exists(path.join(root,'.pending.json'))), 'Interrupted registry write requires explicit recovery');
        return await action(root);
    } finally { await fs.rmdir(lock); }
}
async function exists(file) { try { await fs.lstat(file); return true; } catch(error) { if(error.code === 'ENOENT') return false; throw error; } }
async function save(root, body) {
    const bytes = canonical({...body,sha256:sha(canonical(body))})+'\n';
    const pending = path.join(root,'.pending.json');
    const handle = await fs.open(pending,'wx');
    try { await handle.writeFile(bytes); await handle.sync(); } finally { await handle.close(); }
    await fs.rename(pending,path.join(root,'registry.json'));
}
async function load(root) {
    const file = path.join(root,'registry.json');
    requireThat(await exists(file),'Registry must be initialized with reviewed historical exposures');
    const entry = await fs.lstat(file);
    requireThat(entry.isFile() && !entry.isSymbolicLink(),'Registry state must be a physical file');
    const {sha256,...body} = JSON.parse(await fs.readFile(file,'utf8'));
    requireThat(sha(canonical(body)) === sha256,'Registry checksum mismatch');
    requireThat(body.schemaVersion === 1 && body.namespace === namespace && Array.isArray(body.reservations)
        && Array.isArray(body.history?.exposures),'Unsupported registry state');
    for (const prior of body.history.exposures) { range(prior.firstSeed,prior.seeds); hash(prior.evidenceSha256,'history'); }
    const ids = new Set();
    for (const prior of body.reservations) {
        const normalized = identity(prior.identity);
        requireThat(prior.id === sha(canonical(normalized)) && !ids.has(prior.id),'Invalid or duplicate reservation');
        ids.add(prior.id);
    }
    return body;
}

/** Initialization is explicit: an empty history means a reviewed clean environment, never an automatic reset. */
export async function initializeRegistry(root, history) {
    requireThat(history && typeof history.reviewedBy === 'string' && history.reviewedBy.trim()
        && Array.isArray(history.exposures), 'Reviewed history manifest required');
    for (const item of history.exposures) { range(item.firstSeed,item.seeds); hash(item.evidenceSha256,'history'); }
    return locked(root, async folder => {
        requireThat(!(await exists(path.join(folder,'registry.json'))),'Registry already initialized; never erase exposure');
        const body = {schemaVersion:1,namespace,createdAt:new Date().toISOString(),history:structuredClone(history),reservations:[]};
        await save(folder,body);
        return {namespace,historicalIntervals:history.exposures.length};
    });
}

/** Corpus/run names, roles, seats and protocol versions do not create a new deal namespace. */
export async function reserveEvaluation(root, request, options = {}) {
    const selected = identity(request);
    const id = sha(canonical(selected));
    return locked(root, async folder => {
        const body = await load(folder);
        const historical = body.history.exposures.some(prior => overlaps(selected,prior));
        const overlapping = body.reservations.filter(prior => overlaps(selected,prior.identity));
        const prior = overlapping.find(prior => prior.id === id);
        if (prior) {
            return {schemaVersion:1,namespace,reservationId:id,disposition:'CONTINUATION',identity:selected,
                originallyReservedAt:prior.reservedAt,registryRoot:folder};
        }
        if (selected.phase === 'heldout') {
            requireThat(!historical,'Seed interval overlaps reviewed historical exposure');
            requireThat(overlapping.length === 0,'Seed interval overlaps an existing exposure; rename/copy cannot reset it');
        }
        const reservedAt = new Date().toISOString();
        const disposition = historical || overlapping.length ? 'DEVELOPMENT_REUSE' : 'NEW_RESERVATION';
        body.reservations.push({id,identity:selected,reservedAt,disposition,historicalOverlap:historical,
            priorExposureIds:overlapping.map(prior=>prior.id),firstRun:request.run ?? null,firstCorpus:request.corpus ?? null});
        await save(folder,body);
        return {schemaVersion:1,namespace,reservationId:id,disposition,identity:selected,
            originallyReservedAt:reservedAt,registryRoot:folder};
    },options.waitMs);
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
    try {
        const [mode,root,input,output] = process.argv.slice(2);
        requireThat(['initialize','reserve'].includes(mode) && root && input && output,
            'Usage: whist-evaluation-registry.mjs initialize|reserve <registry> <input.json> <fresh-receipt.json>');
        requireThat(!(await exists(output)),'Fresh receipt required');
        const request = JSON.parse(await fs.readFile(input,'utf8'));
        const receipt = mode === 'initialize' ? await initializeRegistry(root,request) : await reserveEvaluation(root,request);
        await fs.writeFile(output,JSON.stringify(receipt,null,2)+'\n',{flag:'wx'});
        console.log(JSON.stringify(receipt));
    } catch(error) { console.error(error.stack); process.exitCode = 1; }
}
