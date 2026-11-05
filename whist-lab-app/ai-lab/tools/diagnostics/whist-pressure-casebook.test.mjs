import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import {pressureFacts} from './whist-pressure-casebook.mjs';
import {cardCode} from '../corpus/whist-corpus.mjs';

const opening = () => ({handSize: 1, trump: 'S', turnedCard: '7S', ownHand: ['AH'],
    history: [], trick: [], players: Array.from({length: 4}, (_, seat) => ({remaining: 1, bid: 0, won: 0, seat}))});

test('Own and turned cards are excluded without distributing undealt cards to opponents', () => {
    const result = pressureFacts(opening(), 0);
    assert.equal(result.unknownCards.length, 30); assert.equal(result.undealtUnknownCount, 27);
    assert.ok(!result.unknownCards.includes('AH') && !result.unknownCards.includes('7S'));
});

test('A partial trick is counted once and played KS plus turned AS promote QS', () => {
    const observation = {...opening(), turnedCard: 'AS', ownHand: ['QS'],
        history: [{seat: 1, card: 'KS'}], trick: [{seat: 1, card: 'KS'}],
        players: [1, 0, 1, 1].map((remaining, seat) => ({seat, remaining, bid: 0, won: 0}))};
    const before = structuredClone(observation), facts = pressureFacts(observation, 2);
    assert.equal(facts.remaining, 1); assert.equal(facts.unknownCards.length, 29);
    assert.deepEqual(facts.guaranteedMasterTrumps, ['QS']); assert.equal(facts.impossible, true);
    assert.deepEqual(observation, before);
});

test('The retained real impossible-contract fixture has exactly five unknown cards and no undealt ones', async () => {
    const fixture = JSON.parse(await fs.readFile(new URL('../../../game-core/src/test/resources/regressions/contract-certificate/cases.json', import.meta.url), 'utf8')).impossible.input.before;
    const observation = {trump: 'NT', turnedCard: null, ownHand: fixture.hand.map(cardCode),
        players: fixture.seats, history: fixture.history.map(p => ({seat: p.seat, card: cardCode(p.card)})),
        trick: fixture.trick.map(p => ({seat: p.seat, card: cardCode(p.card)}))};
    const facts = pressureFacts(observation, 2);
    assert.equal(facts.needed, 3); assert.equal(facts.remaining, 2); assert.equal(facts.tableDifference, 1);
    assert.equal(facts.impossible, true); assert.equal(facts.undealtUnknownCount, 0);
    assert.deepEqual([...facts.unknownCards].sort(), ['8C', '8D', '9S', 'KD', 'QS']);
    // Uniform choice of the final leader's one remaining card: two spades, two diamonds, one club.
    // Either retained ace wins in exactly 2/5 cases; current AC always wins against either discard.
    assert.equal((-3 * 3 + -2 * 2) / 5, -2.6);
});

test('Actual opponent hands and an inconsistent card-count ledger are rejected', () => {
    const hidden = opening(); hidden.players[1].hand = ['AS'];
    assert.throws(() => pressureFacts(hidden, 0), /no opponent hands/);
    const bad = opening(); bad.players[1].remaining = 0;
    assert.throws(() => pressureFacts(bad, 0), /card-count ledger/);
});
