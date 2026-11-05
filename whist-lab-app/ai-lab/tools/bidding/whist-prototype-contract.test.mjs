import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';

const root = new URL('../../experiments/round5-rejected-bid-prior/', import.meta.url);
const expectedFeatures = ['intercept', 'size/8', 'noTrump', 'trumps/8', 'trumpPower/8',
    'nonTrumpA/4', 'nonTrumpK/4', 'nonTrumpQ/4', 'nonTrumpJ/4', 'suitSquares/64',
    'voids/4', 'rankPower/8', 'position/3', 'priorBids/8'];
const [metadata, engine, independent] = await Promise.all([
    fs.readFile(new URL('calibration.json', root), 'utf8').then(JSON.parse),
    fs.readFile(new URL('engine/EstimatedBidPrior.java', root), 'utf8'),
    fs.readFile(new URL('independent/EstimatedBidPrior.java', root), 'utf8')]);

function coefficients(source, name) {
    const match = source.match(new RegExp(`double\\[\\] ${name}=\\{([^}]+)\\}`));
    assert.ok(match, `Missing numeric vector ${name}`);
    const tokens = match[1].split(',').map(value => value.trim());
    assert.ok(tokens.every(value => value.length > 0), 'Empty coefficient');
    const values = tokens.map(Number);
    assert.ok(values.every(Number.isFinite), 'Non-finite coefficient');
    return values;
}

function validate(model, engineSource, independentSource) {
    assert.equal(model.schemaVersion, 1);
    assert.equal(model.status, 'DEVELOPMENT_ONLY');
    assert.deepEqual(model.features, expectedFeatures, 'Ordered feature vocabulary');
    const selected = model.selected;
    assert.equal(selected.coefficients.length, expectedFeatures.length, 'Coefficient arity');
    assert.ok(selected.coefficients.every(Number.isFinite), 'Finite selected coefficients');
    assert.ok(Number.isFinite(selected.lambda) && selected.lambda > 0, 'Positive regularization');
    const experiment = model.experiments.find(row => row.lambda === selected.lambda);
    assert.ok(experiment, 'Selected experiment exists');
    assert.deepEqual(experiment.coefficients, selected.coefficients, 'Selected experiment matches');
    assert.deepEqual(coefficients(engineSource, 'COEFFICIENTS'), selected.coefficients, 'Engine coefficients');
    assert.deepEqual(coefficients(independentSource, 'BETA'), selected.coefficients, 'Independent coefficients');
    const digest = source => source.match(/TRAINING_DIGEST="([a-f0-9]{64})"/)?.[1];
    assert.ok(digest(engineSource), 'Engine declares its training identity');
    assert.equal(digest(independentSource), digest(engineSource), 'Shared training identity');
}

test('Retained prototype metadata agrees numerically with both Java source variants', () => {
    validate(metadata, engine, independent);
});

test('Feature reordering or duplication cannot silently relabel the coefficients', () => {
    for (const duplicate of [false, true]) {
        const changed = structuredClone(metadata);
        if (duplicate) changed.features[1] = changed.features[0];
        else [changed.features[1], changed.features[2]] = [changed.features[2], changed.features[1]];
        assert.throws(() => validate(changed, engine, independent), /Ordered feature vocabulary/);
    }
});

test('Missing and non-finite coefficients fail before source comparison', () => {
    const short = structuredClone(metadata); short.selected.coefficients.pop();
    assert.throws(() => validate(short, engine, independent), /Coefficient arity/);
    const invalid = structuredClone(metadata); invalid.selected.coefficients[0] = NaN;
    assert.throws(() => validate(invalid, engine, independent), /Finite selected coefficients/);
});

test('The selected regularization and experiment must remain consistent', () => {
    for (const lambda of [0, -1, Infinity, 1000000]) {
        const changed = structuredClone(metadata); changed.selected.lambda = lambda;
        assert.throws(() => validate(changed, engine, independent));
    }
    const changed = structuredClone(metadata); changed.experiments[0].coefficients[0] += 1;
    assert.throws(() => validate(changed, engine, independent), /Selected experiment matches/);
});

test('Changing either source coefficient vector is detected independently', () => {
    const first = String(metadata.selected.coefficients[0]);
    const alteredEngine = engine.replace(first, '0');
    const alteredIndependent = independent.replace(first, '0');
    assert.notEqual(alteredEngine, engine); assert.notEqual(alteredIndependent, independent);
    assert.throws(() => validate(metadata, alteredEngine, independent), /Engine coefficients/);
    assert.throws(() => validate(metadata, engine, alteredIndependent), /Independent coefficients/);
});

test('The artifact remains research data and both source identities are required', () => {
    const promoted = structuredClone(metadata); promoted.status = 'ACTIVE';
    assert.throws(() => validate(promoted, engine, independent));
    assert.throws(() => validate(metadata, engine.replace('TRAINING_DIGEST', 'MISSING_DIGEST'), independent), /training identity/);
    assert.throws(() => validate(metadata, engine, independent.replace('TRAINING_DIGEST', 'MISSING_DIGEST')), /Shared training identity/);
});
