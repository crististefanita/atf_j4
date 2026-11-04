# Feedback Whist Lab

Acest index păstrează lacunele de produs și de testare ale Whist Lab. J4
deține regulile, AI-ul, salvările, UI-ul, acțiunile Android și integrarea
consumerului cu rapoartele.

## Proprietatea schimbărilor

Defectele neutre Core sau Reporting, inclusiv contractul HTML și rendererul
Extent/fallback, aparțin J0 și se înregistrează în
[feedback-ul J0](../../../atf_j0/docs/feedback/README.md). J4 păstrează
așteptările proprii jocului și verifică raportul consumatorului cu acțiuni,
stări, aserțiuni și capturi reale. Testele și feedback-ul despre Contract
Whist sunt în [J5](../../../atf_j5/README.md).

## Feedback activ

| Subiect | Document |
|---|---|
| Claritatea dovezii de joc în rapoartele Whist Lab | [Așteptări consumer Android](ATF_Android_Alignment_Feedback.md) |
| Pointer stabil și handoff pentru ultimul raport Whist Lab | [Handoff UI](UI_REPORT_HANDOFF_Feedback.md) |
| Meniu, atu, cărți și istoricul scorului | [Utilizarea aplicației](WHIST_LAB_User_Experience_Feedback.md) |
| Contractele meniului și registrul partidei | [Criterii de integrare](MENU_AND_SCORE_HISTORY_Feedback.md) |
| Modele AI experimentale și limitele lor | [Cercetarea AI](AI_PUBLIC_BELIEFS_R15_R16_Feedback.md) |
| Colecții, input și criterii de acceptanță AI | [Feedback R11](AI_LOSS11_Feedback.md), [feedback R13/R14](AI_CONTRACT_PRESSURE_R13_Feedback.md), [feedback R12](AI_ROUND12_Feedback.md) |

[README-ul rapoartelor Whist Lab](../reporting/README.md) leagă ultima
acceptanță și încercările. [Indexul documentației](../README.md) leagă
contractele curente de implementare și teste.

## Închiderea unei cerințe

- Reproducerea numește versiunea, profilul și intrările afectate.
- Testul păstrează un expected independent și cazul negativ relevant.
- Rezultatul separă componentele, build-ul APK, interacțiunea UI, auditul
  capturilor și obiectivul jocului.
- Un raport nou nu rescrie o încercare anterioară și nu transformă lipsa de
  dovadă într-un PASS.
- Schimbările concurente se reconciliază înainte de promovarea surselor.