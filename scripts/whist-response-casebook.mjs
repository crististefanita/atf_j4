import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {validateGame, decisionExamples} from './whist-corpus.mjs';

const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const cardId = code => 'SHCD'.indexOf(code.at(-1)) * 13 +
    ({A: 14, K: 13, Q: 12, J: 11}[code.slice(0, -1)] ?? Number(code.slice(0, -1))) - 2;
const code = id => ([...Array(13)].map((_, i) => String(i + 2)).slice(0, 9).concat(['J', 'Q', 'K', 'A']))[id % 13] + 'SHCD'[Math.floor(id / 13)];
const suit = c => Math.floor(c / 13);
const power = (c, led, trump) => (suit(c) === trump ? 200 : suit(c) === led ? 100 : 0) + c % 13;
const cost = (c, trump) => c % 13 + (suit(c) === trump ? 13 : 0);
const legal = (hand, led, trump) => {
    if (led < 0) return hand;
    const follows = hand.filter(c => suit(c) === led);
    if (follows.length) return follows;
    const cuts = hand.filter(c => suit(c) === trump);
    return cuts.length ? cuts : hand;
};
const winner = (trick, trump) => trick.reduce((a, b) =>
    power(b.card, suit(trick[0].card), trump) > power(a.card, suit(trick[0].card), trump) ? b : a).seat;
const points = (p, won) => {
    const exact = won === p.goals[p.hero], base = exact ? 5 + won : -Math.abs(won - p.goals[p.hero]);
    if (!p.seriesEnabled || p.size === 1) return base;
    return base + (exact && p.streak === 4 ? p.seriesBonus : !exact && p.streak === -4 ? -5 : 0);
};
const chooseCost = (cards, trump, highest) => cards.reduce((a, b) =>
    (highest ? cost(b, trump) > cost(a, trump) : cost(b, trump) < cost(a, trump)) ? b : a);

/** Separate list-based reference for the accepted deterministic simulated opponent. */
function opponent(p, hands, trick, actor, won) {
    const choices = legal(hands[actor], trick.length ? suit(trick[0].card) : -1, p.trump);
    const wants = p.goals[actor] > won[actor];
    if (!trick.length) return chooseCost(choices, p.trump, wants);
    const preferred = choices.filter(c => (winner([...trick, {seat: actor, card: c}], p.trump) === actor) === wants);
    return chooseCost(preferred.length ? preferred : choices, p.trump, preferred.length ? !wants : !wants);
}

/** Reconstruct the old own trick-bound heuristic for comparison, not as the expected optimum. */
function ownHeuristic(p, hands, trick, won) {
    const actor = p.hero, choices = legal(hands[actor], trick.length ? suit(trick[0].card) : -1, p.trump);
    const wants = p.goals[actor] > won[actor], secure = [], possible = [];
    for (const c of choices) {
        const led = trick.length ? suit(trick[0].card) : suit(c), value = power(c, led, p.trump);
        const owns = trick.every(t => power(t.card, led, p.trump) <= value);
        let overtaken = false, forced = false;
        if (owns) for (let offset = 1; offset < hands.length - trick.length; offset++) {
            const replies = legal(hands[(actor + offset) % hands.length], led, p.trump);
            const beating = replies.filter(r => power(r, led, p.trump) > value).length;
            overtaken ||= beating > 0;
            forced ||= replies.length > 0 && beating === replies.length;
        }
        if (wants ? owns && !overtaken : !owns || forced) secure.push(c);
        if (wants ? owns && !forced : !owns || overtaken) possible.push(c);
    }
    if (secure.length) return chooseCost(secure, p.trump, !wants);
    if (possible.length) return chooseCost(possible, p.trump, false);
    return chooseCost(choices, p.trump, !wants);
}

function advance(p, hands, trick, actor, won, card, continuation) {
    const next = hands.map(h => [...h]), at = next[actor].indexOf(card);
    assert.ok(at >= 0 && legal(next[actor], trick.length ? suit(trick[0].card) : -1, p.trump).includes(card));
    next[actor].splice(at, 1);
    let round = [...trick, {seat: actor, card}], turn = (actor + 1) % 4, count = [...won];
    if (round.length === 4) { turn = winner(round, p.trump); count[turn]++; round = []; }
    return continuation(next, round, turn, count);
}

