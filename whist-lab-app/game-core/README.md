# Motorul de reguli Whist

## Pe scurt

Bibliotecă Java fără dependență de Android. Același motor este consumat de
aplicație și de testele JVM; verificările independente ale testelor nu trebuie
să împrumute verdictul motorului verificat.

## Contracte în cod

| Sursă | Contract |
|---|---|
| [GameConfig](src/main/java/ro/whistlab/core/GameConfig.java) | Număr de jucători, variantă, nivel, seed și configurarea seriilor. |
| [GameMode](src/main/java/ro/whistlab/core/GameMode.java) | Variante distincte și alegerea profilului românesc. |
| [GameRules](src/main/java/ro/whistlab/core/GameRules.java) | Pachet, program, oferte legale, cărți legale, câștigător și scor de bază. |
| [RomanianScoring](src/main/java/ro/whistlab/core/RomanianScoring.java) | Punctele contractului, bonusul de serie și contorul următor. |
| [GameSession](src/main/java/ro/whistlab/core/GameSession.java) | Tranzițiile sesiunii, istoricul public și validarea restaurării. |
| [ScoreHistory](src/main/java/ro/whistlab/core/ScoreHistory.java) | Licitațiile pe locuri, punctele mâinii și totalurile după fiecare mână încheiată. Salvările vechi păstrează numai informația cunoscută. |
| [HowToPlay](src/main/java/ro/whistlab/core/HowToPlay.java) | Regulile și programul afișate în română și engleză. |

## Whist românesc

Profilul acceptă 3–6 jucători și un pachet redus de `8 × N` cărți mari.

Pentru `ROMANIAN_WHIST`, programul este `N` mâini de o carte, 2…7, `N` mâini de opt, 7…2 și `N` mâini de o carte. `LONG_ROMANIAN` are propriul program; nu se suprapune implicit peste primul profil. Programele tuturor variantelor sunt calculate de `GameRules.handSizes`.

Licitarea și prima levată pornesc după dealer. Oferta este între zero și mărimea
mâinii; ultima ofertă care ar face suma egală cu numărul de levate este interzisă.
Dealerul avansează la mâna următoare.

Jucătorul urmează culoarea cerută dacă o are.

- Dacă nu o are, dar deține atu, taie obligatoriu; în lipsa ambelor poate decarta.
- Supratăierea nu este obligatorie.
- Câștigă cel mai mare atu sau cea mai mare carte din culoarea cerută.
- Câștigătorul conduce următoarea levată.
- Atuul românesc este indicat de o carte neîmpărțită, întoarsă; mâinile de opt cărți sunt fără atu.

Contractul exact acordă `5 + levate`; ratarea acordă `-abs(levate - contract)`.

- Seriile sunt opționale și se aplică variantelor românești.
- După cinci contracte exacte eligibile se acordă +5 sau +10; după cinci ratări se scad 5 puncte, apoi contorul se resetează.
- O mână de o carte nu avansează seria, dar rezultatul opus o întrerupe.
- Contractul zero exact este un succes.
- Constructorul `GameConfig` cu patru argumente păstrează profilul fără serii; constructorii și validările explicite definesc celelalte valori implicite.

Oh Hell și celelalte variante folosesc profilul lor de pachet, program, atu și scor.

De exemplu, obligația de a tăia din profilul românesc nu se transferă automat în Oh Hell. Alegeți întotdeauna `GameMode` în fixture și în scenariu.

## Sesiune și salvare

O comandă ilegală este respinsă înainte de modificarea stării. Scorul și soldul
de închidere se aplică după ultima carte a mâinii. `ScoreHistory` permite doar
avansarea cu o mână; punctele unei mâini sunt diferența dintre două solduri,
inclusiv punctele seriei.

`GameSession.restoreComplete` cere istoricul public complet.

Restaurarea de compatibilitate poate accepta o salvare veche cu istoric limitat și păstrează originea cunoscută a scorului. Ea nu justifică inventarea unor cărți sau scoruri anterioare. Schema JSON Android este responsabilitatea [GameSessionStore](../app/src/main/java/ro/whistlab/app/GameSessionStore.java).

## AI și componente

[AiDifficulty](src/main/java/ro/whistlab/core/AiDifficulty.java) expune Normal și Hard.

- `EXPERT` este un alias de compatibilitate, migrat la Hard la citirea unei salvări vechi.
- [AiPolicy](src/main/java/ro/whistlab/core/AiPolicy.java) definește dispatch-ul actual.
- Estimările probabilistice și garzile de final cu domeniu finit nu demonstrează joc perfect cu informație ascunsă.
- Codul experimental extern nu este activat automat.

Testele de reguli, sesiune, scor, checkpoint și AI se găsesc în
[src/test/java](src/test/java/ro/whistlab/core/). Cazurile cu expected finit trebuie
să aibă un oracol separat sau un invariant justificat; o mutare istorică a
câștigătorului nu este singură un expected corect.

## Rulare offline

Dintr-o copie proprie din `%TEMP%` a `whist-lab-app`:

```powershell
.\gradlew.bat -p algorithm-build :game-core:quickRegressionTest --offline --no-daemon
.\gradlew.bat -p algorithm-build :game-core:regressionTest --offline --no-daemon
.\gradlew.bat -p algorithm-build :game-core:contextRegressionTest --offline --no-daemon
```

[build.gradle](build.gradle) definește selecțiile efective și excluderile testului implicit.

- Quick este o selecție, nu o populație care se adună la regresia completă.
- Colecția `context` urmărește finaluri mici cu martori legali independenți.
- `documentation` verifică regulile afișate; `strategy` și `research` au scope-uri separate.
- Turneele `learning` și `arena` se activează explicit, cu identitate de candidat și protocol, nu prin testul implicit.

[Catalogul colecțiilor](../../config/test-collections.json) și
[runnerul](../../scripts/run-whist-collection.ps1) sunt sursele selecției și
parametrilor actuali. Native XML și datele de comparație se păstrează în
directorul extern al execuției. Un rezultat JVM nu reprezintă o execuție Android.

## Politica Normal și Hard

[Manifestul regulilor de joc](../../config/whist-play-objectives.yaml) fixează
obiectivele observabile pentru cele două niveluri. Normal conduce cu forță cât timp
mai are levate de făcut și evită levatele în plus după contract. Hard maximizează
șansa propriului contract folosind licitațiile și cărțile publice. Când limitele
publice arată că exactul este imposibil, Hard păstrează mai întâi punctele proprii
estimate, apoi maximizează numărul estimat de adversari care ratează. Scorul celui
mai puternic rival departajează egalitățile. Un certificat complet poate înlocui
această estimare doar dacă nu reduce punctele proprii în nicio continuare legală și
provoacă mai multe ratări în fiecare lume public fezabilă. Certificatul actual
acoperă finalurile cu două cărți la patru locuri.

Rulați o partidă mock completă, deterministă și offline cu:

```powershell
.\scripts\run-offline-policy-match.ps1 -Seed 20261104
```

Suita parcurge toate cele 24 de mâini românești 1–8–1, verifică legalitatea prin
tranzițiile reale ale `GameSession` și înregistrează licitațiile, cărțile public
jucate, necesarul rămas pe tură, deciziile explicate de AI și punctajul fiecărui
loc. Locurile impare folosesc Hard, iar cele pare Normal, pentru a observa ambele
politici la aceeași masă. Rezultatul JSON și receipt-ul hash-uit apar sub
`C:\work\whist_back\offline-mock`.

Acesta este un instrument de simulare și diagnostic. Rezultatul unei singure
partide descrie linia jucată; nu certifică joc perfect sau un câștig general.