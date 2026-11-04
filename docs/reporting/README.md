# Rapoarte și limitele dovezilor

## Pe scurt

Raportul Extent trebuie să explice scopul, intrările, regulile verificate și
rezultatul observat fără acces la surse. Acceptanța componentelor, execuția
Android și verificarea randării sunt rezultate distincte.

## Unde se găsește raportul curent

În exemplele PowerShell, indicați depozitul extern prin `WHIST_EVIDENCE_ROOT`. Rădăcina folosită
de publicatorul UI se poate configura separat prin `ATF_J4_UI_REPORTS` sau
proprietatea `atf.android.handoffRoot`.

| Pointer în depozitul extern | Conținut |
|---|---|
| `ui-reports/whist-lab/LATEST.md` | Ultima acceptanță și ultima încercare UI, cu linkuri către HTML și metadate. |
| `ui-reports/whist-lab/latest-accepted.html` | Scurtătura stabilă către ultimul HTML acceptat în scopul său declarat. |
| `ui-reports/whist-lab/scopes/<scope>/latest-accepted.json` | Acceptanța pentru scopul precis; absența ei nu este PASS. |
| `ui-reports/whist-lab/scopes/<scope>/latest-attempt.json` | Ultima încercare în acel scop, inclusiv eșecul sau refuzul execuției. |
| `ai-training/LATEST_ALGORITHM_REPORT.json` | Ultima comparație între strategii, cu rezultate separate pentru joc și randare. |
| `ai-training/LATEST_COMPONENT_REPORT.json` | Ultimul raport de componente publicat, SHA-256, numărători și limitele verificării browser. |
| `ui-reports/whist-lab/runs/<runId>/metadata.json` | Pachet, APK, surse, biblioteci, timp, populație și verdictul rulării UI. |

```powershell
$ui = Join-Path $env:WHIST_EVIDENCE_ROOT 'ui-reports/whist-lab'
Get-Content -LiteralPath (Join-Path $ui 'LATEST.md')
Get-Content -LiteralPath (Join-Path $env:WHIST_EVIDENCE_ROOT 'ai-training/LATEST_COMPONENT_REPORT.json') -Raw | ConvertFrom-Json
```

[Indexul UI actualizat de publicator](../feedback/whist-extent/README.md)
rămâne punctul de intrare pentru capturile Android.
Un raport de layout sau un pilot de câteva mâini nu certifică terminarea
unei partide românești complete și nici puterea actuală a AI-ului.

## Cum se citește o acceptanță

- Identificați pachetul aplicației, profilul, dificultatea, numărul de jucători
  și bonusul de serie; originalul și Whist Lab au identități separate.
- Verificați selecția și populația: metode JUnit, invocări parametrizate,
  partide, perechi și comparații sunt unități diferite.
- Citiți expected, actual și verdictul capturat în fiecare invocare selectată.
- Consultați limitele: surse înghețate, salvări migrate, date lipsă și diferența
  dintre execuție nouă și republicarea unor capturi anterioare.
- Verificați separat auditul datelor și auditul interactiv al aceluiași HTML,
  legate prin SHA-256.

## Un raport compact și auditabil

Selecția este configurată înaintea publicării și păstrează invocări întregi.
Toate eșecurile și criteriile de acceptare rămân vizibile, împreună cu
inventarul populației și motivele omiterii exemplelor repetitive.

Pozele Android sunt PNG-urile originale încorporate Base64 în HTML.
Componentele offline folosesc intrări, stări și comparații; ele nu produc
capturi de telefon și nu sunt prezentate ca execuții UI.

[Politica de detaliu](../ATF_Game_Report_Presentation.md), [contractul dovezilor](../ATF_Android_Report_Contract.md) și [handoff-ul UI](../WHIST_UI_REPORT_HANDOFF.md) descriu mecanismul.

[Feedback-ul](../feedback/README.md) stabilește responsabilitățile pentru intrarea raportului și biblioteca binară.

## Evaluarea offline a deciziilor la 2–8 cărți

Raportul compară trei variante ale jucătorilor pe 320 de perechi de partide. Cele trei criterii de promovare eșuate se pot inspecta alături de exemplele de componentă și inventarul celor 640 de partide reluate.

[Deschide raportul Extent](../../../../whist_back/ai-training/20261104-r18-loss-focus/final-reports/final-algorithm-extent-report.html)
