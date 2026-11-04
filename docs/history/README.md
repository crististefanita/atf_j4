# Dovezi istorice

## Pe scurt

Rezultatele închise descriu sursele și configurația folosite la execuție.
Ele sunt păstrate în depozitul extern de dovezi, separat de ghidurile curente.

## Arhiva documentelor

Înaintea reorganizării, documentele au fost capturate în snapshot-ul de
guvernanță și păstrate cu SHA-256 în `documentation-governance`.
Manifestul `history-snapshot.json` leagă numele documentului, conținutul original
și locația din arhivă.

Configurați `WHIST_EVIDENCE_ROOT` către depozitul de dovezi al stației.
Pentru această reorganizare, cheia arhivei este
`documentation-governance/20261104-doc-refresh/history-snapshot.json`.

```powershell
$manifest = Join-Path $env:WHIST_EVIDENCE_ROOT 'documentation-governance/20261104-doc-refresh/history-snapshot.json'
Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json
```

## Interpretare

- O acceptanță istorică nu dovedește comportamentul unui APK sau algoritm nou.
- O configurație încercată și respinsă rămâne respinsă, chiar dacă unele teste
  de componentă ale mecanismului au trecut.
- Un raport offline nu conține acțiuni Android noi; un pilot UI nu dovedește
  terminarea unei partide complete.
- Cele mai recente rapoarte se consultă prin
  [README-ul raportării](../feedback/whist-extent/README.md).
- Contractele curente sunt în [indexul documentației](../README.md).
