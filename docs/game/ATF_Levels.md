# Normal and Hard

## Overview

[AiDifficulty](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/config/AiDifficulty.java) defines game levels and [AiPolicy](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/policy/AiPolicy.java) selects implementation. Difficulty changes the decision of the AI in the same rule contract.

## Liabilities

```mermaid
flowchart LR
    OBS[Private and public observation] -->|select through AiPolicy| LEVEL[Level]
    LEVEL -->|Normal| NORMAL[Local policy]
    LEVEL -->|HARD| HARD[Simulation and certificates]
    NORMAL -->|propune| LEGAL[Engine validated action]
    HARD -->|propune| LEGAL
```

<details>
<summary>Legend: difficulty does not alter legality</summary>

Both policies use the same variant and the permitted observation. The engine checks the action before changing the condition; no difficulty receives the actual hands of the opponents.

</details>

## Obiective

| Policy | Comportament |
|---|---|
| Normal at bidding | Estimate the power of your own hand and choose an allowed bid. |
| Normal before contract | Search for the tricks still needed and keep future chances when the current trick cannot be taken. |
| Normal after contract | Avoid additional levatas and download future risk within legal limits. |
| Hard with recoverable contract | Prioritize the estimated chance of exact, own points and the score of the main rival. |
| Hard drive with public impossibility | Protect first the estimated own points, then track the estimated adverse failures. |
| Normal and Hard with playing for zero | They switch to targeting exactly zero once public score bounds rule out a top-two finish through conventional scoring and zero is still reachable in the remaining schedule. |

[Modelul deciziei](../ai/ATF_DecisionModel.md#priority-objectives) holds the order of criteria, domains and runtime certificates. [Manifesto of objectives](../../config/whist-play-objectives.yaml) links the intention of implementation and components; exact parameters and conditions are read from the code.

## Information and adaptation

- The requirements of each place shall be recalculated after the levatates have been completed.
- The cards, the cards played, the table and the card turned are excluded from the hypothetical poor polo.
- Demonstrated color gaps narrow down the feasible hands.
- Known bids are contract targets in simulated continuances.
- La 2–8 cards, general weighting after adverse behaviour remains research; the bid of a card already has the specialized conditioning model and the fallback described in the [modelul deciziei](../ai/ATF_DecisionModel.md#estimate-of-alternatives).
- A Monte Carlo estimate and a finished certificate have different levels of certainty.

## Compatibilitate

The difficulty parser converts the old name EXPERT into HARD without a third menu option. Unknown names and invalid checkpoints are treated by explicit validations of persistence.

[Contract tests](../../whist-lab-app/game-core/src/test/java/ro/whistlab/core/ai/policy/DifficultyContractTest.java) checks the enumeration, the display and migration. [Collections](../testing/ATF_Collections.md) distinguish the domain regressions from the comparison of policies and the integration of Android.

[Modelul deciziei](../ai/ATF_DecisionModel.md) formalises the observation, estimation and order of objectives.
