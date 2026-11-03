import test from 'node:test';import assert from 'node:assert/strict';
import {auditCertificateFix, certificateControls} from './whist-certificate-accept.mjs';
const plan = {kind:'CERTIFIED_LOCAL_CORRECTNESS',version:11,componentIds:certificateControls,
    roles:['expert','player'],opponents:['EXPERT_V10','PLAYER_V8'],heldout:{seeds:16}};
const controls=()=>certificateControls.map(invocationId=>({invocationId,status:'PASS',checkpoints:[{comparisons:[{name:'Independent proof',expected:true,actual:true,passed:true}]}]}));
const native={tests:520,failures:0,errors:0,skipped:0};
const summary=()=>({phase:'heldout',status:'REGRESSION',pairs:256,
    families:plan.roles.flatMap(role=>plan.opponents.map(opponent=>({role,opponent,pairs:64,beforePoints:100,afterPoints:100,
        beforeWinShare:16,afterWinShare:16,largeHandExactBefore:600,largeHandExactAfter:600}))),
    roles:{expert:{pointDelta:0,winShareDelta:0,largeHandExactDelta:0,accepted:false},player:{pointDelta:0,winShareDelta:0,largeHandExactDelta:0,accepted:false}}});
test('A flat independently certified bugfix never becomes a strict stronger-play claim',()=>{
    const r=auditCertificateFix(summary(),controls(),native,plan);assert.equal(r.accepted,true);
    assert.equal(r.strictProgressionStatus,'REGRESSION');assert.equal(r.roles.player.strengthImprovementClaimed,false);
});
test('Local acceptance refuses a regression in any family instead of hiding it in the aggregate',()=>{
    const s=summary();s.families[0].afterPoints=99;s.roles.expert.pointDelta=-1;
    assert.equal(auditCertificateFix(s,controls(),native,plan).accepted,false);
    const w=summary();w.families[3].afterWinShare=15;w.roles.player.winShareDelta=-1;
    assert.equal(auditCertificateFix(w,controls(),native,plan).accepted,false);
});
test('More points cannot compensate for a lost exact contract',()=>{
    const s=summary();s.families[0].afterPoints=110;s.roles.expert.pointDelta=10;
    s.families[0].largeHandExactAfter=599;s.roles.expert.largeHandExactDelta=-1;
    assert.equal(auditCertificateFix(s,controls(),native,plan).accepted,false);
});
test('Missing, failed or duplicate controls cannot certify a local fix',()=>{
    assert.throws(()=>auditCertificateFix(summary(),controls().slice(1),native,plan));
    const bad=controls();bad[0].status='FAIL';assert.throws(()=>auditCertificateFix(summary(),bad,native,plan));
    const duplicate=controls();duplicate[1]=duplicate[0];assert.throws(()=>auditCertificateFix(summary(),duplicate,native,plan));
});
test('Wrong cohorts, impossible counts and inconsistent aggregates are rejected',()=>{
    const s=summary();s.families[0].pairs=63;assert.throws(()=>auditCertificateFix(s,controls(),native,plan));
    const e=summary();e.families[0].largeHandExactAfter=1025;assert.throws(()=>auditCertificateFix(e,controls(),native,plan));
    const a=summary();a.roles.player.pointDelta=1;assert.throws(()=>auditCertificateFix(a,controls(),native,plan));
    assert.throws(()=>auditCertificateFix(summary(),controls(),{...native,skipped:1},plan));
});
