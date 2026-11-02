# Reanaliză AI și adversar independent — protocol

Profil: Whist românesc, patru locuri, un adversar de evaluare și trei boți.
Nu promitem victorii împotriva unui jucător perfect cu informație ascunsă.
„Perfect” poate fi verificat numai pe domenii finite și informații declarate,
de exemplu rezolvarea exactă a unui final cu toate cărțile unei ipoteze cunoscute.

Înainte de schimbarea politicilor am înghețat `LegacyAiPolicy` și
`LegacyMonteCarloAi` în sursele de test. Nu sunt împachetate în APK.
Benchmark-ul vechi 1000…1007 rămâne regresie; nu mai este numit holdout nou.

- Dezvoltare: seed-uri 200…207, locul evaluat rotit prin 0…3.
- Evaluare finală separată: 40000…40015, aceleași distribuții și locuri pentru
  versiunile comparate; păstrăm și înfrângerile.
- Adversari: Hard și Expert înghețate, plus un planner independent cu intrare
  limitată la mâna proprie, cărți publice, oferte, levate și scoruri.
- Raportăm puncte, contracte exacte și victorii/egalități; câștigul colectiv al
  trei boți nu dovedește că un bot este mai puternic decât adversarul unic.
- În Android, plannerul controlează numai locul uman prin UI. Cititorul de
  checkpoint pentru audit poate vedea toate mâinile; proiecția pentru decizie
  exclude structural mâinile adversarilor. Oracolul regulilor rămâne separat.
- O modificare este acceptată după contraexemplu/regresie, verificarea regulilor
  și comparația cu versiunea înghețată. Seed-urile finale nu se schimbă după scor.

Constatări inițiale: Hard conduce puternic inclusiv când nu mai dorește levate;
simularea de licitare Expert setează țintele tuturor la mărimea mâinii;
rollout-ul folosește o politică greedy chiar în finaluri mici; candidatele
resamplează redundant aceleași distribuții. Acestea sunt ipoteze de îmbunătățit,
nu declarații de forță dovedită înaintea experimentelor.

## Rezultate clasice și noul profil cu serii

Candidatele pure de început au regresat și sunt păstrate în dovezi. Candidata 05
(probabilitate exactă la o carte, distribuții fezabile comune, amestec 50/50
final exact/euristic) a obținut pe dezvoltare 5.177 vs 5.074 puncte, iar pe
128 perechi finale 10.380 vs 10.003, cu 38,5 vs 30 victorii ponderate la egalitate.
Aceste rezultate sunt pentru profilul clasic fără serii, nu pentru bonusul nou.

Înainte de rularea noului profil declarăm separat: dezvoltare seed-uri 500…503,
evaluare finală 50000…50015, loc rotit 0…3, familii OLD și CHALLENGER, bonus
pozitiv de serie 10 și penalizare −5. Aceleași distribuții, locuri și adversari
pentru cele două politici comparate. O regresie se raportează, fără schimbarea
retroactivă a seed-urilor finale. Sunt scenarii finite de evaluare, nu dovadă
de joc perfect.

Profilul nou folosește contoare publice și valoarea aleasă în meniul jocului.
Compatibilitatea hash/seed a plannerului independent pentru intrările clasice
este verificată separat față de forma V1 înghețată. Câmpurile temporale din
benchmark măsoară cea mai lentă decizie a oricărui actor din joc, nu exclusiv
eroul; fișierele vechi păstrează numele inițial `newMaxDecisionMillis`.

Profilul cu +10 a trecut evaluarea finală pe 128 de perechi: **11.074 vs 10.216**
puncte, **38,5 vs 29,5** victorii ponderate. Față de OLD: 5.570/5.205 puncte și
17/16 victorii; față de CHALLENGER: 5.504/5.011 și 21,5/13,5. Sunt rezultate
finite, nu garanție de victorie. La dezvoltare: 2.443/2.348 puncte; progres în
ambele familii, fără progres al numărului total de victorii.

Experimentul 07, care includea noii parametri în seed-ul ambilor selectori,
regresase în familia CHALLENGER. Corecția păstrează lumile ipotetice comune când
se schimbă numai recompensa, folosind-o în utilitate. Fluxul plannerului cu
serii s-a schimbat și el între aceste experimente; nu comparăm scorurile brute
între ele ca un A/B cu adversar identic. În fiecare experiment perechea OLD/NEW
are aceeași politică adversă. Ambele surse și rezultatele sunt arhivate.

Repetarea profilului clasic după adăugarea seriilor a păstrat identice toate
cele **1.423** de valori comparate: scoruri individuale/finale, contracte exacte,
victorii, distribuții și locuri. Timpii sunt excluși din comparația deterministă.
Dovezi: `C:\work\whist_back\ai-training\20261102-143525\`, în special
`series-heldout/`, `classic-stream-parity.json` și experimentele păstrate.

## Runda V3 închisă — corpus și selecție separată pe rol

Protocolul și source freeze sunt în
`C:/work/whist_back/ai-training/20261102-round3/`. Training: 800…863;
development: 2000…2007; heldout: 60000…60015, deschis numai după înghețarea
Expert candidate05. Expertul trece ambele familii fixe: win share agregat 31
față de 28 pe 128 de perechi, puncte 10.640 față de 10.649. Plannerul nu are
progres demonstrat; schimbările de forță sunt respinse, rămâne fallback-ul de
fezabilitate. Rezultatele detaliate, cele șase experimente păstrate și limitele
sunt în [acceptanța V3](AI_CORPUS_ROUND3_ACCEPTANCE.md). Heldout-ul este acum
consumat și nu se reutilizează ca evaluare nouă după ajustări.

## Runda V4 — închisă pentru tuning

Training și calibrare exclusiv din development-ul anterior; două iterații pe
4500…4507, apoi freeze bid02 înainte de71000…71015. Câte128perechi heldout per
rol: Expert+190puncte/+4win share, planner+242/+5. Ambele familii fixe per rol
trec. Setul final este consumat și nu se reutilizează ca evaluare nouă după
ajustări. [Dovezi, timpi, raport și limita TAB MCP](AI_CORPUS_ROUND4_ACCEPTANCE.md).
