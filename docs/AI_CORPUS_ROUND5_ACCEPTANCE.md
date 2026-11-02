# Round 5 — rejected candidates, accepted test organization

Run: C:/work/whist_back/ai-training/20261102-184455-round5. Romanian four-player profile, series +10/-5. Development seeds 5500..5507, all four hero seats, two fixed V4 opponent families, both policies. Each candidate ran 128 measured JUnit pairs / 256 complete games, independently audited card-by-card and score-by-score. All gameplay oracle invocations passed. Policy progression is a separate gate.

| Candidate | Change | Expert point / win-share delta | Independent point / win-share delta | Decision |
|---|---|---|---|---|
| large01 | Fitted opponent-bid prior + near-tie card denial | +81 / +1.5 | -138 / 0 | Reject |
| large02 | Fitted prior only | -185 / -4 | -86 / +0.5 | Reject |
| large03 | Near-tie card denial only | +12 / +2 | +8 / -2.5 | Reject |
| large04 | Doubled sampled worlds for bidding and play, sizes 2–8 | +173 / 0 | +7 / +0.5 | Reject overall; Expert loses one fixed-family gate |

Each role must have no fixed-family win-share regression, no mean score drop over two points/game, and a strict aggregate gain including positive 2–8-card point contribution. Aggregate improvements cannot hide a losing family. One-card contributions were unchanged. The training-only prior MAE improvement (0.8671 to 0.4861) did not justify deploying it. Research source is isolated in ai-lab/experiments; the production policies are restored byte-for-byte from the accepted V4 snapshot. No new strength claim is made.

There are 1,024 new retained games. The complete corpus validation passed for 3,185 games, 125 distinct deals and 1,197,560 played cards. These experiments reuse eight development deals with rotated roles/seats, not 1,024 independent random deals. Heldout 81000..81015 was never opened because no complete candidate passed development.

Accepted work: named/time-scoped collections, isolated runner, development-only casebook (14 examples including worst losses and every size 2–8), bilingual Help/docs with 32 executable examples, and consumer-only J16 input selection/budgets/reader context. Default regression excludes learning; best/worst teaching cases are reviewed before becoming independently specified regression tests.

Final verification: 375 offline JVM regression cases passed, 105 quick cases passed through the new runner, 16 selected J16 consumer/screenshot contracts passed, 11 Node corpus/evaluation contracts passed. APK built with stable V4 and RO/EN help; not installed or device-tested in this run. Menu E2E RO/EN additions compile and remain opt-in.

Reports: documentation/how-to-play-extent-j16.html (32 invocations, 100 comparisons, 2,862,580 bytes) and large03/selected-development-extent-j16.html (5 of 129 invocations, 2,138 comparisons, 4,739,578 bytes). The latter preserves the rejected candidate gate FAIL. All omitted IDs and the scope distinction are inline. The complete JSON receipts remain external archival/training data, not a hidden dependency for auditing the included examples.

Physical J16 SHA-256: 02b798a5848d5167feca16a0977f4f30b7047bb49ea666200aa7d0ea11731367. Static DOM checks pass; browser rendering remains pending after TAB MCP rejected file navigation. No alternate browser/server workaround was used. Legacy resume/reporting gaps remain tracked in the J4 feedback file; no Reporting implementation was changed.
