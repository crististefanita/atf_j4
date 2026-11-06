# Uneltele intrărilor pentru publicare

## Pe scurt

Aceste unelte Node.js aparțin consumatorului raportării și guvernanței livrării. Ele validează identitatea înregistrărilor capturate, selectează invocări întregi și înregistrează integritatea dosarului fixat, fără a recalcula rezultatul jocului.

## Contract și verificare

[whist-report-input.mjs](whist-report-input.mjs) deține selecția și inventarul populației native. [Testele sale](whist-report-input.test.mjs) verifică identități duplicate, surse contradictorii, omisiuni și păstrarea eșecurilor declarate.

Din rădăcina proiectului:

```powershell
node --test atf-tests/tools/whist-report-input.test.mjs
```

Datele selectate rămân aceleași obiecte evaluate, în ordinea declarată. [Politica prezentării](../../docs/reporting/ATF_Presentation.md) și [auditul publicării](../../docs/reporting/ATF_Audit.md) definesc etapele ulterioare.

## Înregistrarea integrității livrării

[record-release-integrity.mjs](record-release-integrity.mjs) citește versiunea activă din index și înregistrează mărimea și SHA-256 pentru toate fișierele ei, fără manifestul autoreferențial. Refuză căile simbolice și schimbarea inventarului ori a manifestului în timpul pregătirii; scrierea folosește un fișier temporar și înlocuire atomică.

```powershell
node atf-tests/tools/record-release-integrity.mjs
```

`--project <root>` și `--version <version>` permit o rădăcină și o ediție explicite. Unealta nu execută teste și nu promovează calificarea; [verificatorul Java](../src/test/java/atf/tests/governance/documentation/release/integrity/ReleaseIntegrity.java) confruntă separat octeții și rezultatele JUnit. [Componentele manifestului](../src/test/java/atf/tests/governance/documentation/release/integrity/ManifestIntegrityComponentTest.java) exercită și lansatorul real pe fișiere temporare, inclusiv pe căi cu spații și diacritice.

[Modulul de integrare](../README.md) · [Laboratorul AI](../../whist-lab-app/ai-lab/README.md)
