# Contract Whist și Whist românesc: contracte de reguli distincte

Actualizat la 2 noiembrie 2026. Sursa normativă a profilului românesc, matricea
RW01–RW07, cazurile exhaustive și evaluarea AI sunt în
[game-core/README.md](../whist-lab-app/game-core/README.md).

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

Partida reală v4.5/145, capturată în
`C:\work\whist_back\deep-app-test\runs\20261101-231857-full-match\evidence`,
a încheiat 16 mâini și a ajuns la checkpoint-ul 7/8 din mâna 17 înainte de
pauza cerută. Secvența efectivă a trecut de o carte și a revenit la opt.
Totalurile după mâna 16 sunt `[75,110,127,100]`.

`OriginalWhistState` citește starea jocului din backup-ul Android fără a o
modifica. `OriginalMatchOracle` calculează separat de ambele aplicații:
posesia cărții, culoarea cerută, câștigătorii, ordinea următoare, levatele,
ofertele și scorurile cumulative. Capturile PNG rămân dovezi vizuale; nu sunt
tratate drept OCR validat. Foaia de scor singură nu dovedește identitatea
cărților sau legalitatea mutărilor.

Instrucțiunile originale sunt păstrate în rularea istorică
`20261101-222629/evidence/instructions.xml`. Ele permit alegerea între atu și
decartare când lipsește culoarea. Corecția utilizatorului despre atu obligatoriu
este aplicabilă profilului românesc, confirmat de sursele de mai jos.

Aceste observații nu certifică încă o partidă originală întreagă sau toate
configurațiile 3/5/6 locuri. Emulatorul a fost oprit cu datele păstrate.

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
va păstra PNG-urile reale inline Base64 și va distinge verdictul funcțional
de observațiile de acceptanță vizuală. Implementarea Reporting rămâne în J0.
