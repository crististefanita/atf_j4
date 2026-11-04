# Contract Whist și Whist românesc: contracte de reguli distincte

Actualizat la 4 noiembrie 2026. Sursa normativă a profilului românesc, matricea
RW01–RW07, cazurile exhaustive și evaluarea AI sunt în
[game-core/README.md](../whist-lab-app/game-core/README.md).

Observarea live a aplicației-model și producerea fixture-urilor sunt deținute de [J5](../../atf_j5/README.md). J4 păstrează doar adaptorul de citire pentru snapshot-uri arhivate și replay-ul care verifică motorul și UI-ul Whist Lab; testele J4 nu pornesc și nu inspectează pachetul original.

## Diferențele care schimbă testele

| Aspect | Original Contract Whist, modul Oh Hell | Whist românesc clasic implementat |
|---|---|---|
| Pachet | Standard de 52, observat în starea originală | 8 × numărul de jucători; se elimină rangurile mici |
| Partidă la 4 locuri | 10→1→10, 19 mâini, 109 levate | 1→8→1 cu blocurile extreme repetate de 4 ori; 24 mâini, 94 levate |
| Atu | Culoarea cărții întoarse neîmpărțite | Carte neîmpărțită; fără atu la mâinile de 8 |
| Nu ai culoarea cerută, ai atu | Instrucțiunile originale permit decartare sau atu | Atu obligatoriu |
| Ofertă exactă | Levate + 10 | Levate + 5 |
| Ofertă inexactă | Numărul levatelor | Minus diferența absolută față de ofertă |
| Licitare | Ultima ofertă nu poate egaliza suma cu mărimea mâinii | Aceeași restricție |
| Următoarea levată | O conduce câștigătorul | Aceeași regulă |

Nu transferăm o regulă între aceste profiluri doar fiindcă ambele se numesc
Whist. Nici ciclul de atu din modul Descending nu se aplică modului Oh Hell.

## Ce susțin dovezile originale

Partida reală v4.5/145, capturată în `<evidence-root>/deep-app-test\runs\20261101-231857-full-match\evidence`, a încheiat toate cele 19 mâini, cu 109 levate și 436 de cărți jucate.

Secvența este 10→1→10, iar totalurile finale sunt `[89,126,145,119]`. Preferința originalului `match.originalGameOver=true` și captura finală confirmă sfârșitul partidei. Aceleași distribuții, oferte și cărți au trecut atât replay-ul motorului Java, cât și jocul Android Whist Lab; rezultatele nu sunt injectate în motor.

`OriginalWhistState` citește starea jocului din backup-ul Android fără a o modifica.

`OriginalMatchOracle` calculează separat de ambele aplicații: posesia cărții, culoarea cerută, câștigătorii, ordinea următoare, levatele, ofertele și scorurile cumulative. Capturile PNG rămân dovezi vizuale; nu sunt tratate drept OCR validat. Foaia de scor singură nu dovedește identitatea cărților sau legalitatea mutărilor.

Instrucțiunile originale sunt păstrate în rularea istorică `20261101-222629/evidence/instructions.xml`.

Ele permit alegerea între atu și decartare când lipsește culoarea. Corecția utilizatorului despre atu obligatoriu este aplicabilă profilului românesc, confirmat de sursele de mai jos.

Auditul a identificat 198 de răspunsuri la culoare, 27 de tăieri când lipsește culoarea și 102 decartări, dintre care 20 cu atu încă disponibil.

Cele din urmă confirmă direct diferența față de profilul românesc. S-au verificat 28 de levate cu mai multe atuuri, 18 cu un singur atu și 63 fără atu. Scorul total este 479: 109 puncte din levate și 370 din 37 de contracte exacte.

Aceste observații certifică o partidă completă originală Oh Hell, patru jucători, AI Normal.

Nu certifică toate configurațiile 3/5/6 locuri sau multiplayer. Vezi [acceptanța completă și limitele](COMPLETE_GAME_ACCEPTANCE.md).

## Confirmare Android a profilului românesc în Whist Lab

[Partida normală Romanian Expert](ROMANIAN_EXPERT_ACCEPTANCE.md) a încheiat 24 de mâini, 94 de levate și 376 de mutări, fără replay sau distribuție impusă.

- Toate cele 128 de mutări din cele patru mâini de opt cărți sunt fără atu.
- Oracolul separat a verificat 12 tăieri obligatorii, 159 răspunsuri la culoare, 111 decartări și toate câștigurile/penalizările.
- Scoruri finale: `[12,87,91,61]`, reconfirmate din 260 de snapshot-uri offline și din rândurile UI.
- Aceste dovezi aparțin aplicației noastre, nu redefinirii regulilor originalului.

## Surse pentru profilul românesc

- [Goobix, regulile propriului joc](https://ro.goobix.com/jocuri-online/whist/):
  pachet redus, răspuns la culoare, tăiere obligatorie, bonus și penalizare.
- [Pagat, Romanian Whist](https://www.pagat.com/exact/romwhist.html):
  distribuție, secvență, dealer, atu și licitare.
- [Clubul de Whist, regulament propriu](https://clubuldewhist.ro/regulament/):
  demonstrează existența altor structuri competiționale. Nu importăm automat
  variantele de club, penalizările disciplinare sau criteriile de departajare.

Profilul de bază nu adaugă premiere de serie, joc pe nevăzute sau obligație de
supratăiere. Acestea necesită contracte și teste separate dacă sunt introduse.

## Ordinea acceptanței

1. Componente: distribuție, licitare, carte legală, levată, scor și persistență.
2. Partide complete în model independent, pentru 3–6 locuri și toate nivelurile AI.
3. Evaluare AI cu aceleași distribuții și locuri, rezultate păstrate chiar la eșec.
4. Extent offline cu intrări și expected/actual la fiecare mutare reprezentativă.
5. Abia apoi Android E2E: integrare UI și reluarea partidei originale păstrate.

Testele românești nu folosesc emulatorul ca oracol. Raportul Android separat
păstrează PNG-urile reale inline Base64 și distinge verdictul funcțional
de observațiile de acceptanță vizuală. Implementarea Reporting rămâne în modulul de biblioteci comune.
