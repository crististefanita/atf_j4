# Independent player for adversarial tests

This source folder is compiled only as test support: game-core adds it to its
test source set; atf-test-android adds it through build-helper generate-test-sources.
It is not an Android app module and is not packaged into the APK.

`IndependentPlayer.Position` can contain only the player's own cards and public
facts: remaining hand sizes, bids, tricks, cumulative scores, current/history
cards, turned card and series configuration/counters. Opponents' private card
values and the game shuffle seed have no fields. `WhistChallenger` is the JSON
redaction boundary; permutation tests verify this boundary.

The planner implements Romanian legality, winners and score evaluation using
integer cards, without importing game-core. It compares counterfactual bids on
24 possible deals and card choices on 48. It uses public void inference and a
greedy rollout opponent. Series rewards affect score utility; they do not
arbitrarily change sampled worlds in otherwise identical public positions.

The accepted V3 player keeps those V2 decisions. After 64 failed rejection
attempts, a Hall-feasible sampler recovers a valid constrained deal. A real-engine
late-hand fixture proves the old sampler can exhaust all attempts while the
new fallback succeeds. Hall conditions also match exhaustive allocation on
2,000 small constrained worlds.

Exact one-card probability and small-endgame MaxN helpers remain offline
diagnostics. Six development experiments did not establish a strength gain for
this player; their policy changes were removed from active decisions and their
complete JSON games retained. The production Expert improved separately. See
[V3 results and corpus](../../docs/AI_CORPUS_ROUND3_ACCEPTANCE.md).

This is a declared independent benchmark opponent, not a perfect player. Its
classic random stream is checked against the original record hashes, and the
paired tournaments retain wins, ties and losses. See the
[evaluation protocol](../../docs/AI_TRAINING_PROTOCOL.md).

## V4 accepted one-card bids

The current policy conditions one-card bids on already-public bids, using its
own numerical posterior implementation and no game-core imports. A forced
last bid is uninformative. Training uses development games only. The frozen V3
player remains the reference; the V3 paragraphs above describe that history.
On fresh deals V4 gains242points and5win shares over128paired games, with both
fixed opponent families passing. See [V4 acceptance](../../docs/AI_CORPUS_ROUND4_ACCEPTANCE.md).

R6: late-card rank correction is shared in behavior with the Expert through a separate implementation; public-bid statistical weights failed independent evaluation and remain test-only. See ../../docs/AI_ROUND6_ANALYSIS.md. Default regression excludes research/mining tournaments.
