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

## Evaluarea RAG a candidatului

[Evaluatorul](evaluate-release-rag.mjs) confruntă planul, manifestul, payload-ul și snapshot-ul APK.

- [Perimetrul suitei](component-evidence-scope.mjs) stabilește când se poate reutiliza o execuție. [Pregătirea dovezilor](prepare-component-reuse.mjs) și evaluatorul folosesc aceleași verificări pentru receipt-uri și hash-uri. Lansatorul componentelor folosește această pregătire la cerere.
- Politica pe versiuni stabilește grupurile Maven și Gradle obligatorii. Fiecare grup are propriul Extent și receipt.

```powershell
$ProjectRoot = (Get-Location).Path
node .\atf-tests\tools\evaluate-release-rag.mjs `
  --project-root $ProjectRoot `
  --candidate-dir $CandidateDir `
  --source-root $SourceSnapshotDir `
  --plan-file $QualificationPlan `
  --output $VerdictFile
```

Verdictul este JSON cu citări prin cale, SHA-256 și JSON Pointer/linie. Singura scriere este `$VerdictFile`, în afara tuturor intrărilor.

- Codul `0` înseamnă `READY_FOR_HUMAN_REVIEW`; `2` înseamnă `BLOCKED`; `1` indică intrare sau invocare invalidă.
- Înaintea reviziei umane se cer receipt-uri legate prin hash pentru componente, build APK, identitate și semnare.
- Starea publicată cere și receipt pentru descărcarea anonimă.
- E2E neexecutat rămâne `NOT_RUN`; schimbarea unui flux utilizator cere grupuri E2E Extent trecute.

Evaluatorul nu rulează Git, nu verifică indexul și nu inspectează fișierele omise din input manifest. Revizia operatorului rămâne necesară pentru arborele complet, promovare și publicare. Contractul detaliat este în [guvernanța RAG](../../docs/feedback/ATF_Release_RAG.md) și [calificarea release-ului](../../docs/testing/ATF_ReleaseQualification.md).

[Modulul de integrare](../README.md) · [Laboratorul AI](../../whist-lab-app/ai-lab/README.md)
