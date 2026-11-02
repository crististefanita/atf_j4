# Runda V3 — corpus offline și îmbunătățire Expert

Run: `C:\work\whist_back\ai-training\20261102-round3\`.
Corpus: `C:\work\whist_back\training-corpus\`.

## Date reținute

1.137 de partide complete în JSON simplu, 27.288 de mâini, 109.152 de licitații,
106.878 de levate și 427.512 cărți. Fiecare partidă a trecut replay-ul independent
Node: actori, proprietatea cărților, culoare/atu obligatoriu, câștigători,
licitația finală, scoruri, bonusuri și contoare.

- `development`: 881 de partide, utilizabile pentru antrenament.
- `heldout`: 256 de partide, rezervate evaluării.
- Sunt **93 de seturi distincte de distribuții**, nu 1.137 de distribuții
  independente. Rotirea locurilor și experimentele pe același seed sunt corelate.
- O partidă este observația Android V2; celelalte sunt partide executate în
  componentă. Fișierele nu sunt inventate sau convertite în victorii după rezultat.
- Statisticile candidate sunt separate în index după ID-ul experimentului;
  eticheta istorică `candidate-v3` din JSON se interpretează împreună cu ID-ul și
  `policy-provenance/`. Heldout nu intră în statisticile de antrenament.

## Ce se promovează

Expert candidate05 extinde căutarea MaxN pe distribuții posibile la trei cărți
rămase pe loc pentru mese de patru jucători. La celelalte mese limita rămâne două.
Păstrează 32 de lumi comune și amestecul 50/50 cu rollout-ul euristic. API-ul de
decizie rămâne limitat la cărțile proprii și informația publică.

Protocolul a fost stabilit înainte de selecție: win share fără regresie în fiecare
familie fixă, scădere medie de scor cel mult două puncte pe partidă și progres
strict agregat în victorii sau scor. Win share împarte victoria între egalități.

| Evaluare | Adversari fixați | Perechi | Victorii V2 → nou | Puncte V2 → nou |
|---|---|---:|---:|---:|
| Dezvoltare | 3 Expert V2 | 32 | 8 → 8,5 | 2.637 → 2.735 |
| Dezvoltare | 3 planner V2 | 32 | 9 → 11 | 2.847 → 2.912 |
| Heldout | 3 Expert V2 | 64 | 16 → 18 | 5.463 → 5.409 |
| Heldout | 3 planner V2 | 64 | 12 → 13 | 5.186 → 5.231 |

Heldout: **31 față de 28 win share**, scor total **10.640 față de 10.649**.
Este un progres finit la victorii, cu punctaj aproape egal. Nu dovedește joc
perfect, dominanță contra fiecărui adversar sau victorie inevitabilă.

Cele 16 partide suplimentare, seed-uri 3000…3003 cu locuri rotite, folosesc
politicile finale între ele și sunt doar descriptive. Plannerul are win share 5;
restul mesei 11. Trei adversari nu reprezintă un singur adversar individual.

## Ce se respinge și ce se repară la planner

Șase experimente sunt păstrate integral: sampler Hall folosit pentru toate
distribuțiile, probabilitatea exactă de o carte, forecast de serie cu ponderi
diferite, bugete mai mari și finaluri exacte în joc/licitație. Nicio variantă de
forță a plannerului nu a trecut criteriul. Candidate05 are status combinat
REGRESSION în dezvoltare deoarece plannerul a regresat; familiile Expert trec
separat și numai Expertul este înghețat pentru heldout.

Plannerul activ revine la deciziile V2. Păstrează un fallback Hall după 64 de
încercări eșuate, pentru a nu rata o distribuție fezabilă. Un test reconstruiește
o mână legală de opt cărți, după șase levate: samplerul vechi eșuează pe toate
încercările controlate, iar fallback-ul găsește distribuția corect constrânsă.
Nu prezentăm această reparație drept progres demonstrat la victorii.

Forecast-ul de serie și solverul independent rămân instrumente de analiză
testate, fără a influența politica acceptată. Ratele au fost calibrate numai din
cele 256 de partide inițiale V2, cu shrinkage Beta(6,4); manifestul de calibrare
păstrează ID-urile și hash-urile. Nu există antrenament pe heldout.

## Verificări și raport

- 333 de teste JVM de componentă: PASS, zero erori, eșecuri sau skip.
- 4 contracte Node: PASS, inclusiv observații fără cărți private ale adversarilor,
  11 coruperi refuzate și refuzul exportului heldout pentru antrenament.
- Whole-core JaCoCo: 785/807 linii (97,27%), 881/963 ramuri (91,48%).
- APK: build PASS. Noua versiune nu a fost instalată sau executată pe Android
  în această rundă offline. Partida Android V2 rămâne dovada istorică separată.
- `corpus-extent-report.html`: J0 Core C3 / Reporting J15, 3.222 de comparații
  native, text complet și trei capturi Android V2 inline Base64. Experimentele
  respinse sunt WARNING, cu statusul original REGRESSION păstrat.
- `corpus-report-verification.properties`: PASS pentru fidelitatea publicării,
  valorile complete expected/actual și bytes PNG. Nu relabelăm regresiile ca PASS.

Pentru format, replay și comenzi vezi [AI_CORPUS.md](AI_CORPUS.md).
