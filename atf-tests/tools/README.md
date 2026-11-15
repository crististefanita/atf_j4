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

[Selectorul](plan-release-tests.mjs) compară inventarul fixat cu ultima sursă publicată relevantă din catalog. Generează câmpurile planului v2 și grupurile E2E cerute înainte de build. [Evaluatorul](evaluate-release-rag.mjs) recalculează selecția și confruntă planul, manifestul, payload-ul și snapshot-ul APK. [Comanda de selecție](../../docs/testing/ATF_ReleaseQualification.md#selecția-testelor-din-diff)

- [Perimetrul suitei](component-evidence-scope.mjs) stabilește când se poate reutiliza o execuție. Pentru colecțiile Gradle, documentația și intrările exclusiv Maven ori Android E2E nu invalidează dovada aplicației; sursele, configurația de compilare, dependențele și căile necunoscute cer selecție nouă. [Pregătirea dovezilor](prepare-component-reuse.mjs) și evaluatorul folosesc aceleași verificări pentru receipt-uri și hash-uri. Lansatorul componentelor folosește această pregătire la cerere.
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

Verdictul este JSON cu citări prin cale, SHA-256 și JSON Pointer/linie. Include bytes planului în Base64 UTF-8 la `qualificationPlan.contentBase64`; hash-ul păstrează identitatea planului și permite revizuirea fără o copie temporară externă. Singura scriere este `$VerdictFile`, în afara tuturor intrărilor.

- Codul `0` înseamnă `READY_FOR_HUMAN_REVIEW`; `2` înseamnă `BLOCKED`; `1` indică intrare sau invocare invalidă.
- Înaintea reviziei umane se cer receipt-uri legate prin hash pentru componente, build APK, identitate și semnare.
- Starea publicată cere și receipt pentru descărcarea în modul de acces declarat. Pentru GitHub se verifică și retenția celor două ediții, inclusiv tagurile și asset-urile. [Comanda de colectare](../../docs/testing/ATF_ReleaseQualification.md#retenția-pe-github-după-publicare)
- [Planul per flux](prepare-android-e2e.mjs) și evaluatorul folosesc aceleași verificări pentru HTML, PNG și chitanțe. [Importul](apply-android-e2e.mjs) aduce dovada nouă în candidatul fixat. [Contractul lansatorului](../../docs/testing/ATF_ReleaseQualification.md#rularea-și-reluarea-unui-flux-pe-apk-semnat)
- E2E neexecutat rămâne `NOT_RUN`; schimbarea unui flux utilizator cere grupuri E2E Extent trecute.

Evaluatorul confruntă inventarul complet al snapshot-ului cu manifestul. Revizia operatorului verifică indexul Git, arborele de promovare și publicarea. Contractul și oracolele sunt descrise în [calificarea release-ului](../../docs/testing/ATF_ReleaseQualification.md) și la [regresiile evaluatorului](../../docs/testing/ATF_ReleaseQualification.md#regresiile-evaluatorului).

[Modulul de integrare](../README.md) · [Laboratorul AI](../../whist-lab-app/ai-lab/README.md)

## Măsurare și reproducere

- [Cronologia](assemble-release-timeline.mjs) combină chitanțele proceselor și păstrează intervalele suprapuse. [Procedura de calificare](../../docs/testing/ATF_ReleaseQualification.md#cronologia-calificării)
- [Verificatorul Git](verify-release-git-snapshot.mjs) confruntă inventarul și octeții, inclusiv EOL, și verifică payload-ul publicat înainte de curățare. [Contractul de retenție](../../docs/platform/ATF_ReleaseRetention.md#curățarea-locală-după-livrare)