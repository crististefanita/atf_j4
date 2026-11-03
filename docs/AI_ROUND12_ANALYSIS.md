# R12 — licitare, continuări și informație disponibilă

3 noiembrie 2026. Offline; sursele UI, emulatorul și biblioteca Reporting nu sunt modificate de această iterație. Dovezi externe: `C:/work/whist_back/ai-training/20261103-r12`.

## Lotul de analiză

Din cele 128 de jocuri baseline ale experimentului development enum16 au fost extrase 758 de ratări de contract la 2–8 cărți. La 6–8 cărți, 274 de ratări sunt prin levate **peste** contract. Aceasta nu dovedește automat sublicitare: aceeași eroare finală poate proveni dintr-o licitație mică, un discard greșit, un atu obligatoriu sau o continuare modelată prea optimist.

Nu folosim jocurile heldout pentru alegerea regulilor. Baseline-ul R12 este livrarea R11 acceptată: Expert Safety10 plus certificat public și independent V8 plus certificat public. Zece clase de test înghețate păstrează exact aceste politici. Două partide complete de binding verifică toate cele 24 de mâini, licitații, cărți și scoruri față de sursa acceptată. R11/V10 și V8 fără certificat rămân referințe istorice distincte.

## Screening și replicare păstrate integral

Delta este față de baseline-ul acceptat R11. Scorul agregat favorabil nu poate ascunde o familie de adversari pierdută.

| Candidat / development | Expert: puncte / victorii ponderate / exacte 2–8 | Independent: aceleași metrici | Decizie |
|---|---|---|---|
| sim-all, 4 seed-uri | +16 / +1 / −4 | −63 / −3 / −6 | Respins |
| mixture, 8 seed-uri | +3 / +0,5 / −5 | +39 / +1 / +4 | Respins; pierdere de exacte într-o familie |
| paired-sampling, 16 seed-uri | +159 / +1,5 / +15 | +61 / 0 / +10 | Respins; familiile proprii pierd victorii și exacte |
| bid-only, 8 seed-uri | 0 / 0 / 0 | +205 / +1 / +20 | Respins; independentul pierde o familie |
| late-response, 4 seed-uri, numai independent | — | +18 / 0 / +1 | Respins; familia V11 pierde 8 exacte |

Experimentele de continuare nu sunt dovezi că o mutare ar fi optimă în jocul real. Varianta late-response optimizează împotriva unei continuări ipotetice determinate; acea optimizare poate supraestima informația disponibilă și a regresat contra independentului V11.

Auditul sursei a găsit o neconcordanță în protocolul paired-sampling: independentul a folosit efectiv **48 eșantioane BID și 48 PLAY**, deși descrierea intenționată menționa 96 PLAY. Receptele și hash-urile originale rămân neschimbate; această variantă nu este denumită test de 96 PLAY și nu este promovată. Ablația bid-only confirmă efectul pe sursa reală. Expertul a avut efectiv 64 PLAY și aceeași limită de 128 BID.

Suita nativă a mixture are un FAIL păstrat: un test istoric cerea identitatea mediilor numerice cu V6, deși modelul de continuare fusese schimbat. Nu transformăm acea rulare în PASS și nu o folosim drept calificare. Cerința permanentă este ca mediile de puncte să păstreze unitatea și semantica declarate, nu să aibă invariabil aceeași valoare după schimbarea unui estimator.

## Lecția de două levate, verificată manual

Atu cupă, contract 1. Jucătorul are **8H și AD**. Lumea ipotetică este:

| Loc | Cărți | Contract |
|---|---|---|
| 0 | 9H, 7C | 1 |
| 1, jucătorul analizat | 8H, AD | 1 |
| 2 | QH, KD | 1 |
| 3 | 10S, JS | 0 |

Conducerea lui AD: locul 2 răspunde KD; locul 3 aruncă JS; locul 0 taie obligatoriu cu 9H. Apoi 7C este tăiat cu 8H și supratăiat cu QH. Jucătorul rămâne la zero: −1 punct.

Conducerea lui 8H consumă QH și 9H. Locul 2 câștigă și conduce KD; AD câștigă ultima levată: exact contractul, +6. Regresia verifică toate rotațiile, bonusurile configurabile +5/+10 la al cincilea succes și −5 la al cincilea eșec. Oracolul este separat, în JavaScript, cu reguli numerice proprii.

Această demonstrație explică o eroare a **simulatorului intern pe o lume declarată**. Mâinile complete sunt dovezi post-game și date pentru testul intern; nu sunt oferite jucătorului real. Șapte lecții similare au fost reduse din jocuri pierdute, câte una pentru fiecare mărime 2–8. Expected-ul lor privește modelul declarat, nu optimalitatea cu informație ascunsă.

## O continuare trebuie să respecte informația publică

Două lumi pot avea aceeași mână proprie, același istoric public și aceleași contracte, dar acțiuni optimiste diferite dacă simulatorul privește mâinile ascunse. A media cele mai bune alegeri făcute **după** dezvăluirea fiecărei lumi nu este același lucru cu a alege o singură carte **înainte** de dezvăluire.

Contraexemplul didactic are 9S/AD rămase, atu cupă și contractul deja făcut. În prima lume numai 9S păstrează contractul; în a doua numai AD. Cu ponderi egale declarate pentru acest exemplu, un selector care vede lumea ar raporta 6 puncte în ambele; orice singură alegere publică are media 2,5. Ponderile egale sunt o construcție de test, nu un posterior calibrat pentru partidele reale.

Varianta consistent-tail verifică două schimbări:

- Expert: solverul complet al unei lumi este limitat la cel mult două cărți per loc, unde următoarea carte proprie după alegerea curentă este forțată. Continuarea greedy obișnuită rămâne aceeași; noua limită nu este prezentată drept soluție generală a jocului cu informație ascunsă.
- Independent: în simularea ultimelor trei cărți, propriile alegeri folosesc mâna proprie, cărțile publice, contractul și numărul cărților rămase. API-ul noii continuări nu poate reprezenta mâinile ascunse ale adversarilor. La conducere pentru evitarea unei levate compară scoruri de amenințare calculate din marginale combinatorii; acestea sunt un **criteriu de ordonare**, nu probabilități calibrate de câștig. Culoarea și tăierea obligatorie rămân constrângeri exacte.

Validarea statistică a acestei variante este separată de corectitudinea proiecției de informații. Un oracol sau un argument conceptual bun nu este suficient pentru promovare.

## Colecții și audit

Turneele rămân opt-in. Cazurile mici, exemplele de învățare și populațiile finale sunt colecții distincte. Arhiva brută este externă; setul activ development are maximum 96 de jocuri și manifest SHA. O rotație nu este un seed independent. Un lot final consumat nu devine date de antrenament.

Reporting aparține J0. J4 livrează selecții de capturi întregi, inventare, surse și verdicte originale; nu rescrie gate-urile FAIL în PASS. Auditul static al HTML-ului și execuția algoritmilor rămân rezultate diferite de verificarea vizuală interactivă ori Android.
