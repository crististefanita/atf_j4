import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {validateGame, decisionExamples} from '../corpus/whist-corpus.mjs';

const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const suit = card => card.at(-1);
const rank = card => ({J: 11, Q: 12, K: 13, A: 14}[card.slice(0, -1)] ?? Number(card.slice(0, -1)));
const deck = n => [...'SHCD'].flatMap(s => Array.from({length: 2 * n}, (_, i) => {
    const value = 15 - 2 * n + i;
    return String(({11: 'J', 12: 'Q', 13: 'K', 14: 'A'})[value] ?? value) + s;
}));

/** Independent public ledger for retained teaching positions, not an optimal-action predictor. */
export function pressureFacts(observation, actor) {
    const players = observation.players, n = players.length;
    assert.ok(n >= 3 && n <= 6 && Number.isInteger(actor) && actor >= 0 && actor < n);
    assert.ok(players.every(p => !('hand' in p) && Number.isInteger(p.bid) && p.remaining >= 0),
        'Public counts and completed bids only; no opponent hands');
    const known = new Set([...observation.ownHand, ...observation.history.map(p => p.card),
        ...observation.trick.map(p => p.card), ...(observation.turnedCard ? [observation.turnedCard] : [])]);
    const unknown = deck(n).filter(c => !known.has(c));
    const remaining = (players.reduce((sum, p) => sum + p.remaining, 0) + observation.trick.length) / n;
    assert.ok(Number.isInteger(remaining) && remaining > 0, 'Complete card-count ledger required');
    const own = players[actor], needed = own.bid - own.won;
    const masters = observation.trump === 'NT' ? [] : observation.ownHand.filter(c =>
        suit(c) === observation.trump && !unknown.some(u => suit(u) === suit(c) && rank(u) > rank(c)));
    const undealt = unknown.length - players.reduce((sum, p, seat) => sum + (seat === actor ? 0 : p.remaining), 0);
    assert.ok(undealt >= 0, 'Public ledger cannot invent dealt cards');
    return {needed, remaining, density: needed / remaining,
        tableDifference: players.reduce((sum, p) => sum + p.bid - p.won, 0) - remaining,
        guaranteedMasterTrumps: masters, impossible: needed < masters.length || needed > remaining,
        unknownCards: unknown, undealtUnknownCount: undealt};
}

/** Fixed cap, one position per context; outcomes never become optimal-move labels. */
export async function pressureCasebook(invocations, corpus, output, cap = 12) {
    assert.ok(Number.isInteger(cap) && cap > 0 && cap <= 32);
    const selected = new Map(), sources = [];
    for (const file of (await fs.readdir(invocations)).filter(f => f.startsWith('development-') && f.endsWith('.json')).sort()) {
        const receipt = JSON.parse(await fs.readFile(path.join(invocations, file), 'utf8'));
        assert.equal(receipt.status, 'PASS');
        const after = receipt.results.find(r => r.kind === 'after');
        const gameFile = path.join(corpus, 'games/development', after.gameId + '.json');
        const bytes = await fs.readFile(gameFile), game = JSON.parse(bytes);
        assert.equal(game.partition, 'development'); validateGame(game);
        sources.push({id: game.id, sha256: hash(bytes)});
        for (const example of decisionExamples(game)) {
            if (example.kind !== 'card' || example.seat !== receipt.parameters.heroSeat || example.observation.handSize < 2) continue;
            const facts = pressureFacts(example.observation, example.seat);
            const regime = facts.impossible ? 'impossible' : facts.needed === 0 ? 'at-target' :
                facts.needed === facts.remaining ? 'need-all' : 'partial';
            const key = [regime, Math.sign(facts.tableDifference), facts.guaranteedMasterTrumps.length > 0 ? 'master' : 'no-master'].join('/');
            if (!selected.has(key) && selected.size < cap) selected.set(key, {
                id: 'pressure-' + key.replaceAll('/', '-'), regime, source: {gameId: game.id, sha256: hash(bytes)},
                hand: example.handNumber, actor: example.seat, facts, observation: example.observation,
                recordedAction: example.action, recordedOutcome: example.outcome,
                scope: 'Public decision context plus separately recorded ex-post outcome; no claim that the recorded action is optimal.'});
        }
    }
    const result = {schemaVersion: 1, partition: 'development', cap, selected: selected.size,
        selection: 'First context per regime/table sign/master state, declared sorted source order; bounded teaching data, not a strength benchmark.',
        sources, cases: [...selected.values()]};
    await fs.writeFile(output, JSON.stringify(result, null, 2) + '\n', {flag: 'wx'});
    return {selected: result.selected, cap, sources: sources.length, bytes: (await fs.stat(output)).size};
}

if (process.argv[1]?.replaceAll('\\', '/').endsWith('/whist-pressure-casebook.mjs')) {
    try {
        const [invocations, corpus, output] = process.argv.slice(2);
        assert.ok(output, 'Usage: whist-pressure-casebook.mjs <development-invocations> <corpus> <fresh-output>');
        console.log(JSON.stringify(await pressureCasebook(invocations, corpus, output), null, 2));
    } catch (e) { console.error(e.stack); process.exitCode = 1; }
}
