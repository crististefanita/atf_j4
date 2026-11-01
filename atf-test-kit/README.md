# atf-test-kit — baza comună în J3

## Pe scurt

Modulul păstrează clasificarea și auditul runnerului, fără renderer.
Sursele și testele au fost preluate selectiv din referința J0.
`bootstrap-provenance.json` păstrează snapshot-ul inițial; sincronizările
ulterioare sunt reconciliate separat și nu rescriu istoricul.

## Ownership și limită

Implementarea comună are owner în J0. Kit este folosit de adaptorul JUnit din
source-set-ul de test; produsul Speech și comanda operațională rămân fără JUnit.
Modulul nu primește modele audio, decoder ASR sau logică de transcriere
pentru a evita ownerul Speech.

[Arhitectura J3](../docs/ATF_Speech_Architecture.md) delimitează consumerul.
[Sync-ul selectiv](../docs/ATF_Workspace_Synchronization.md) păstrează
diferențele locale și interzice mirror-ul altui deployment.

### Toleranță numerică auditată

`AuditedAssertions.assertEquals(expected, actual, delta, message)` pentru
`double` execută JUnit o singură dată și publică expected ± delta, actual
și mesajul în assertion-step-ul standard. Nu reduce comparația la true/true;
preview-ul rămâne 240, iar verdictul și cauza JUnit sunt păstrate.

## Audit obligatoriu în corpul testului

`ComponentAssertionAudit.AssertionRequirement` este politică neutră, fără
Reporting ori reguli de domeniu. Constructorii existenți și publicațiile
existente păstrează `ANY_PHASE`; consumerul poate alege explicit `TEST_BODY`
prin `ComponentAuditExtension.Publication.assertionRequirement()`.

| Situație cu TEST_BODY | Rezultat |
|---|---|
| Setup/teardown au checks, corpul nu are | FAIL; setup-ul nu înlocuiește dovada corpului |
| JUnit direct ori numai observație în corp | FAIL dacă nu există comparație Kit evaluată |
| Corp cu o comparație Kit reală | verdictul evaluat, fără reevaluare de renderer |
| Failure original sau abort | cauza și FAIL/SKIP rămân primare; fără corp inventat după setup abort |
| Handler JUnit consumă eroarea sau comparația e prinsă | failure-ul auditat rămâne FAIL |

Un succes fără comparație publică rândul structural
`Audited assertion coverage / TEST BODY`: expected `at least 1 evaluated assertion`,
actual `0`, verdict FAIL. Acesta diagnostichează lipsa dovezii, nu inventează
o verificare business. Preview-ul rămâne 240; evaluarea folosește valorile complete.

Probele permanente `TestBodyAssertionRequirementTest` și
`TestBodyAssertionRequirementComponentTest` verifică politica, lifecycle-ul
JUnit real și izolarea invocărilor template. Adoptarea strictă se decide în
adaptorul fiecărui consumer; unit-urile obișnuite nu primesc această cerință.

### Regresii assertion requirement

| Responsabilitate | Owner al probei |
|---|---|
| politica neutră ANY_PHASE / TEST_BODY | src/test/java/atf/testkit/assertions/requirement; aceleași zece cazuri, prin API-ul public |
| eșecul controlat al fixture-urilor | Launcher explicit; nested fixtures nu sunt selectate independent prin wildcard |
| coeziune | cele opt clase generale rămân în assertions, iar cele două probe de requirement au subpachet propriu |
