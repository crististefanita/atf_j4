# Realizările Whist

## Scop și model

Catalogul cuprinde 40 de obiective stabile, în opt grupuri tematice de câte cinci, bazate pe rezultate observabile ale jocului. Fiecare grup conține două obiective Ușoare, două Medii și unul Greu; dificultatea realizării este distinctă de nivelul AI-ului.

Toate cele cinci obiective sunt vizibile și acumulează progres de la prima mână eligibilă. Grupul recomandat este primul în care jucătorul nu a obținut încă un obiectiv. Recomandarea este orientativă; obiectivele din toate grupurile rămân disponibile în orice ordine.

```mermaid
flowchart LR
    MATCH[Partidă completă și eligibilă] -->|furnizează rezultatul| FACTS[WhistMatchFacts]
    LEDGER[ScoreHistory complet] -->|furnizează scorurile și mâinile| FACTS
    RULES[GameConfig și GameRules] -->|determină eligibilitatea| FACTS
    FACTS -->|alimentează evaluarea| CATALOG[Catalog: 8 grupuri × 5]
    CATALOG -->|SUM pe mână/meci sau cel mai bun meci| STATE[Stare locală versionată]
    ID[matchId stabil] -->|deduplicare| STATE
    STATE -->|realizare nouă în așteptare| POPUP[Popup după scorul mâinii/meciului]
    STATE -->|afișează| VIEW[Dialog Realizări RO/EN]
    STATE -->|recomandă fără să blocheze| PATH[Grup recomandat; fără blocarea celorlalte]
```

<details>
<summary>Legendă: fapte, stare și notificări</summary>

Mâinile și partidele furnizează fapte distincte. Evaluarea actualizează starea locală cu identificatori idempotenti, iar notificarea rămâne în așteptare până la confirmarea utilizatorului.

</details>

## Grupele și obiectivele

| Grup | Ușor | Mediu | Greu |
|---|---|---|---|
| 1. Primii pași | Încheie o partidă; obține un contract exact | Încheie cinci partide; obține 20 de contracte exacte | Câștigă trei partide |
| 2. Contracte pe mărimea mâinii | Contract exact la o carte; zero levate la o mână de cel puțin șase cărți | Cel puțin 50% exacte la mâinile de 2–3 cărți; cel puțin 50% exacte la mâinile de 4–5 cărți | Cel puțin 75% exacte la mâinile de 6–8 cărți |
| 3. Precizie în serie | Trei contracte exacte consecutive; cel puțin 50% exacte într-o partidă | Cel puțin 60% exacte; cel puțin 65% exacte într-o partidă | Cel puțin 75% exacte într-o partidă |
| 4. Serii pozitive | Încheie meci cu serii activate; obține primul bonus pozitiv | Obține trei bonusuri pozitive; câștigă cu bonus pozitiv | Obține bonusuri pozitive în cinci partide distincte |
| 5. Variante românești | Încheie 1–8–1; încheie 8–1–8 | Câștigă în 1–8–1; câștigă în 8–1–8 | Obține 500 de puncte prin regula Joc la 0 |
| 6. Mese și clasamente | Încheie o partidă la 3; încheie una la 6 jucători | Top doi la 5; top doi la 6 jucători | Câștigă trei partide la 6 jucători |
| 7. Închidere și versatilitate | Contract exact în ultima mână; exactitate la șapte mărimi de mână | Final cu trei contracte exacte; exactitate la toate mărimile 1–8 | Cinci contracte exacte în ultimele cinci mâini |
| 8. Clasament și reveniri | Păstrează/îmbunătățește locul de la jumătatea jocului; top trei la patru jucători | Top doi la patru jucători; urcă cel puțin un loc după jumătatea jocului | De pe ultimul loc la jumătate, termină în primii doi |

Catalogul executabil și textele în română și engleză sunt în [WhistAchievementCatalog](../../whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/WhistAchievementCatalog.java). Interfața afișează progresul plafonat la țintă; modelul păstrează valoarea acumulată pentru ca obiectivele cu praguri diferite să poată folosi aceeași măsură.

## Sursa de adevăr

- Progresul de mână se acordă numai după ce levatele, licitațiile și scorul acelei mâini au fost înregistrate. Obiectivele care depind de întreaga partidă se acordă numai după `SCORE` și `finalHand`, pentru locul uman de la scaunul 0.
- O partidă este utilizabilă dacă registrul include întregul program de mâini, ofertele și levatele înregistrate pentru fiecare mână. Un checkpoint vechi cu istoric incomplet nu inventează evenimente și nu acordă realizări.
- Contractul exact este decis de perechea salvată `bid == tricks`, nu de o deducție din scorul final. Mărimea mâinii provine din `GameRules.handSizes` pentru varianta și masa configurate.
- Pragurile exprimate în procente folosesc numărul real de mâini din partida și categoria respectivă. Astfel, aceeași realizare nu devine mai ușoară doar pentru că varianta sau numărul jucătorilor distribuie mai multe mâini dintr-o anumită mărime.
- Clasamentul folosește `matchScore`, deci egalitatea pe primul loc este victorie comună, iar primii doi sunt determinați ținând cont de egalități. Bonusul de 500 pentru Joc la 0 este inclus în această valoare.
- Un bonus de serie este recunoscut numai dacă diferența dintre schimbarea înregistrată și punctajul de bază este pozitivă. Penalizarea de serie nu este o realizare și nu este recompensată.
- Realizările sunt locale instalării și nu sunt legate de numele editabil al jucătorului. Partidele de fixture sau replay rămân excluse prin eligibilitatea existentă a partidei. Auto-play-ul mâinii este inclus: rezultatul jocului este măsurat, iar proveniența manuală/automată nu este persistată de modelul actual.

## Acumulare și reluare

- `SUM` adună evenimentele eligibile, iar `BEST_MATCH` păstrează cel mai bun rezultat dintr-o partidă; seriile nu se unesc între partide. ID-urile persistente, derivate din `matchId`, fac salvarea, reluarea și reconcilierea idempotente.
- La atingerea pragului, ID-ul realizării intră în coada locală. Ecranul de scor grupează notificările după mână sau partidă; elementele rămân în coadă până la închidere și reapar după o oprire prematură.
- Starea JSON versionată este separată de clasamente și profil. Migrarea păstrează progresul și ID-urile existente; datele corupte sau incompatibile produc o eroare explicită, fără resetare tăcută.

## Verificare

[WhistAchievementComponentTest](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementComponentTest.java) verifică forma catalogului, disponibilitatea obiectivelor între grupuri, pragurile `SUM`/`BEST_MATCH`, ambele variante românești, mesele de 3 și 6 locuri, faptele pe mână și pe partidă, notificările persistente și idempotenta după restart și redenumire. [GamePersistenceRankingComponentTest](../../whist-lab-app/app/src/test/java/ro/whistlab/app/persistence/GamePersistenceRankingComponentTest.java) verifică ordinea checkpoint–progres la mâna încheiată–clasament–replay și izolarea erorilor de stocare.

Corpusul offline activ este selectat pentru lecții AI, nu pentru a estima șansa unui jucător uman de a obține o realizare. El poate depista obiective fără exemple observate sau praguri cu probleme, dar procentele din acel corpus nu sunt prezentate ca probabilități de utilizator.

[Contractele variantelor](README.md) · [Componente JVM ale aplicației](../../whist-lab-app/app/src/test/README.md) · [Strategia de testare](../testing/README.md)
