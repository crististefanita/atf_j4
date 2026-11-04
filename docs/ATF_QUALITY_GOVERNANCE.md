# Guvernanța calității ATF

## Pe scurt

Guvernanța transformă principiile de structură în verificări executabile locale. Suita inspectează sursele și documentele proiectului fără dispozitiv, turneu sau acces la servicii externe.

## Perimetru și responsabilități

```mermaid
flowchart LR
    POLICY[Politică explicită] -->|delimitează| INVENTORY[Inventar de surse]
    INVENTORY -->|alimentează| GUARDS[Verificări]
    DOCS[Documente curente] -->|alimentează| GUARDS
    GUARDS -->|localizează| FINDING[Abatere explicabilă]
    FINDING -->|cere| REVIEW[Corecție și revizie]
```

<details>
<summary>Legendă: un rezultat are un perimetru</summary>

Inventarul include rădăcinile Maven declarate și rădăcinile JVM suplimentare din politică. Validatoarele documentare inspectează toate README-urile și fișierele Markdown din docs, excluzând directoarele generate.

</details>

[Suita locală](../atf-tests/src/test/java/atf/tests/governance/AtfQualityGovernanceSuite.java) și [politica](../config/atf-governance-policy.json) definesc contractul executabil. Adaptarea păstrează regulile reutilizabile ale verificatoarelor comune; ipotezele despre modulele altor aplicații nu sunt importate.

## Reguli executabile

| Zonă | Contract | Limită |
|---|---|---|
| Reactor | Modulele sursă coincid cu politica; bibliotecile comune rămân binare | Nu copiem implementarea sau infrastructura de regresie a producătorului. |
| Pachete | Declarația corespunde căii; coeziunea se verifică separat pe modul și set de surse | Numărul de fișiere nu măsoară singur calitatea semantică. |
| Coeziune | Maximum opt fișiere Java pe pachet, în afara excepțiilor revizuite | O excepție conține lista exactă, motiv și acțiune de separare. |
| Importuri | Fără importuri duplicate; comentariile și șirurile nu devin cod | Verificarea lexicală folosește parserul comun. |
| Dependențe | Motorul nu cunoaște Android, ATF sau JUnit; infrastructura respectă direcțiile declarate | Test Kit și adaptoarele declarate ale consolei dețin integrarea JUnit; interdicțiile se aplică pe responsabilitate. |
| Niveluri | O clasă executabilă Maven are un singur nivel; tabelul ghidului corespunde codului | Clasele ajutătoare și modulele Gradle au politici explicite. |
| Documentație | Structură, proză, stil, diagrame, legende, linkuri, fragmente și căi relocabile | Formularea în română și corectitudinea conceptuală cer și revizie editorială. |

Politica nu permite mărirea automată a pragului sau regenerarea excepțiilor pentru a obține succes. O înlocuire de fișier cu același număr de clase cere revizie, iar o excepție devenită inutilă este respinsă.

## Rulare izolată

Din rădăcina proiectului:

```powershell
.\scripts\run-atf-governance.ps1
```

Lansatorul compilează într-o copie temporară proprie și verifică explicit proiectul sursă. Inventarul cu hash-uri este comparat înainte și după verificare; schimbările concurente invalidează certificarea acelei copii.

Depozitul Maven al rulării este propriu copiei temporare. Cache-ul existent poate inițializa dependențele prin `-MavenCache`; instalarea bibliotecilor din `lib` nu înlocuiește fișierele folosite de alte procese.

Pentru o copie de lucru deja pregătită, cu bibliotecile și modulele necesare:

```powershell
mvn -B '-Dmaven.repo.local=<depozit-Maven-propriu>' -pl atf-tests -am '-Dtest=AtfQualityGovernanceSuite' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

`atf.governance.root` poate selecta explicit rădăcina verificată. O rădăcină invalidă produce eroare, fără revenire tacită la alt proiect.

## Dovezi și limite

- Rezultatul nativ păstrează clasa, regula și abaterea localizată.
- Probele negative verifică detectarea unui defect injectat și acceptarea documentului sănătos vecin.
- Inventarul surselor nu ignoră un pachet valid numit `target` din interiorul unei rădăcini compilate.
- Verificarea documentară nu certifică algoritmul jocului, randarea Android sau compatibilitatea tuturor bibliotecilor.
- Calificarea binarelor rămâne contractul distinct al [integrării bibliotecilor](LIBRARY_INTEGRATION.md).
- Deficiențele semantice și datoria de separare rămân vizibile în [feedback](feedback/ATF_QUALITY_GOVERNANCE_Feedback.md).

[Arhitectura testării](TESTING_ARCHITECTURE.md) · [Documentație](DOCUMENTATION_GOVERNANCE.md) · [Schimbarea testelor](ATF_Test_Changes_Baseline.md)
