# Reutilizarea guvernanței și coeziunea codului

## Responsabilitate

Proiectul deține perimetrul Android și adaptarea verificatoarelor. Contractul comun reutilizabil se solicită prin [responsabilitățile bibliotecilor](README.md), fără introducerea în consumator a implementărilor acestora.

## API reutilizabil pentru verificatoare

| Prioritate | Lacună | Criteriu de închidere |
|---|---|---|
| P1 | Validatoarele comune nu au încă un contract împachetat independent de reactorul producătorului. | API sau CLI cu rădăcină explicită, inventar de documente și surse, module, rădăcini JVM suplimentare și excepții motivate. |
| P1 | Descoperirea și unele verificări factuale presupun țintele producătorului. | Contractele factuale primesc sursa și afirmația de verificat; un profil Android valid este verificabil fără modulele altei aplicații. |

Integrarea locală păstrează verificatoarele adaptate și controalele lor negative până la demonstrarea echivalenței API-ului împachetat. Configurația Surefire nu substituie identificarea rădăcinii efectiv inspectate.

## Criterii pentru contractul comun

- Același inventar alimentează structura, linkurile, fragmentele, proza, stilul, diagramele și căile configurabile.
- Diagnosticul conține regula, fișierul, linia și remedierea; rezultatul identifică validatorul și populația verificată.
- Rădăcina greșită, inventarul gol, documentele lipsă și căile din afara perimetrului sunt refuzate.
- Probele negative detectează defectul injectat și acceptă documentul sănătos vecin, inclusiv pe căi cu spații și diacritice.
- Descoperirea surselor și adnotările compuse păstrează aceeași semantică; comentariile și exemplele nu devin clase executabile.
- Rezolvarea adnotărilor trebuie să acopere pozițiile legale față de modificatorii declarației; adaptoarele locale folosesc forma canonică până la calificarea completă a parserului comun.
- Validarea nu rescrie documentele și nu relaxează regulile pentru a închide o revizie.

## Coeziunea și evoluția pachetelor

[Politica executabilă](../../config/atf-governance-policy.json) aplică limita fără derogări pentru structura curentă. [Responsabilitățile pachetelor](../governance/ATF_Packages.md) delimitează API-urile, suportul comun și selecțiile testelor.

- O mutare verifică noua declarație de pachet, importurile, dependențele și API-urile restrânse.
- O excepție devenită inutilă se elimină după verificare; lista nu se regenerează automat pentru a ascunde creșterea.
- O listă revizuită păstrează motivul și acțiunea de separare, iar înlocuirea unui tip cere revizie explicită.

[Guvernanța calității](../governance/README.md) · [Bune practici documentare](../governance/ATF_Documentation.md)

## Control tehnic activ

<details>
<summary>Control activ: proprietatea și documentația bibliotecilor</summary>

```yaml
testChanges:
  - id: LIBRARY-OWNERSHIP-README-J4
    status: VERIFIED
    base: d46948264e7b35347ad812be1e67e9fba30eef05
    expires: 2026-11-30
    reason: Require exactly four flat JAR/POM pairs plus the dedicated README and common producer ownership of library source and feedback; preserve physical binary identity, independent negative oracles and consumer acceptance.
    proof: 'mvn -B -ntp -pl atf-tests -am -Dtest=PlatformLibraryContractTest,PlatformLibraryIntegrityTest -Dsurefire.failIfNoSpecifiedTests=false test'
    paths:
      - atf-tests/src/test/java/atf/libraries/PlatformLibraryContractTest.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibraryIntegrityTest.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibrarySurface.java
```

Controalele negative resping README-ul absent, documentele suplimentare, un director în locul README-ului și atribuirea codului ori feedback-ului unui consumer. Bibliotecile distribuite își păstrează coordonatele și hash-urile.

</details>
