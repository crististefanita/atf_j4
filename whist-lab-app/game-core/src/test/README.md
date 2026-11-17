# WHIST engine tests

## Overview

The tests check the rules, sessions and policies of the observation allowed to the actor. [Common strategy](../../../../docs/testing/README.md) has the levels and criteria; this catalogue delimits the engine collections and their cost.

## Collections and oracles

| Familie | Property and limit |
|---|---|
| Rules and score | Legal cards, winners, contracts, series and invalid neighbours, with independent expectations. |
| Session and restoration | Orders, unique hand advancement, historical and refusal of inconsistent states. |
| A.I. policy | The actor's observation, legality, necessity and justified choices in controlled areas. |
| Playing for zero | The final score in both Romanian variants, the transition of AI to exact zero and the maintenance of the usual strategy when second place remains possible. |
| Cercetare | Experimental interference and sequelae, separated from the implicit regression. |
| Offline evaluation | Rolls, sources and lots identified; strategic progress has its own protocol and reserved data. |

## Choice of execution

[Project collections](../../../../docs/testing/ATF_Collections.md) choose the tasks of [Gradle](../../build.gradle). [Intrarea JVM](../../../algorithm-build/README.md) allows checks without Android SDK; use temporary copies and external output.

A small regression fixes the rule and the distinct defect without running the entire training corpus again. Research, arena and reserved evaluation shall be explicitly selected and shall not be gathered at regression as unique new tests.

## Outcome interpretation

The fairness of a rule, the cost of a decision and the superiority of policy are different qualifications. [Learning protocol](../../../../docs/learning/README.md) maintains the provenance, selection and promotion criteria; a rollout or a finite certificate does not guarantee each game's winning.

## Game evaluation at 0

Pregnancy `:game-core:zeroMatchPairedTest` explicitly runs full games on JVM. The pair shall retain the seats, series, distribution, order of hands and the trunks, with the rule disabled and activated.

| Parametru Gradle | Rol |
|---|---|
| `whist.zero.firstSeed`, `whist.zero.repeats` | The deterministic range of distributions for each combination of variant and number of places. |
| `whist.zero.pressure` | Choose the controlled prefix lot, where a place receives legal adverse bids before the last five hands. Natural and controlled lots shall be interpreted separately. |
| `whist.zero.allHard` | Pick a natural lot with Hard at all places. The option is used separately from the controlled prefix. |
| `whist.zero.output`, `whist.zero.reference` | Write the JSON pairs into a new folder and face control with the frozen reference when available. |
| `whist.auditOutput`, `whist.auditSource` | Extent destination and identification of the execution source. |

The JSON retains public moves, scores, zero choices and distribution prints. The final comparison separates the development and reserved intervals.

[Motorul](../../README.md) · [Common strategy](../../../../docs/testing/README.md) · [Design of AI components](../../../../docs/testing/ATF_AiComponents.md)
