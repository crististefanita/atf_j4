# Rejected bid-model research overlays

## Pe scurt

These sources preserve the corrected R15 and R16 experiments. They are outside every production and default test source set. The accepted Hard engine remains R14.

`config/whist-research-candidates.json` binds each overlay file to its target path and SHA-256, and guards the accepted baseline before use. The collection runner applies an explicitly selected overlay only in its temporary copy.

```powershell
.\scripts\run-whist-collection.ps1 -Collection regression -ResearchCandidate r16-bid-forecast -RunRoot <evidence-root>/collections\fresh-r16-qualification
```

Supported research selections are `r15-bid-only-v2`, `r15-bid-and-card` and `r16-bid-forecast`.

Learning additionally requires the matching protocol15/16 and a candidate ID equal to its declared prefix or beginning with that prefix plus a hyphen. Keep every run in a fresh external folder. Do not reuse consumed development or heldout seeds as fresh evidence.

R15A changes bid weighting; R15B also changes card weighting and separates estimator-version snapshots from physical utility bounds.

- R16 changes predictions of future unknown auction targets.
- All share the same observed-bid likelihood, its14 component tests and6 golden fixtures.
- The shared test uses R16's narrower description of the unchanged future-bid assertion: it checks the likelihood helper, not the whole chooseBid API.
- The learned model predicts recorded behavior; it does not establish optimal bids or calibrated hidden-card probabilities.

Native qualification and complete rules/scoring replay do not imply strength acceptance.

All three candidates were rejected by their prespecified development criteria. Historical qualification counts are506/506/510 respectively; these are reference counts, never a substitute for executing the current source.

The baseline guards cover all30 accepted policy/core sources.

Following the current-table trump quota correction, new temporary replays use that corrected base and include the later contextual regressions. They are new source identities; do not relabel the original R14-based results or expect the historical native counts to describe a new replay. The original full source archives and receipts remain in the external R15/R16 evidence folders.
