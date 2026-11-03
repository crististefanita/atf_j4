import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {validateGame} from './whist-corpus.mjs';

const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const json = bytes => JSON.parse(bytes.toString().replace(/^\uFEFF/, ''));
const write = (file, value) => fs.writeFile(file, JSON.stringify(value, null, 2) + '\n', {flag: 'wx'});

export function validateCapture(r, legacyArenaInventory = false, expectedStatus = 'PASS') {
    assert.equal(r.schemaVersion, 1);
    for (const key of ['runId', 'invocationId', 'className', 'methodName', 'startedAt', 'endedAt']) {
        assert.equal(typeof r[key], 'string'); assert.ok(r[key].length);
    }
    assert.ok(Number.isFinite(Date.parse(r.startedAt)) && Number.isFinite(Date.parse(r.endedAt)));
    assert.ok(Number.isSafeInteger(r.durationNanos) && r.durationNanos > 0);
    assert.ok(['PASS', 'FAIL'].includes(expectedStatus), 'Only explicitly declared closed outcomes are publishable');
    assert.equal(r.status, expectedStatus, 'Source outcome differs from the declared report population');
    if (r.status === 'FAIL') assert.ok(typeof r.failure === 'string' && r.failure.length, 'A failed experiment needs its original reason');
    assert.ok(r.parameters && Array.isArray(r.checkpoints) && r.checkpoints.length);
    const owners = new Map(); let knownDuplicates = 0;
    for (const cp of r.checkpoints) {
        assert.ok(cp.owner && cp.title && cp.context && Array.isArray(cp.comparisons));
        if (owners.has(cp.owner)) {
            assert.ok(legacyArenaInventory && cp.owner === r.invocationId + '/setup'
                && cp.title === 'Complete Romanian schedule'
                && owners.get(cp.owner) === 'Four declared strategies and balanced seating', 'Repeated checkpoint owner');
            knownDuplicates++;
        }
        owners.set(cp.owner, cp.title);
        for (const c of cp.comparisons) {
            assert.equal(typeof c.passed, 'boolean');
            if (r.status === 'PASS') assert.equal(c.passed, true, 'Contradictory PASS capture');
        }
    }
    if (legacyArenaInventory) assert.equal(knownDuplicates, 1, 'Legacy exception must match the declared historical defect exactly');
}

export function selectWholeCaptures(records, requestedIds, expectedStatuses = {}) {
    assert.equal(new Set(requestedIds).size, requestedIds.length, 'Repeated selection');
    const byId = new Map();
    for (const r of records) {
        validateCapture(r, false, expectedStatuses[r.invocationId] ?? 'PASS');
        if (r.status !== 'PASS') assert.ok(requestedIds.includes(r.invocationId), 'Declared failed experiment must be included whole');
        assert.ok(!byId.has(r.invocationId), 'Duplicate population identity');
        byId.set(r.invocationId, r);
    }
    for (const id of Object.keys(expectedStatuses)) assert.ok(byId.has(id), 'Declared outcome absent from population');
    return requestedIds.map(id => { assert.ok(byId.has(id), 'Missing requested capture: ' + id); return byId.get(id); });
}

export function nativeCounts(xml) {
    const head = xml.match(/<testsuite\b([^>]*)>/);
    assert.ok(head, 'Missing native JUnit suite');
    const result = {};
    for (const key of ['tests', 'failures', 'errors', 'skipped']) {
        const match = head[1].match(new RegExp('\\b' + key + '="(\\d+)"'));
        assert.ok(match, 'Missing native count: ' + key); result[key] = Number(match[1]);
    }
    assert.ok(result.tests > 0 && result.failures === 0 && result.errors === 0 && result.skipped === 0,
        'Unfinished, failing or skipped native execution');
    return result;
}

function checkResult(r, result, game) {
    const seat = result.seat ?? r.parameters.heroSeat;
    assert.ok([0, 1, 2, 3].includes(seat));
    const best = Math.max(...game.finalScores), ties = game.finalScores.filter(n => n === best).length;
    assert.equal(result.points, game.finalScores[seat]);
    assert.equal(result.winShare, result.points === best ? 1 / ties : 0);
    const buckets = {};
    for (const hand of game.hands) {
        const score = hand.scores[seat];
        const b = buckets[hand.size] ?? {hands: 0, exact: 0, points: 0, absoluteBidError: 0};
        b.hands++; b.exact += score.bid === score.won ? 1 : 0;
        b.points += score.base + score.series; b.absoluteBidError += Math.abs(score.won - score.bid);
        buckets[hand.size] = b;
    }
    assert.deepEqual(result.byHandSize, buckets, 'Receipt metrics differ from the independent game replay');
    if (result.totals) assert.deepEqual(result.totals, game.finalScores);
    if (result.exact !== undefined) assert.equal(result.exact, Object.values(buckets).reduce((n, b) => n + b.exact, 0));
}

