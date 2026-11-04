# Honors, low-card covers and contract safety

This guide explains strategy hypotheses for Romanian Whist. Mandatory rules are in [How to play](WHIST_EN.md).

## Catalog and producing code

| Source | Responsibility |
|---|---|
| [YAML catalog](../../config/whist-hand-patterns.yaml) | 19 holding families, required context and each hint's status |
| [Holding extraction](../../scripts/whist-hand-patterns.mjs) | `describeHolding`, `PATTERNS` and the `catalog` generator |
| [Calibration](../../scripts/whist-heuristic-calibrate.mjs) | Descriptive observations from validated development games |
| [Context manifest](../../config/whist-contextual-discard.yaml) | Rules and tests for quota, discards and the current-table trump |

The catalog declares `RESEARCH_CATALOG_NOT_RUNTIME_CONFIGURATION` and `runtime_prior_influence: 0`. Editing its weights does not automatically change the AI.

## What a low cover means

A low card in the same suit can absorb a round while opponents consume higher honors. Promotion depends on the lead, entries, distribution and ruffing opportunities.

| Holding | Useful hypothesis | Limitation |
|---|---|---|
| K + low | The king may become a winner after the ace | The ace may be retained; the king may be ruffed or discarded |
| Q + two lows | The queen may survive the consumption of A/K | Higher honors may fall together or in separate rounds |
| J + three lows / 10 + four lows | Promotion may occur in a long suit | Required rounds or entries may be unavailable |
| Side A | Highest rank of its suit | It may be ruffed or discarded to avoid a trick |
| Trump A | Wins the trick in which it is played | Other cards may produce unwanted tricks |
| Trump K after public removal of A | Highest remaining trump | An ace still on the unfinished table remains active |
| A-K / A-K-Q | Evaluate the sequence jointly | Do not add separate K/Q hints to the sequence again |
| Low trumps and a side void | Potential ruffs | Mandatory trump may force an unwanted trick |

- Simple K+x and Q+xx families exclude certain higher own honor sequences.
- `HIGHEST_CARD_CAPTURE` describes capture with the highest honor.
- `SUIT_TRICKS` describes tricks won in the suit, a different quantity.
- `trick_hint` is contextual and is not independently additive across honors.

## Available context

| Stage | Observable information |
|---|---|
| Deal | Own cards, player count, hand size and the turned card |
| Bidding | Only the already submitted bid prefix |
| Play | All contracts, public history, current table, remaining needs and scores |
| Small ending | Feasible worlds and legal continuations, when enumeration is complete |

- With 3–6 players, the minimum rank is `15 − 2 × players`.
- Own and public cards cannot be assigned to fictional opponent holdings.
- A turned trump is undealt; a trump still on the table can win the current trick.
- Signed remaining need is `bid − completed tricks won`.
- Safety targets the exact contract, including zero.

## What calibration measures

The [calibrator](../../scripts/whist-heuristic-calibrate.mjs) accepts games from the development partition only. Actual game and context counts belong to each execution, not to an engine constant.

1. Validate each game's rules and score.
2. Deduplicate identical action traces.
3. Group observations by initial world, seat and suit.
4. Average policy observations inside each group before aggregation.
5. Apply evidence weight `n / (n + 20)`.
6. Export at most 128 cells supported by at least 20 worlds each.

An observed king-capture rate depends on the player's policy, including deliberate discards. It is not an intrinsic or counterfactual win probability for K+x.

```powershell
node scripts/whist-hand-patterns-cli.mjs catalog <fresh-output.yaml>
node --test scripts/whist-hand-patterns.test.mjs scripts/whist-heuristic-calibrate.test.mjs
```

## Relationship to the current engine

- [Hard](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/MonteCarloAi.java) evaluates cards through its model and bounded public guards.
- The [contract certificate](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ContractGuarantee.java) checks two-own-card endings and all continuations within its limits.
- [Public salvage](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/PublicSalvage.java) requires an impossible contract and dominance without worsening own points.
- The [independent player](../../whist-lab-app/ai-lab/src/main/java/ro/whistlab/coach/IndependentPlayer.java) has a separate implementation and utility.

In the [contextual cases](../AI_CONTEXTUAL_DISCARD.md), the same AS/7H calls for discarding the ace at zero need and retaining it at need one. A lower card is not automatically safer.

Activating a new hint requires independently justified components, frozen source and a separate evaluation on games unused for development. The catalog makes no perfect-play or exhaustive-state-space claim.
