# Feedback și responsabilități

## Pe scurt

Feedback-ul descrie o problemă reproductibilă, impactul asupra cititorului sau
jucătorului și condițiile concrete de închidere. Un document istoric rămâne
dovadă pentru versiunea sa, fără să devină automat o problemă curentă.

## Proprietatea schimbărilor

**`atf_j0` este owner pentru Core și biblioteca Reporting.** Problemele de
renderer, contracte neutre sau împachetare JAR se transmit acolo prin feedback;
implementarea și testele native ale bibliotecilor rămân la owner.

| Suprafață | Responsabilitatea acestui proiect |
|---|---|
| Reguli și AI | Motor, ipoteze YAML, regresii situaționale și evaluare pe partide noi. |
| Aplicație Android | Meniu, masă, istoric de scor, localizare și persistență. |
| Testare Android | Adaptor, acțiuni măsurate, capturi și oracole independente. |
| Intrarea raportului | Identități reale, comparații capturate, selecție, context și proveniență. |
| Consum binar | POM, perechea JAR/POM, proveniență și acceptanța integrării locale. |

## Feedback activ

| Subiect | Document |
|---|---|
| Claritatea raportului pentru un cititor extern | [Alinierea dovezilor](ATF_Android_Alignment_Feedback.md) |
| Publicarea ultimului raport pe scop | [Handoff UI](UI_REPORT_HANDOFF_Feedback.md) |
| Meniu, atu, cărți și istoricul scorului | [Utilizarea aplicației](WHIST_LAB_User_Experience_Feedback.md) |
| Eliminarea modurilor și registrul complet al licitațiilor | [Criterii de integrare](MENU_AND_SCORE_HISTORY_Feedback.md) |
| Siguranța continuării originalului | [Partida originală](ORIGINAL_ROMANIAN_LIVE_Feedback.md) |
| Modele experimentale respinse și limitele lor | [Cercetarea AI](AI_PUBLIC_BELIEFS_R15_R16_Feedback.md) |

[README-ul raportării](../reporting/README.md) explică unde se consultă ultimul
raport acceptat și ultima încercare. [Indexul documentației](../README.md) leagă
cerințele de ghidurile curente.

## Când se închide o cerință

- Reproducerea precizează intrările, profilul și versiunea afectată.
- Regresia verifică așteptări independente și păstrează cazul negativ relevant.
- Rezultatul precizează ce s-a executat și ce rămâne neverificat.
- Pentru UI, acceptanța funcțională, verificarea capturilor și auditul raportului
  au verdicte distincte, legate de același APK și aceiași bytes HTML.
- Schimbările concurente se reconciliază înaintea promovării; un snapshot
  verificat nu autorizează suprascrierea surselor care au evoluat între timp.
