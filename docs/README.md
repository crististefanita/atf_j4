# Whist Lab documentation

## Overview

The documentation explains the rules, the architecture of the AI players and how we develop and verify them.
Each area has one introductory README and supporting guides with explicit owners and sources.

## Documentation areas

| Area | Main question |
|---|---|
| [Game](game/README.md) | What are the rules, variants, difficulty levels and scoring data? |
| [AI players](ai/README.md) | How do players turn the information they are allowed to see into a decision? |
| [Offline learning](learning/README.md) | How do we use the corpus, comparisons and holdout evaluation? |
| [Testing](testing/README.md) | Which test layer, collection and oracle verify the change? |
| [Reporting](reporting/README.md) | How do we retain results and verify that they were published? |
| [Platform](platform/README.md) | How do we integrate binary libraries and deliver the Android app? |
| [Governance](governance/README.md) | How do we maintain the quality of code, documentation and changes? |
| [Feedback](feedback/README.md) | Which gaps or qualifications remain open, and who owns them? |

## Common workflows

- For a game rule, check the variant profile, the engine, the independent component test and the bilingual help.
- For an AI policy, check [how it is built and improved](learning/ATF_PolicyEvolution.md), the allowed observation, the decision model, the local regression and the evaluation protocol.
- For another game, check the [transfer inventory and domain acceptance](learning/ATF_GameTransfer.md), the independent oracle, the adapter and the components before training.
- For an Android change, identify the app and check the components, the on-device scenario and the captures of the same state.
- For a reorganization, check the document owner, the references in code and documentation, and then governance.

## Sources and boundaries

The [project README](../README.md) introduces the product and modules. Guides point to the code, POM or test that owns the claim. Run data and editorial assessments live in the external archive.

The [documentation practices](governance/ATF_Documentation.md) cover writing, organization and review. Each stable rule has one owning document. The feedback register tracks the remaining gaps.
