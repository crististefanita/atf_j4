# Whist game-core: reguli și verificare offline

Motorul Java rulează fără emulator, ADB sau interfață. Profilul se alege explicit
prin `GameConfig.mode`: `ROMANIAN_WHIST` este Whist românesc; `OH_HELL` păstrează
regulile aplicației Android studiate. Un PASS al primului nu certifică al doilea.

## Contractul Whist românesc

Surse consultate la **2 noiembrie 2026**:
[Goobix, editorul jocului](https://ro.goobix.com/jocuri-online/whist/) și
[Pagat, descrierea Romanian Whist](https://www.pagat.com/exact/romwhist.html).
Constructorul compatibil cu patru argumente păstrează profilul clasic fără
premiere pentru serii. Meniul Android nou activează implicit seriile, cu +10;
+5 reproduce valoarea din secțiunea Romanian a originalului. Reguli de club
precum [8-2-2-8, 8-1-8, 8-8-8](https://clubuldewhist.ro/regulament/)
sunt variante distincte, nu excepții ascunse în acest profil.

| ID | Contract implementat |
|---|---|
| RW01 | 3–6 jucători; pachet de `8 × N` cărți mari. Rang minim: 9/7/5/3 la 3/4/5/6 locuri. Asul este cel mai mare. |
| RW02 | `N` mâini de 1, apoi 2…7, `N` de 8, 7…2 și `N` de 1. Dealerul avansează un loc. Atuul vine din prima carte neîmpărțită; la 8 cărți nu există atu. |
| RW03 | Licitarea și prima levată pornesc de la stânga dealerului. Oferta este între 0 și mărimea mâinii. Numai ultima ofertă care ar egaliza suma cu mărimea mâinii este interzisă. |
| RW04 | Ai culoarea cerută: o urmezi. Nu o ai, dar ai atu: tai obligatoriu. Nici culoare, nici atu: decartezi. Fără atu, nu există obligația de tăiere. |
| RW05 | Câștigă cel mai mare atu sau cea mai mare carte din culoarea cerută. Câștigătorul conduce următoarea levată. Nu adăugăm obligația de supratăiere. |
| RW06 | Exact: `5 + levate`. Inexact: `-abs(levate - ofertă)`. Scorul se adaugă o dată, după ultima carte. Totalurile negative sunt valide. |
| RW07 | La ultima carte, motorul înregistrează o singură dată soldurile tuturor locurilor în `ScoreHistory`. Reluarea și trecerea la mâna următoare păstrează registrul. [Schema și probele](../../docs/WHIST_SCORE_HISTORY.md) |

La 4 jucători secvența are **24 de mâini, 94 de levate, 376 de cărți jucate**.
Nu se aplică regula Oh Hell `levate + 10 dacă exact`.

## Instructions complete și serii configurabile

Am citit toate secțiunile meniului original, inclusiv cele aflate sub primul
viewport. [Matricea Instructions](../../docs/ORIGINAL_INSTRUCTIONS_COVERAGE.md)
separă cerințele offline implementate de rating/multiplayer neimplementate.

`GameConfig(..., true, 5)` activează seria originală; `(..., true, 10)` activează
bonusul pozitiv cerut. După cinci contracte exacte eligibile se acordă +5/+10,
după cinci ratări −5, apoi contorul se resetează. Mâinile de o carte nu măresc
seria, dar rezultatul opus o întrerupe. Contractele zero exacte sunt succese.
`RomanianScoring.Result` separă punctele contractului, punctele de serie și
contorul următor. Scorarea sesiunii le aplică numai după ultima carte.

`RomanianStreakContractTest` verifică domeniul complet de 5.112 combinații,
serii la mâna a cincea/a zecea, întreruperi, restaurare și respingerea unor
bonusuri incoerente. `OriginalVariantContractTest` acoperă opt moduri, inclusiv
Descending 17 la trei locuri, Down & Up, Maximum, Long Romanian și Reverse.
`OriginalVariantModelTest` verifică 48 de partide complete cu model separat,
toate variantele la 3–6 locuri și Romanian/Long Romanian cu serii 5/10 sau fără.

Checkpoint-ul păstrează opțiunea, contorul semnat −4…4 și premiul ultimei mâini.
Schema Android v3 citește v1/v2 fără să inventeze serii anterioare. Reluarea la
SCORE nu reaplică bonusul. AI-ul Expert primește numai contoarele publice și
valoarea configurată; reward-ul modifică utilitatea, nu distribuția eșantionată
a cărților în stări publice identice. Profilul clasic păstrează fluxul aleator
înghețat al plannerului independent.

Expert folosește la patru locuri un posterior al licitațiilor publice pentru o
mână de o carte, calibrat pe development, cu oferta forțată exclusă ca indiciu.
Celelalte mese păstrează priorul combinatoric. Folosește 32 de lumi
comune pentru alternative, repartizare fezabilă cu respectarea culorilor
epuizate și enumerare completă a cel mult 256 de lumi mici. În finaluri cu
cel mult trei cărți pe loc la patru jucători, respectiv două la celelalte mese,
utilitatea combină 50/50 rollout euristic și MaxN
exact într-o ipoteză completă. Aceasta nu este rezolvarea perfectă a jocului cu
informație ascunsă. Solverul are oracol finit separat; comparațiile păstrează
și experimentele regresive. Vezi [protocolul AI](../../docs/AI_TRAINING_PROTOCOL.md).

[Runda V3](../../docs/AI_CORPUS_ROUND3_ACCEPTANCE.md) are 333 de teste JVM PASS
și 1.137 de partide JSON revalidate independent, fără emulator. Expertul acceptat
are win share 31 față de 28 pe 128 de comparații heldout; punctajul agregat este
aproape egal. Forecast-ul de serie calibrat este un instrument de analiză:
experimentele care îl activau au regresat și nu intră în politica acceptată.

## Acoperirea cazurilor

`RomanianWhistRulesTest` verifică fiecare ofertă/scor 0…8, restricția ultimei
oferte pentru toate dimensiunile mesei, distribuția și blocurile de mâini.
`RomanianWhistExhaustiveTest` verifică 6.375 de configurații de legalitate
(toate submulțimile nevide ale unui set reprezentativ cu două ranguri per
culoare) și 4.960 de perechi ordonate din pachetul de 32 de cărți, pentru toate
cele cinci stări de atu. Aceste domenii sunt exhaustive; toate partidele
posibile nu sunt enumerate.

`RomanianWhistSessionTest` verifică tăierea obligatorie, urmarea culorii după
ce altcineva a tăiat, supratăierea neobligatorie, respingerea fără modificarea
stării, ordinea licitării, scorarea o singură dată și păstrarea totalurilor
negative. `GameCheckpointIntegrityTest` corupe intenționat totalul ofertelor,
locul ofertantului, cărțile și istoricul; restaurarea trebuie să le respingă.

`RomanianWhistModelTest` are un model separat pentru cărți legale, actori,
câștigători și scor. Acesta **nu apelează GameRules pentru rezultatele
așteptate**. Rulează partide complete la 3–6 locuri, cu ambele politici de bază
și cu Expert, verificând fiecare mutare și restaurând checkpoint-uri în toate
fazele. O rulare cu dovezi produce un flow complet explicat de 4 jucători.

`CheckpointHistoryAuditTest` reconstruiește istoricul complet al unei mâini
la restaurare. Include câștigător inventat, contoare de levate redistribuite cu
suma păstrată, ordine schimbată, renunțare ilegală la culoare, evitarea atuului,
istoric v2 trunchiat și atu inventat la opt cărți. `restoreComplete` cere toate
cărțile jucate; `restore` rămâne calea explicită pentru checkpoint-uri v1 cu
istoric parțial. O mână v1 reluată nu devine v2 completă prin simpla resalvare;
următoarea distribuție nouă începe din nou cu istoric complet.

`MatchSummaryTest` verifică explicațiile profilurilor, prioritatea culorii,
tăierea obligatorie, supratăierea opțională, mâinile fără atu, progresul și
câștigătorii la egalitate sau cu totaluri negative. Expert are o cale explicită
pentru o singură carte legală: o joacă fără simulare și fără punctaj estimat
inventat. Restul deciziilor păstrează eșantionarea descrisă mai jos.

JaCoCo măsoară linii și ramuri ale codului; raportul păstrează și ramurile
neacoperite. Procentul de cod și acoperirea regulilor sunt măsurători diferite.
Regulile de club, premierele, jocul pe nevăzute și multiplayer nu sunt acoperite.

## AI și evaluarea lui

Normal și Hard rămân repere. Expert folosește 32 de distribuții posibile și
simulează restul mâinii pentru fiecare carte legală. Alege după punctajul
contractului, astfel încât evitarea unei levate poate fi mai bună decât
câștigarea ei. Licitarea compară punctajul estimat al ofertelor permise.

`MonteCarloAi.View` conține numai mâna proprie, oferte/levate publice, numărul
cărților adversarilor, cartea întoarsă, levata curentă și istoricul public. Nu poate conține
cărțile ascunse ale adversarilor. Istoricul elimină cărțile deja jucate și
cartea întoarsă este exclusă din cărțile ascunse. Istoricul permite deducerea
culorilor epuizate. Distribuțiile care contrazic aceste
deducții sunt abandonate. Selecția este repetabilă, fără seed din ceas.

`MonteCarloAiTest` permută cărțile ascunse și cere aceeași observație și aceeași
decizie. Verifică și contract zero, levată necesară, tăiere obligatorie în
ciuda interesului strategic, imutabilitate și istoric după restaurare.

`AiTournamentTest` compară Expert cu Hard pe seed-urile declarate 1000…1007,
pentru mese de 3–6 jucători, rotind jucătorul evaluat prin toate locurile;
adversarii rămân Hard. Același
seed și același loc definesc perechea. Se păstrează toate scorurile și ofertele
exacte, inclusiv un rezultat nefavorabil. Gate-ul cere total Expert cel puțin
egal cu Hard la fiecare dimensiune de masă. Sunt 144 de perechi în total;
acest eșantion nu demonstrează optimalitate globală.

## Comenzi

Din `whist-lab-app`, numai motorul:

```powershell
.\gradlew.bat --no-daemon --console=plain :game-core:test :game-core:jacocoTestReport
.\gradlew.bat --no-daemon --console=plain :game-core:aiTournamentTest
```

Rularea reproductibilă cu dovezi și Extent nativ J12, din rădăcina J4:

```powershell
.\scripts\run-rule-components.ps1
```

Scriptul compilează și APK-ul, dar **nu pornește emulatorul și nu instalează**.
Dovezile rămân în `C:\work\whist_back\rules-components\<run>\`. Raportul
arată SETUP, TEST BODY, TEARDOWN, cazurile executate, intrări și expected/actual
pentru fiecare mutare a partidei reprezentative. Capturile Base64 aparțin
raportului separat de acceptanță Android; nu fabricăm poze de UI din teste JVM.
Consumerul păstrează preview-ul de 240 caractere și folosește corpurile TEXT
native cu `bodyMaxDisplayCharacters=0`, pentru ca datele integrale să rămână
în HTML. Publisherul verifică fiecare valoare, inclusiv ultimul element.

## Extinderea componentelor — 2 noiembrie 2026

Rularea `C:\work\whist_back\romanian-expert\20261102-131344\component-acceptance\`
are **210 cazuri motor/AI PASS**, fără erori sau omiteri, înainte de Android.
Cele 23 de cazuri noi se adaugă baseline-ului de 187; aceleași 88 de partide,
40.304 mutări și benchmark-ul cu 144 perechi rămân verificate. Expert/Hard:
14.604/7.689 puncte, 2.792/1.777 contracte exacte.

GameRules rămâne la 77/77 linii și 94/94 ramuri. Întreg motorul are acum
652/671 linii (97,17%) și 549/607 ramuri (90,44%). Gărzile și explicațiile noi
măresc codul măsurat; procentul global nu este o enumerare a partidelor posibile.
Un control separat elimină numai auditul istoricului într-un mutant TMP:
7 dintre cele 12 cazuri îl refuză. Mutantul nu ajunge în APK.

Pentru verificarea normală Android, oracolul separat și raportul corespunzător,
vezi [Whist românesc Expert](../../docs/ROMANIAN_EXPERT_ACCEPTANCE.md).

## Prima acceptanță componentă — 2 noiembrie 2026, istoric

Rularea `C:\work\whist_back\rules-components\20261102-card-acceptance\`:
187 cazuri JUnit/Gradle PASS, fără FAIL/ERROR/SKIP; publisherul Extent a trecut
separat. Cele 88 de partide cu model independent au verificat 40.304 mutări și
8.712 levate. Domeniile exhaustive sunt 6.375 configurații de legalitate și
4.960 comparații de câștigător. Nu sunt numărate drept cazuri JUnit separate.

JaCoCo: GameRules are 77/77 linii și 94/94 ramuri. Întregul game-core are
595 linii acoperite / 17 neacoperite (97,22%) și 469 ramuri / 46 neacoperite
(91,07%). Ramurile rămase sunt vizibile în raport; nu pretindem acoperirea
oricărei stări sau partide posibile.

Expert vs Hard: 144 perechi, aceleași seed-uri și rotație prin toate locurile
la 3–6 jucători. Totaluri 14.604 vs 7.689; oferte exacte 2.792 vs 1.777.
Expert a trecut pragul separat la fiecare dimensiune de masă. Nu este o
estimare a performanței împotriva oamenilor sau o demonstrație de optimalitate.

Raportul `component-extent-report.html` păstrează fiecare
intrare/expected/actual integral, verificat automat. Nu a fost verificat vizual
în browser: MCP a blocat schema file. Acest raport este numai pentru componente;
acceptanța Android și capturile reale inline Base64 rămân separate.

Rularea anterioară `20261102-complete` (184 cazuri) este păstrată ca istoric.
Capturile Android au identificat apoi pica marcată roșu și caroul negru.
`CardPresentationTest` acoperă acum culorile corecte, identitatea textuală și
accesibilă a tuturor celor 52 de cărți și respingerea rangurilor invalide.

[Acceptanța completă Android](../../docs/COMPLETE_GAME_ACCEPTANCE.md) a trecut
ulterior toate cele 19 mâini originale și replay-ul acelorași 436 de intrări
în motor și UI, cu scoruri identice. Testul normal Romanian Expert a confirmat
separat ieșirea din replay și integrarea unei mâini reale. Raportul Android
include 325 checkpoint-uri cu PNG inline Base64; testele de componente rămân
independente de emulator și de randarea originalului.
