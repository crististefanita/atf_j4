# Uneltele intrărilor pentru publicare

## Pe scurt

Aceste unelte Node.js aparțin consumatorului raportării. Ele validează identitatea și completitudinea înregistrărilor capturate, apoi selectează invocări întregi fără a recalcula rezultatul jocului.

## Contract și verificare

[whist-report-input.mjs](whist-report-input.mjs) deține selecția și inventarul populației native. [Testele sale](whist-report-input.test.mjs) verifică identități duplicate, surse contradictorii, omisiuni și păstrarea eșecurilor declarate.

Din rădăcina proiectului:

```powershell
node --test atf-tests/tools/whist-report-input.test.mjs
```

Datele selectate rămân aceleași obiecte evaluate, în ordinea declarată. [Politica prezentării](../../docs/reporting/ATF_Presentation.md) și [auditul publicării](../../docs/reporting/ATF_Audit.md) definesc etapele ulterioare.

[Modulul de integrare](../README.md) · [Laboratorul AI](../../whist-lab-app/ai-lab/README.md)
