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
este verificată separat față de forma V1 înghețată. Campoanele temporale din
benchmark măsoară cea mai lentă decizie a oricărui actor din joc, nu exclusiv
eroul; fișierele vechi păstrează numele inițial `newMaxDecisionMillis`.
