# Whist game-core: reguli și verificare offline

Motorul Java rulează fără emulator, ADB sau interfață. Profilul se alege explicit
prin `GameConfig.mode`: `ROMANIAN_WHIST` este Whist românesc; `OH_HELL` păstrează
regulile aplicației Android studiate. Un PASS al primului nu certifică al doilea.

## Contractul Whist românesc

Surse consultate la **2 noiembrie 2026**:
[Goobix, editorul jocului](https://ro.goobix.com/jocuri-online/whist/) și
[Pagat, descrierea Romanian Whist](https://www.pagat.com/exact/romwhist.html).
Acesta este profilul clasic ales, fără premiere pentru serii. Reguli de club
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
| RW07 | Starea păstrează cărțile, ofertele, levatele, scorurile și istoricul public la salvare/reluare. Jocul se oprește după ultima mână. |

La 4 jucători secvența are **24 de mâini, 94 de levate, 376 de cărți jucate**.
Nu se aplică regula Oh Hell `levate + 10 dacă exact`.

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

Rularea reproductibilă cu dovezi și Extent nativ J11, din rădăcina J4:

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

## Acceptanță componentă — 2 noiembrie 2026

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