/** Exact own best response against one explicitly declared simulated opponent policy. */
export function responseOracle(p) {
    assert.equal(p.hands.length, 4);
    assert.ok(p.hands.every(h => h.length <= 3));
    assert.equal(new Set(p.hands.flat()).size, p.hands.flat().length);
    const optimal = (hands, trick, actor, won) => {
        if (!hands.some(h => h.length)) return points(p, won[p.hero]);
        const choices = actor === p.hero ? legal(hands[actor], trick.length ? suit(trick[0].card) : -1, p.trump)
            : [opponent(p, hands, trick, actor, won)];
        return Math.max(...choices.map(c => advance(p, hands, trick, actor, won, c, optimal)));
    };
    const baseline = (hands, trick, actor, won) => {
        if (!hands.some(h => h.length)) return points(p, won[p.hero]);
        const card = actor === p.hero ? ownHeuristic(p, hands, trick, won) : opponent(p, hands, trick, actor, won);
        return advance(p, hands, trick, actor, won, card, baseline);
    };
    const values = Object.fromEntries(legal(p.hands[p.hero], p.trick.length ? suit(p.trick[0].card) : -1, p.trump)
        .map(c => [code(c), advance(p, p.hands, p.trick, p.hero, p.won, c, optimal)]));
    const best = Math.max(...Object.values(values));
    return {best, baseline: baseline(p.hands, p.trick, p.hero, p.won), values,
        optimalCards: Object.keys(values).filter(c => values[c] === best),
        baselineCard: code(ownHeuristic(p, p.hands, p.trick, p.won))};
}

export async function mineResponseCases(receiptsFolder, corpus, output) {
    const cases = [], capturedSizes = new Set();
    for (const file of (await fs.readdir(receiptsFolder)).filter(f => f.startsWith('development-player-') && f.endsWith('.json')).sort()) {
        const receipt = JSON.parse(await fs.readFile(path.join(receiptsFolder, file))), hero = receipt.parameters.heroSeat;
        const result = receipt.results.find(r => r.kind === 'before');
        const bytes = await fs.readFile(path.join(corpus, 'games/development', result.gameId + '.json'));
        const game = JSON.parse(bytes); assert.equal(game.partition, 'development'); validateGame(game);
        const observations = new Map([...decisionExamples(game)].filter(d => d.seat === hero && d.kind === 'card')
            .map(d => [d.handNumber + '/' + d.observation.history.length, d.observation]));
        for (const hand of game.hands) {
            if (capturedSizes.has(hand.size) || hand.size < 2 || hand.scores[hero].bid === hand.scores[hero].won) continue;
            const hands = hand.deal.map(d => d.cards.map(cardId)), won = [0, 0, 0, 0]; let history = 0;
            for (const round of hand.tricks) {
                const trick = [];
                for (const play of round.plays) {
                    if (!capturedSizes.has(hand.size) && play.seat === hero && hands[hero].length === Math.min(3, hand.size)) {
                        const observation = observations.get(hand.number + '/' + history);
                        const p = {hero, hands: hands.map(h => [...h]), trick: [...trick], won: [...won],
                            goals: hand.scores.map(s => s.bid), size: hand.size, trump: hand.trump === 'NT' ? 4 : 'SHCD'.indexOf(hand.trump),
                            seriesEnabled: game.rules.seriesEnabled, seriesBonus: game.rules.seriesBonus,
                            streak: observation.players[hero].streak};
                        const oracle = responseOracle(p);
                        if (oracle.best > oracle.baseline && !oracle.optimalCards.includes(oracle.baselineCard)) {
                            cases.push({id: 'response-tail-' + hand.size, gameId: game.id, gameSha256: hash(bytes), handNumber: hand.number,
                                scope: 'A post-game world is used ONLY as a hypothetical internal-simulation regression. It is never a real decision input or proof of public-information optimal play.',
                                publicObservation: observation, model: p, oracle});
                            capturedSizes.add(hand.size);
                        }
                    }
                    const c = cardId(play.card); hands[play.seat].splice(hands[play.seat].indexOf(c), 1);
                    trick.push({seat: play.seat, card: c}); history++;
                }
                won[round.winner]++;
            }
        }
        if (capturedSizes.size === 7) break;
    }
    await fs.writeFile(output, JSON.stringify({schemaVersion: 1, cases: cases.sort((a, b) => a.model.size - b.model.size)}, null, 2), {flag: 'wx'});
    return cases;
}

if (typeof process !== 'undefined' && process.argv[1]?.replaceAll('\\', '/').endsWith('/whist-response-casebook.mjs')) {
    try { const cases = await mineResponseCases(...process.argv.slice(2)); console.log(JSON.stringify(cases.map(c => ({id: c.id, baseline: c.oracle.baseline, best: c.oracle.best, alternatives: c.oracle.optimalCards})), null, 2)); }
    catch (error) { console.error(error.stack); process.exitCode = 1; }
}
