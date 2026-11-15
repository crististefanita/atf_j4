# Guvernanța schimbărilor în teste

## Pe scurt

O modificare a testelor păstrează sensul contractului și motivul pentru care o intrare este admisă sau refuzată. Schimbarea implementării nu justifică ajustarea rezultatului așteptat doar pentru a obține succes.

## Criterii

| Suprafață | Contract |
|---|---|
| Expected | Derivat din cerință, invariant sau oracol independent. |
| Control negativ | Eșecul intenționat demonstrează că testul detectează defectul vizat. |
| Identitate | Cazul, sursele, configurația și domeniul sunt explicite. |
| Verdict | Cauza originală se păstrează prin captură și publicare. |
| Populație | Selecția și omisiunile sunt declarate fără numărători istorice fixate în ghid. |
| Rezultate | Rulările concrete și comparațiile de versiuni se păstrează în arhiva externă. |

## Revizia unei schimbări

1. Formulați proprietatea generală și frontierele ei.
2. Reproduceți defectul într-un caz mic cu date controlate.
3. Verificați eșecul controlului negativ și succesul cazului valid.
4. Reconciliați sursele concurente înaintea calificării.
5. Actualizați colecția, contractul și documentația mecanismului în aceeași revizie.

[Guvernanța documentației](ATF_Documentation.md) · [Colecții](../testing/ATF_Collections.md)

## Verificarea diff-ului Git

Rulați [lansatorul de guvernanță](../../scripts/run-atf-governance.ps1) din checkout-ul revizuit. Alegeți commitul comun de dinaintea schimbării. Lansatorul verifică testele pe o copie izolată și citește diferența din checkout-ul Git indicat.

```powershell
.\scripts\run-atf-governance.ps1 -TestChangeBaseRef $reviewBase
```

| Contract | Verificare |
|---|---|
| Bază | Un commit local anterior sau egal cu HEAD. În CI declarați baza explicit și anterioară lui HEAD. Local, fără parametru, HEAD delimitează numai schimbările necomise. |
| Intrări Git | Diferențele comise față de bază, indexul, fișierele de lucru și fișierele noi. Sunt incluse și intrările protejate ignorate de Git. |
| Perimetru | Seturile Maven și Gradle de teste și resurse, POM-urile, configurarea Gradle, selecțiile de suite și intrările executabile ale guvernanței și calificării. |
| Autorizație | O singură intrare eligibilă pentru calea exactă, SHA-ul bazei și data UTC. Data expirării este inclusivă. |
| Refuz | Bază absentă sau străină, cale neacoperită, control expirat, eligibilitate ambiguă, metadate invalide sau stare Git neacceptată. |
| Redenumire | Ștergerea căii vechi plus adăugarea căii noi. Ștergerea cere explicit allowDelete. |

Lansatorul transmite baza prin proprietatea atf.governance.testChanges.baseRef. Pentru selecția de componente se poate seta ATF_TEST_CHANGE_BASE_REF înainte de lansare. Review-ul întregii ramuri folosește întotdeauna un commit anterior lui HEAD.

## Ciclul de viață al controlului

| Etapă | Loc și criteriu |
|---|---|
| ACTIVE | Documentul de [feedback responsabil](../feedback/README.md) păstrează motivul, comanda probei și lista exactă a căilor. |
| VERIFIED | [Registrul tehnic](ATF_TestChangesBaseline.md) păstrează același ID după probă. Eligibilitatea rămâne legată de baza inițială și de expirare. |
| DRAFT și CLOSED | Intrarea structurată poate consemna propunerea sau închiderea. Aceste stări au eligibilitate zero. |
| Retragere | Confruntați controlul cu baza revizuirii. Eliminați intrarea numai când diff-ul vizat nu mai are nevoie de ea. |

- Blocul YAML are cheia `testChanges` și o listă nevidă. Fiecare element declară `id`, `status`, `base`, `expires`, `reason`, `proof` și `paths`. Câmpul `allowDelete` este opțional și implicit fals.
- Căile relative folosesc separatorul `/`. Fiecare ID este unic în documentele inspectate.

Comanda proof exprimă verificarea planificată și se execută separat. [Probele de componentă](../../atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java) construiesc istorii Git locale și califică mecanismul fără dispozitiv.

## Mutarea proprietății unei verificări

Transferul unei implementări către o bibliotecă păstrează toate contractele native la producător. Consumatorul păstrează acceptanța API-ului folosit, identitatea JAR-ului încărcat și refuzurile pentru metadate ori livrări inconsistente.

- Cererea de schimbare identifică sursele afectate, proprietarul nou, înlocuirea echivalentă și criteriile independente de acceptare.
- Rulările și capturile transferului se leagă de livrare. Controlul tehnic păstrează baza Git și căile sale în registru.
- Ștergerea copiei locale nu suprimă un caz negativ și nu transformă o verificare de bibliotecă într-o acceptanță Android.
- Documentația curentă descrie limita de responsabilitate, fără liste istorice de teste șterse.

[Guvernanța calității](README.md) verifică perimetrul curent și interzice sursele paralele ale bibliotecilor binare.