/** Produces bounded domain inputs. It never modifies a receipt, a reporting binary or HTML. */
export async function collectReportInput(specFile, output) {
    const startedAt = new Date().toISOString(), begin = performance.now();
    const specBytes = await fs.readFile(specFile), spec = json(specBytes);
    assert.equal(spec.schemaVersion, 1); assert.ok(spec.id && spec.scope && spec.cohorts.length);
    await fs.mkdir(output, {recursive: false});
    await fs.mkdir(path.join(output, 'invocations'));
    const checkpoints = [], manifest = [], inventory = [], globalIds = new Set(), gameCache = new Map();
    const checkpoint = (owner, title, context, comparisons) => {
        assert.ok(JSON.stringify(context, null, 2).length <= 30000, 'Split large mandatory context at a semantic boundary');
        checkpoints.push({owner, title, context, comparisons});
    };
    const equal = (name, expected, actual) => { assert.deepEqual(actual, expected, name); return {name, expected, actual, passed: true}; };
    checkpoint('release/scope', 'Scope, role decisions and selection made before composition', spec.scope,
        [equal('A bounded input collection; no claim that every native case is rendered', true, true)]);

    const bindings = [];
    for (const item of spec.bindings) {
        const bytes = await fs.readFile(item.file); assert.equal(hash(bytes), item.sha256, 'Source/binary binding changed: ' + item.file);
        bindings.push(item);
    }
    for (let i = 0; i < bindings.length; i += 32) checkpoint('release/bindings/' + i,
        'Exact source and reporting binary identities', {files: bindings.slice(i, i + 32)},
        [equal('Every listed source matches its previously frozen digest', true, true)]);

    for (const native of spec.nativeSuites) {
        const counts = {tests: 0, failures: 0, errors: 0, skipped: 0}, files = [];
        for (const name of (await fs.readdir(native.folder)).filter(n => n.startsWith('TEST-') && n.endsWith('.xml')).sort()) {
            const bytes = await fs.readFile(path.join(native.folder, name)), c = nativeCounts(bytes.toString());
            for (const key of Object.keys(counts)) counts[key] += c[key];
            files.push({file: name, sha256: hash(bytes), ...c});
        }
        assert.deepEqual(counts, native.expected, 'Native population changed: ' + native.id);
        for (let i = 0; i < files.length; i += 24) checkpoint('release/native/' + native.id + '/' + i,
            'Measured native suite: ' + native.id, {scope: native.scope, counts, files: files.slice(i, i + 24)},
            [equal('Actual complete native population', native.expected, counts)]);
    }

    for (const cohort of spec.cohorts) {
        if (cohort.legacyArenaInventory) assert.deepEqual(cohort.includeIds, [], 'Legacy defective captures are inventory only');
        const names = (await fs.readdir(cohort.folder)).filter(n => n.endsWith('.json')).sort();
        assert.equal(names.length, cohort.population.length, 'Unexpected execution population: ' + cohort.id);
        const expected = new Map(cohort.population.map(e => [e.invocationId, e.sha256]));
        assert.equal(expected.size, cohort.population.length);
        const sources = [], records = [];
        for (const name of names) {
            const source = path.join(cohort.folder, name), bytes = await fs.readFile(source), r = json(bytes);
            validateCapture(r, cohort.legacyArenaInventory === true, cohort.expectedStatuses?.[r.invocationId] ?? 'PASS'); assert.equal(name, r.invocationId + '.json');
            assert.equal(hash(bytes), expected.get(r.invocationId), 'Source population changed');
            assert.ok(!globalIds.has(r.invocationId), 'Repeated invocation across cohorts'); globalIds.add(r.invocationId);
            let dealIdentity;
            if (cohort.gameAudit) for (const result of r.results) {
                const id = result.gameId ?? r.invocationId;
                assert.match(id, /^[a-zA-Z0-9_.-]+$/);
                const file = path.join(cohort.gameAudit.corpus, 'games', cohort.gameAudit.partition, id + '.json');
                let cached = gameCache.get(file);
                if (!cached) {
                    const gameBytes = await fs.readFile(file), game = json(gameBytes), audit = validateGame(game);
                    assert.equal(game.partition, cohort.gameAudit.partition); assert.equal(game.id, id);
                    cached = {game, audit, sha256: hash(gameBytes)}; gameCache.set(file, cached);
                }
                checkResult(r, result, cached.game);
                const deal = hash(JSON.stringify(cached.game.hands.map(h => [h.dealer, h.trump, h.turnedCard, h.deal])));
                if (dealIdentity) assert.equal(deal, dealIdentity, 'Paired games changed the initial worlds');
                dealIdentity = deal;
            }
            sources.push({invocationId: r.invocationId, sha256: hash(bytes), status: r.status,
                format: cohort.legacyArenaInventory ? 'LEGACY_DUPLICATE_SETUP_OWNER_NOT_PUBLISHABLE' : 'VALID',
                detailed: cohort.includeIds.includes(r.invocationId), source});
            records.push(r);
        }
        const selected = cohort.legacyArenaInventory ? [] : selectWholeCaptures(records, cohort.includeIds, cohort.expectedStatuses);
        for (const r of selected) {
            const source = sources.find(s => s.invocationId === r.invocationId), file = 'invocations/' + r.invocationId + '.json';
            await fs.copyFile(source.source, path.join(output, file));
            manifest.push({file, sha256: source.sha256, invocationId: r.invocationId});
        }
        inventory.push({id: cohort.id, population: sources.length, detailed: selected.length, sources});
        for (let i = 0; i < sources.length; i += 32) checkpoint('release/inventory/' + cohort.id + '/' + i,
            'Full source inventory: ' + cohort.id, {scope: cohort.scope, total: sources.length,
                detailed: selected.length, omittedFromDetailedHtml: sources.length - selected.length,
                sources: sources.slice(i, i + 32).map(({source, ...s}) => s)},
            [equal('Entire source cohort validated; detail selection does not change outcomes', sources.length, records.length)]);
    }

    for (const proof of spec.proofs) {
        const bytes = await fs.readFile(proof.file); assert.equal(hash(bytes), proof.sha256, 'Proof changed: ' + proof.file);
        const value = json(bytes), comparisons = [];
        for (const [key, expected] of Object.entries(proof.expect)) {
            const actual = key.split('.').reduce((v, k) => v?.[k], value);
            comparisons.push(equal(proof.id + ': ' + key, expected, actual));
        }
        checkpoint('release/proof/' + proof.id, proof.title, {sourceSha256: proof.sha256, evidence: value}, comparisons);
    }
    const gameInventory = [...gameCache.entries()].map(([file, g]) => ({file, sha256: g.sha256, ...g.audit}));
    checkpoint('release/audit', 'Actual independent game replays performed while preparing report input',
        {uniqueGamesReplayed: gameInventory.length, inventoryScope: 'All source games, including games omitted from detailed HTML. External JSON inventory is immutable audit material.',
            limitations: 'No new UI execution or screenshots. No optimal-play claim. Native counters and model acceptance are different verdicts.'},
        [equal('No game replay rejected its rules, legal moves or scores', true, true)]);
    const gate = {schemaVersion: 1, runId: spec.id, invocationId: 'release-input-audit', className: 'scripts.whist-report-input',
        methodName: 'bindReleaseAndCollectWholeInvocations', parameters: {collection: 'verification'},
        startedAt, endedAt: new Date().toISOString(), durationNanos: Math.max(1, Math.round((performance.now() - begin) * 1e6)),
        executionKind: 'NODE_COMPONENT_GATE', status: 'PASS', checkpoints, results: []};
    const gateFile = 'invocations/' + gate.invocationId + '.json'; await write(path.join(output, gateFile), gate);
    manifest.unshift({file: gateFile, sha256: hash(await fs.readFile(path.join(output, gateFile))), invocationId: gate.invocationId});
    const ids = new Set(manifest.map(r => r.invocationId)); assert.equal(ids.size, manifest.length);
    await write(path.join(output, 'manifest.json'), {schemaVersion: 1, capturedInvocations: manifest.length, receipts: manifest});
    await write(path.join(output, 'population-inventory.json'), inventory);
    await write(path.join(output, 'game-audit-inventory.json'), gameInventory);
    await fs.writeFile(path.join(output, 'input-spec.json'), specBytes, {flag: 'wx'});
    const result = {status: 'PASS', capturedInvocations: manifest.length,
        sourceInvocations: inventory.reduce((n, c) => n + c.population, 0), uniqueGamesReplayed: gameInventory.length,
        contexts: checkpoints.length, scope: spec.scope};
    await write(path.join(output, 'collection-result.json'), result);
    return result;
}

if (process.argv[1]?.replaceAll('\\', '/').endsWith('/whist-report-input.mjs')) {
    try { const [spec, output] = process.argv.slice(2); assert.ok(spec && output, 'Usage: whist-report-input.mjs <spec.json> <fresh-output>');
        console.log(JSON.stringify(await collectReportInput(spec, output), null, 2));
    } catch (error) { console.error(error.stack); process.exitCode = 1; }
}
