# Reutilizarea guvernanței și coeziunea codului

## Responsabilitate

Proiectul deține perimetrul Android și adaptarea verificatoarelor. Contractul comun reutilizabil se solicită prin [responsabilitățile bibliotecilor](README.md), fără introducerea în consumator a implementărilor acestora.

## API reutilizabil pentru verificatoare

## Registrul livrărilor

| Prioritate | Lacună | Criteriu de închidere |
|---|---|---|
| P1 | Verificările livrărilor semnalează `RELEASE-UNREGISTERED` pentru rădăcina artefactelor. | Reconciliați indexul structurat, dosarele de versiuni, metadatele și README-urile cu contractul validatorului, păstrând identitatea binarelor și rezultatele originale. |

[Inventarul livrărilor](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactLayoutGovernanceTest.java) și [legarea handoff-ului de asset](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java) rămân controale distincte. Acceptanța unei componente nu închide o eroare a registrului de publicare.

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
- Rezolvarea adnotărilor trebuie să acopere pozițiile legale față de modificatorii declarației; [adaptorul local](../governance/README.md#declarațiile-testelor) folosește structura sintactică JDK, iar un contract comun se califică prin aceleași controale independente.
- Validarea nu rescrie documentele și nu relaxează regulile pentru a închide o revizie.

## Coeziunea și evoluția pachetelor

[Politica executabilă](../../config/atf-governance-policy.json) aplică limita fără derogări pentru structura curentă. [Responsabilitățile pachetelor](../governance/ATF_Packages.md) delimitează API-urile, suportul comun și selecțiile testelor.

[Guvernanța calității](../governance/README.md) · [Bune practici documentare](../governance/ATF_Documentation.md)

## Calificarea afirmațiilor și lansatoarelor

| Prioritate | Responsabil și lacună | Nivel și criteriu de închidere |
|---|---|---|
| P1 | Guvernanța documentară: verificările structurale nu confruntă automat toate afirmațiile despre profilul nou și cel istoric. | Controale factuale cu defecte injectate pentru scor, ajutor și documentație; fiecare afirmație identifică sursa și domeniul, fără deducerea comportamentului dintr-un cuvânt izolat. |
| P2 | Coordonarea: lansatoarele verifică precondițiile în moduri diferite. | Componente controlate pentru Java, Maven, Gradle, Node și Python: capabilitățile cerute intrării sunt verificate înaintea operației costisitoare, iar lipsa lor produce diagnostic și identitate de sursă explicite. |

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
