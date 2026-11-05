# Honors, covers and contract safety

This guide describes general strategy principles for Romanian Whist. Mandatory rules belong to [How to play](../game/ATF_RulesEn.md), while the [learning architecture](README.md) defines how hypotheses become tested policy changes.

## Sources and responsibilities

| Source | Responsibility |
|---|---|
| [Pattern catalog](../../config/whist-hand-patterns.yaml) | Holding classes, required context, hint scope and research status. |
| [Pattern extraction](../../whist-lab-app/ai-lab/tools/patterns/whist-hand-patterns.mjs) | Derives holding descriptors from the actor's cards. |
| [Calibration](../../whist-lab-app/ai-lab/tools/patterns/whist-heuristic-calibrate.mjs) | Descriptive associations from validated development games. |
| [Context manifest](../../config/whist-contextual-discard.yaml) | Conditional rules, information boundaries, implementations and tests. |
| [Policy objectives](../../config/whist-play-objectives.yaml) | Active priorities and isolated experimental alternatives. |

## General principles

- Low covers can help consume higher ranks; promotion still depends on lead, entries, distribution and ruffing.
- Evaluate honor sequences jointly and avoid counting the same card through overlapping patterns.
- Distinguish card rank, suit length and the number of tricks a suit can produce.
- A side honor can be ruffed or legally discarded; its rank alone does not guarantee a trick.
- Mandatory trump can force an unwanted trick after the target has been reached.
- Turned cards, completed-trick cards and cards still on the table have different effects.
- Recompute signed remaining need and feasible opportunities after each public action.
- The table's aggregate auction imbalance never replaces an individual attainable contract.

## Information and inference

```mermaid
flowchart LR
    OWN[Own hand] -->|combines with| OBS[Legal observation]
    PUBLIC[Public prefix] -->|constrains| OBS
    OBS -->|supports| WORLDS[Compatible worlds]
    WORLDS -->|estimates| POLICY[Contract decisions]
```

<details>
<summary>Legend: observable information</summary>

Actual opponent hands and future actions are absent from the policy input. Legal deductions restrict possible worlds; bid-conditioned behavioral likelihood is a separate model hypothesis requiring qualification.

</details>

An observed capture frequency depends on the policy that generated it, including deliberate discards. It is not an intrinsic or counterfactual probability for the holding.

## YAML and policy changes

The [configuration contract](ATF_RulesConfiguration.md) owns manifest roles and activation status. The holding catalog has zero direct runtime influence; using a hint requires an explicit code consumer and a qualified domain.

- The [decision model](ATF_DecisionModel.md#estimarea-alternativelor) distinguishes the active one-card bid-conditioned model from general multi-card behavioral inference under research. A holding hint alone provides neither model.
- Development games, fitted outputs and evaluation results remain external. Versioned model parameters identify reproducible code; promotion follows the [offline protocol](../learning/README.md).

[Romanian guide](ATF_HandPatternsRo.md) · [Contextual decisions](ATF_ContextualPlay.md)
