# Honors, low-card covers and contract safety

These are strategy hypotheses. The mandatory game rules are in [How to play](WHIST_EN.md). The machine-readable research catalog is [whist-hand-patterns.yaml](../../config/whist-hand-patterns.yaml).

A low card in the same suit can absorb a round while higher opposing honors are consumed, leaving one's king or queen promoted. K+x may produce a trick; Q+two lows may produce a trick in a longer hand. Neither is a guarantee. Higher honors may be retained, fall together, run in separate rounds, or be cut by trump. Entry, lead, suit length and the number of remaining rounds matter.

An off-trump ace is the highest rank in its suit, but can be ruffed or deliberately discarded. Own A-K or A-K-Q must be evaluated jointly: independent king/queen hints cannot be added to the ace again. An exposed or previously played trump ace promotes the remaining trump king, and wins the trick in which it is played. A low trump combined with a side void may create a ruff, including an unwanted one when mandatory trumping conflicts with a zero bid.

The catalog distinguishes 19 pattern families, including master trump, off-suit ace, singleton/covered king, covered honor sequences, short/covered queen, long jack/ten holdings, AK/AKQ, long/low trump, side voids and long no-trump suits. Simple K+x and Q+xx patterns exclude higher own honor sequences to avoid mixing different holding classes.

Context includes the reduced deck (minimum rank `15 − 2 × players`, for 3–6 players), fraction of the deck dealt, trump, higher own/public/unseen cards, low covers, suit length, voids, ruff risk, seat and lead. During bidding only the already submitted bid prefix is observable. During play all bids are known, along with tricks still needed, impossible contracts, opponents' needs and series state. Contract safety means maximizing the chance of the **exact bid**, including zero, rather than maximizing tricks.

The v2 calibration uses development only: 4,869 validated complete games, 2,551 distinct action traces and 6,516 contextual cells. It exports the 128 highest-support cells to YAML and retains the full JSONL inventory outside the project. Exact action traces are deduplicated; policy outcomes are averaged within initial-world/seat/suit/context clusters before worlds receive equal weight. Evidence weight is `n / (n + 20)`. `HIGHEST_CARD_CAPTURE` and `SUIT_TRICKS` have distinct meanings.

Observed outcomes are policy dependent, including deliberate ducking or discarding. A king's capture frequency is not an intrinsic or causal win probability for K+x. The catalog therefore remains **`NOT_RUNTIME_CONFIGURATION`, runtime influence 0**. Runtime use requires its own controlled ablation and unused evaluation, rather than importing descriptive weights directly into the player.

The [arena protocol and results](../AI_STRATEGY_ARENA.md) explain balanced seats, distinct strategies per player, separate promotion criteria and the rejected safety variant. This catalog makes no optimal-play or exhaustive state-space coverage claim.
## Verified lesson: avoiding an extra trick

After making the contract, the lower card is not automatically safer. In the R11 counterexample, bid 1 with 1 trick already won, QH + 10D and clubs trump, discarding 10D preserves the exact contract across all 96 feasible public worlds and every legal continuation; discarding QH can add an unwanted trick. This is a contextual proof, not a rule to always retain a queen. The guard applies only to completely enumerated small endgames. Missing proof or an exhausted budget preserves the previous planner. See the [R11 analysis](../AI_LOSS11_ACCEPTANCE.md).


## Bidding and available information — R12

An opening must be chosen before observing the hidden world. Picking a different opening inside each world and averaging afterwards can overstate the achievable contract. [R12](../AI_ROUND12_ANALYSIS.md) retains the counterexamples and rejected models. The current selectable levels are [Normal/Hard](../TWO_GAME_LEVELS.md). Hard retains the accepted engine; complete finite proofs remain distinct from estimated play quality.

## Remaining quota and certified salvage — R13/R14

Remaining need is bid minus completed tricks won; opportunities include the unfinished trick. Own/public cards and the turned card cannot appear in opponent holdings. Contextual pressure weights regressed and remain inactive; see [R13](../AI_CONTRACT_PRESSURE_R13.md).

Hard enables only the [R14 public dominance guard](../AI_CONTRACT_SALVAGE_R14.md): for an already impossible contract, change the card only when every compatible world and legal continuation preserves own points and at least as many opponent misses, with a strict improvement. The7S/7H ending raises rival misses2to3 at the same own penalty−4. Equal averages and exhausted searches never certify dominance. The random128-game evaluation had identical traces; this is a bounded feature correction, not general stronger-play evidence.
