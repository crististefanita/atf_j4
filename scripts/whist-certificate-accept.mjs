import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';

export const certificateControls = [
    'certified-contract-observed-regression', 'certified-contract-no-guarantee-fallback',
    'certified-contract-impossible-fallback', 'certified-contract-budget-unknown',
    'world-completeness-pool8', 'world-completeness-pool9', 'world-completeness-pool10',
    'world-budget-complete-fallback'
];

/** Existing local-correctness workflow, scoped to a universally certified contract fix.
 * A failed strict progression result is retained; it is never renamed PASS here. */
export function auditCertificateFix(summary, captures, native, plan) {
    assert.equal(plan.kind, 'CERTIFIED_LOCAL_CORRECTNESS');
    assert.equal(plan.version, 11);
    assert.deepEqual([...plan.componentIds].sort(), [...certificateControls].sort());
    assert.ok(native.tests >= 520 && native.failures === 0 && native.errors === 0 && native.skipped === 0,
        'Complete native regression required');
    const ids = new Set();
    for (const r of captures) {
        assert.ok(certificateControls.includes(r.invocationId) && !ids.has(r.invocationId)); ids.add(r.invocationId);
        assert.equal(r.status, 'PASS');
        assert.ok(r.checkpoints?.length && r.checkpoints.every(c => c.comparisons?.length &&
            c.comparisons.every(a => a.passed === true && typeof a.name === 'string' &&
                Object.hasOwn(a, 'expected') && Object.hasOwn(a, 'actual'))), 'Missing evaluated control');
    }
    assert.equal(ids.size, certificateControls.length, 'Missing certificate control');
    const roles = plan.roles ?? ['expert', 'player'];
    assert.ok(roles.length && new Set(roles).size === roles.length && roles.every(r => ['expert', 'player'].includes(r)));
    assert.deepEqual(plan.opponents, ['EXPERT_V10', 'PLAYER_V8']);
    const expected = plan.heldout.seeds * 4, families = new Set(), totals = {};
    for (const role of roles) totals[role] = {pointDelta: 0, winShareDelta: 0, exactDelta: 0, localAccepted: true};
    assert.equal(summary.phase, 'heldout'); assert.equal(summary.families.length, roles.length * 2);
    for (const f of summary.families) {
        const key = f.role + '/' + f.opponent;
        assert.ok(roles.includes(f.role) && plan.opponents.includes(f.opponent) && !families.has(key)); families.add(key);
        assert.equal(f.pairs, expected);
        for (const k of ['beforePoints', 'afterPoints', 'beforeWinShare', 'afterWinShare', 'largeHandExactBefore', 'largeHandExactAfter'])
            assert.ok(Number.isFinite(f[k]), 'Invalid family measurement');
        for (const k of ['largeHandExactBefore', 'largeHandExactAfter'])
            assert.ok(Number.isInteger(f[k]) && f[k] >= 0 && f[k] <= expected * 16, 'Impossible exact count');
        const pointDelta = f.afterPoints - f.beforePoints, winShareDelta = f.afterWinShare - f.beforeWinShare;
        const exactDelta = f.largeHandExactAfter - f.largeHandExactBefore, r = totals[f.role];
        r.pointDelta += pointDelta; r.winShareDelta += winShareDelta; r.exactDelta += exactDelta;
        r.localAccepted &&= pointDelta >= 0 && winShareDelta >= -1e-9 && exactDelta >= 0;
    }
    assert.equal(summary.pairs, expected * roles.length * 2);
    for (const [role, r] of Object.entries(totals)) {
        assert.equal(r.pointDelta, summary.roles[role].pointDelta);
        assert.ok(Math.abs(r.winShareDelta - summary.roles[role].winShareDelta) < 1e-9);
        assert.equal(r.exactDelta, summary.roles[role].largeHandExactDelta);
        r.strictStrengthAccepted = summary.roles[role].accepted === true;
        r.strengthImprovementClaimed = r.strictStrengthAccepted;
    }
    return {accepted: Object.values(totals).every(r => r.localAccepted), roles: totals,
        strictProgressionStatus: summary.status, controls: ids.size, pairs: summary.pairs,
        scope: 'Certified local correctness and no observed family regression on this finite cohort. Strict progression remains a separate unchanged verdict; flat local acceptance is not a claim of globally stronger play.'};
}

if (typeof process !== 'undefined' && process.argv[1]?.replaceAll('\\', '/').endsWith('/whist-certificate-accept.mjs')) {
    const [folder, components, nativeFile, planFile] = process.argv.slice(2);
    const startedAt = new Date().toISOString(), begin = performance.now();
    try {
        const parse = async file => JSON.parse((await fs.readFile(file, 'utf8')).replace(/^\uFEFF/, ''));
        const summary = await parse(path.join(folder, 'heldout-summary.json')), native = await parse(nativeFile), plan = await parse(planFile);
        const captures = [];
        for (const id of certificateControls) captures.push(await parse(path.join(components, id + '.json')));
        const result = auditCertificateFix(summary, captures, native, plan);
        const comparisons = [{name: 'All independently specified certificate/budget controls passed', expected: 8, actual: result.controls, passed: result.controls === 8}];
        for (const f of summary.families) for (const [label, before, after] of [
            ['points', f.beforePoints, f.afterPoints], ['win share', f.beforeWinShare, f.afterWinShare],
            ['exact contracts2..8', f.largeHandExactBefore, f.largeHandExactAfter]])
            comparisons.push({name: f.role + '/' + f.opponent + ' — no observed ' + label + ' regression',
                expected: '>= ' + before, actual: after, passed: after + 1e-9 >= before});
        const receipt = {schemaVersion: 1, runId: 'guard18-final', invocationId: 'certified-local-correctness-gate',
            className: 'scripts.whist-certificate-accept', methodName: 'universalContractFixHasNoObservedFamilyRegression',
            parameters: {collection: 'verification', criterion: 'CERTIFIED_LOCAL_CORRECTNESS'}, executionKind: 'NODE_COMPONENT_GATE',
            startedAt, endedAt: new Date().toISOString(), durationNanos: Math.max(1, Math.round((performance.now() - begin) * 1e6)),
            status: result.accepted ? 'PASS' : 'FAIL', failure: result.accepted ? '' : 'Local correctness cohort regressed; preserve role decisions and do not claim full acceptance.',
            checkpoints: [{owner: 'certificate/local-acceptance', title: 'Certified bugfix acceptance; strict strength is separate',
                context: {result, native, requiredControls: certificateControls, plan,
                    strictSummarySha256: crypto.createHash('sha256').update(await fs.readFile(path.join(folder, 'heldout-summary.json'))).digest('hex')}, comparisons}], results: []};
        await fs.mkdir(path.join(folder, 'certificate-invocations'), {recursive: true});
        await fs.writeFile(path.join(folder, 'certificate-invocations', receipt.invocationId + '.json'), JSON.stringify(receipt, null, 2) + '\n', {flag: 'wx'});
        await fs.writeFile(path.join(folder, 'certificate-summary.json'), JSON.stringify(result, null, 2) + '\n', {flag: 'wx'});
        console.log(JSON.stringify(result, null, 2)); if (!result.accepted) process.exitCode = 2;
    } catch (error) { console.error(error.stack); process.exitCode = 1; }
}
