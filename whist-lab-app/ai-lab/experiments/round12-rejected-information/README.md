# R12 information-model experiments — rejected

The accepted runtime remains R11. All final candidate sources and frozen baselines are archived at `C:/work/whist_back/ai-training/20261103-r12/candidates/selected-source/whist-lab-app`.

The final candidate passed 527 native components, two sampling-budget controls and quick 163. On 512 fresh complete games, both roles failed the unchanged final SAFETY criterion. Expert: +30 points / −0.5 win share / +2 exact contracts. Independent: +24 / −7 / −4. No candidate is activated.

`informationResearchTest` contains seven small, test-only counterexamples for common information and common opening decisions. It is opt-in and excluded from the default regression. The numeric response oracle is separate in `scripts/whist-response-casebook.mjs`; the complete hypothetical world is analysis evidence, never real runtime input.

See [the full analysis](../../../../docs/AI_ROUND12_ANALYSIS.md) and [feedback](../../../../docs/feedback/AI_ROUND12_Feedback.md). The consumed heldout range 108000–108015 must not become training data or be called new evaluation again.
