# Verificări de consumator și compoziția dovezilor

## Pe scurt

Modulul verifică integrarea binarelor comune și transformă receptele capturate
într-o prezentare auditabilă. Deține selecția inputului, semantica verificărilor
Whist și acceptorii de publicare. Implementarea generică este consumată prin
API-ul bibliotecii configurate în [POM-ul părinte](../pom.xml).

## Organizare

| Sursă | Responsabilitate |
|---|---|
| [EvidencePolicyYaml](src/main/java/atf/tests/learning/EvidencePolicyYaml.java) | Citirea și validarea politicii de selecție. |
| [EvidenceSelection](src/main/java/atf/tests/learning/EvidenceSelection.java) | Invocări întregi, gate-uri și toate rezultatele non-PASS; inventarul selecției și omisiunilor. |
| [LearningRunReport](src/main/java/atf/tests/learning/LearningRunReport.java) | Compoziția receptei tipizate și păstrarea comparațiilor originale. |
| [GameReportConfiguration](src/main/java/atf/tests/reporting/GameReportConfiguration.java) | Configurație imutabilă pe publicare. |
| [GameReportSession](src/main/java/atf/tests/reporting/GameReportSession.java) | Faze și scopes native, cu limitele declarate. |
| [GameEvidenceBodies](src/main/java/atf/tests/reporting/GameEvidenceBodies.java) | Corpuri complete sau părți TEXT ordonate; refuzul inputului indivizibil peste buget. |
| [LearningReportHtmlAudit](src/main/java/atf/tests/learning/LearningReportHtmlAudit.java) | Verificarea identității checkpoint-urilor, rândurilor și valorilor publicate. |

Politica efectivă și sensul modurilor de prezentare sunt în
[contractul de prezentare](../docs/ATF_Game_Report_Presentation.md). Componentele
reutilizabile sunt în `src/main/java`; intrările JUnit sunt în `src/test/java`.

## Contracte importante

Inputul păstrează verdictul original, cauza eșecului și toate comparațiile
obligatorii ale unei invocări selectate. Contextele opționale pot fi limitate
conform politicii; limitarea lor nu schimbă expected, actual sau PASS/FAIL.
Selecția unei populații mari publică inventarul complet și nu pretinde că toate
invocările omise au fost prezentate individual.

Componentele native, partidele offline, replay-ul unei arhive și un scenariu
Android nou au scope-uri diferite. Republicarea nu produce noi partide sau
capturi. Imaginile istorice ori sintetice păstrează această etichetă.

## Verificare focalizată

Rulați într-o copie proprie din `%TEMP%`, cu POM-ul părinte, modulele necesare,
configurația și perechile fizice JAR/POM din `lib`:

```powershell
mvn -B -pl atf-tests -am "-Dtest=GameReportComponentSuite,LearningRunReportContractTest,EvidenceSelectionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Selecțiile din surse au responsabilități distincte:

- `GameReportComponentSuite`: politică, corpuri JSON/TEXT, redaction și publicare.
- `LearningRunReportContractTest`: proprietatea rândurilor, valori lungi,
  fragmente, timpi și respingerea unor mutații deliberate ale dovezii.
- `EvidenceSelectionContractTest`: populație, praguri, selecție completă și
  inputuri incomplete.
- `CoreReportingConsumerContractTest` și `AndroidInlineScreenshotReportContractTest`:
  contractele binarelor și ale adaptorului PNG.

[LearningRunReportTest](src/test/java/atf/tests/LearningRunReportTest.java)
consumă o selecție capturată explicit. `DeepSuiteExtentReportTest` este o intrare
agregată opt-in, nu testul implicit care ar trebui lansat pentru orice schimbare.
Contractele și verificările arhivate se consultă din [indexul documentației](../docs/),
fără a confunda un rezultat istoric cu sursele curente.
