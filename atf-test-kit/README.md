# Kit comun pentru teste

## Pe scurt

Modulul oferă clasificarea testelor, scope-uri, configurare și auditul
aserțiunilor, fără un renderer sau reguli ale jocului. Adaptoarele JUnit sunt
suport de test; nu fac parte din motorul Whist.

## API-uri principale

- [Adnotările de clasificare](src/main/java/atf/testkit/) separă unit, component,
  integration și e2e.
- [TestRunScope](src/main/java/atf/testkit/scope/TestRunScope.java) și
  [resursele scoped](src/main/java/atf/testkit/scope/) controlează ciclul de viață.
- [ComponentAssertionAudit](src/main/java/atf/testkit/assertions/ComponentAssertionAudit.java)
  păstrează fazele, comparațiile evaluate și cauza originală.
- [ComponentAuditExtension](src/main/java/atf/testkit/assertions/ComponentAuditExtension.java)
  adaptează auditul la JUnit; consumerul furnizează publicarea.
- [AtfTestKitConfigurationLoader](src/main/java/atf/testkit/config/AtfTestKitConfigurationLoader.java)
  definește descoperirea și citirea configurației.

## Aserțiuni și audit

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
| politica neutră ANY_PHASE / TEST_BODY | src/test/java/atf/testkit/assertions/requirement; contracte prin API-ul public |
| eșecul controlat al fixture-urilor | Launcher explicit; nested fixtures nu sunt selectate independent prin wildcard |
| coeziune | cazurile generale rămân în assertions; probele requirement au subpachet propriu |

## Verificare

Rulați dintr-o copie proprie din `%TEMP%`, cu POM-ul părinte și dependențele
locale ale reactorului:

```powershell
mvn -B -pl atf-test-kit -am test
```

[POM-ul modulului](pom.xml) și [sursele testelor](src/test/java/) definesc
dependențele și contractele efective. Aceste comenzi nu sunt dovada unei execuții
deja trecute.
