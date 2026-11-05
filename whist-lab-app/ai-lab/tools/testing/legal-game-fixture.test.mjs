import test from 'node:test';
import assert from 'node:assert/strict';
import {createLegalGame} from './legal-game-fixture.mjs';
import {validateGame} from '../corpus/whist-corpus.mjs';

test('generated fixtures are reproducible, isolated objects and legal for every supported table size',()=>{
    for(let players=3;players<=6;players++)for(const seriesEnabled of [false,true])for(const seriesBonus of [5,10]) {
        const game=createLegalGame({players,seriesEnabled,seriesBonus});
        const expectedHands=3*players+12;
        assert.equal(validateGame(game).hands,expectedHands);
        assert.deepEqual(createLegalGame({players,seriesEnabled,seriesBonus}),game);
        assert.equal(game.source.kind,'GENERATED_COMPONENT_FIXTURE');
    }
    const changed=createLegalGame();changed.hands[0].deal[0].cards.length=0;
    assert.equal(createLegalGame().hands[0].deal[0].cards.length,1);
});

test('different fixture seeds vary deals without reading or adapting a retained game',()=>{
    assert.notDeepEqual(createLegalGame({seed:1}).hands[0].deal,createLegalGame({seed:2}).hands[0].deal);
    for(const options of [{seed:-1},{seed:1.5},{players:2},{players:7},{seriesBonus:7},{seriesEnabled:'true'}])
        assert.throws(()=>createLegalGame(options));
});
