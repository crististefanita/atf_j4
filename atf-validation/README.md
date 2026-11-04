# Validare semantică API

## Pe scurt

Modulul evaluează contracte de răspuns și produce comparații și decizii de
validare independente de renderer. Nu deține transportul și nu verifică
regulile Whist în locul motorului sau al oracolului jocului.

## Puncte de intrare

[ResponseContract](src/main/java/atf/validation/contract/ResponseContract.java)
grupează regulile, iar
[ValidationEvaluator](src/main/java/atf/validation/evaluation/ValidationEvaluator.java)
aplică politica strictă sau warning asupra tabelului evaluat. Limita de afișare
nu înlocuiește valoarea folosită pentru verdict.

[JsonSchemaCapabilities](src/main/java/atf/validation/contract/schema/JsonSchemaCapabilities.java)
definește subsetul de schemă acceptat și refuză keyword-urile nesuportate.
[Modelele AI](src/main/java/atf/validation/ai/) descriu așteptări și observații
neutre; nu sunt algoritmul adversarilor din Whist Lab.

## Contract JSON și subset de schemă

Evaluarea parsează cel mult o dată body-ul în contextul apelului. Documentul
trebuie consumat integral; JSON malformat, chei duplicate și trailing data
produc comparații negative. Contextul nu supraviețuiește între evaluări; portul public
`ResponseValidationRule` rămâne compatibil.

| Boundary | Contract executabil |
|---|---|
| Prezență | document invalid, cale invalidă, missing și valoare prezentă sunt distincte; explicit null este prezent |
| Căi | proprietăți/indexuri `$.item[0].id`, forma simplă `item.id` și JSON Pointer cu escape-uri valide; wildcard/filter/recursive descent sunt refuzate |
| Egalitate scalară | obiectele/array-urile nu devin șir gol; `jsonPathEquals` și membrii `everyArrayItemEquals` cer scalari |
| Schema | preflight independent de instanță, inclusiv ramuri neatinse; schemă obiect sau boolean |
| Tipuri și valori | `type` unic sau listă, `enum`, comparații numerice exacte; integer include valoarea numerică integrală |
| Obiecte | `required`, `properties`, `additionalProperties` boolean/schemă; false funcționează și fără properties |
| Array-uri | `items` boolean/schemă, `minItems`, `maxItems` |
| Șiruri | `minLength`/`maxLength` în codepoints Unicode; `pattern` folosește regex Java valid |
| Numere | `minimum`/`maximum` inclusiv, cu BigDecimal; fără conversie la double pentru verdict |
| Metadate | `$schema` acceptă numai URI-ul draft 2020-12; `$comment`, `title`, `description`, `default`, `examples` sunt adnotări |
| Nesuportat | orice alt keyword, inclusiv `const`, `$ref` și `format`, ori formă invalidă este refuzată; nu pretindem întregul JSON Schema |

URI-ul acceptat este `https://json-schema.org/draft/2020-12/schema`.
Un keyword refuzat rămâne refuzat chiar dacă inputul i-ar satisface sensul;
`const:42` nu este ignorat nici pentru 42, nici pentru 99.

Strict produce FAIL, iar politica warning păstrează rândurile negative și
starea WARNING. Valorile integrale decid verdictul; proiecția auditului
păstrează preview-ul 240, fără reevaluare în renderer.

### Probe și integrare

Regresii permanente: [prezență și containere](src/test/java/atf/validation/JsonPresenceContractTest.java)
și [subsetul schemei](src/test/java/atf/validation/JsonSchemaContractTest.java).
Fixture-urile API verifică separat verdictul, rândurile și preview-ul;
nu demonstrează transportul unui serviciu real.

### Content-Type — clasificare semantică JSON

`contentTypeJson()` delegă tipul/subtipul concret către `MediaTypes.isJson` din
Core. Parametrii nu pot transforma un tip text/plain în JSON; ACTUAL
păstrează headerul observat, redactat și limitat după evaluare.

| Header observat | Verdictul regulii |
|---|---|
| application/json, inclusiv cu charset | PASS; tipul și subtipul sunt case-insensitive |
| application/problem+json sau application/vnd.atf+json | PASS; sufix JSON concret |
| text/json sau text/vnd.atf+json | PASS conform vocabularului Core, fără restricție implicită la application |
| text/plain cu profile="application/json" | FAIL; parametrul nu este tipul/subtipul |
| application/jsonp, tip absent, wildcard ori listă de tipuri | FAIL; cerința este un tip JSON concret |

- `userItem`, `userCollection`, `postItem`, `postCollection` și
  `relationshipCollection` folosesc regula semantică, cu statusurile și
  cerințele de payload păstrate.
- `contentTypeContains` rămâne operație literală de compatibilitate.
  Rândul semantic are identitatea Content-Type JSON, distinctă de cel literal,
  astfel încât ambele reguli pot fi compuse în același contract.
- Clasificarea nu validează sintaxa integrală a parametrilor sau body-ul.
  Regulile JSON verifică separat documentul și câmpurile cerute.

[JsonMediaTypeContractTest](src/test/java/atf/validation/media/JsonMediaTypeContractTest.java)
protejează contractele strict/warning, cazurile nominale și parametrii
înșelători; acceptanța portului neutru și a artifactului rămâne la consumer.

### Parser comun și proprietatea datelor

Documentele complete folosesc JsonDocuments din Core: fără duplicate ori
trailing content, cu decimale exacte. Validation păstrează diagnosticele și
politica proprie pentru null; nu deține un mapper strict paralel.

## Verificare

Rulați dintr-o copie proprie din `%TEMP%`, cu POM-ul părinte și dependențele
locale ale reactorului:

```powershell
mvn -B -pl atf-validation -am test
```

[POM-ul modulului](pom.xml) și [sursele testelor](src/test/java/) definesc
dependențele și contractele efective. Aceste comenzi nu sunt dovada unei execuții
deja trecute.
