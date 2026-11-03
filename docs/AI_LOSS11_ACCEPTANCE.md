# R11 — pierderi A/B, regresii mici și contracte certificate

3 noiembrie 2026. Execuție exclusiv offline. Dovezile acestei iterații sunt în `C:/work/whist_back/ai-training/20261103090413-loss11`.

## Ce am învățat din pierderi

O rată mai bună de predicție a licitațiilor adversarilor nu implică un joc mai bun. Predictorul experimental estimează licitații observate, dependente de politica adversarului; nu estimează direct levatele realizabile. Variantele următoare nu sunt activate în runtime:

| Experiment | Rol | Delta puncte / victorii ponderate / contracte exacte 2–8 | Decizie |
|---|---|---|---|
| forecast11, development | Expert / independent | +116 / +1 / +12; −73 / −0,5 / −4 | Expert calificat numai pentru evaluare separată |
| forecast11, evaluare nouă | Expert | +57 / +0,5 / −2 | Respins; familia V8 pierde contracte și victorii |
| forecast12 | independent | −148 / −4 / −14 | Respins |
| belief13 | independent | −13 / +2 / −3 | Respins |
| safebid14 | independent | −97 / +2 / −8 | Respins |
| invariance15 | Expert / independent | −285 / −5,5 / −24; −109 / +2,5 / −14 | Respins |
| enum16 | Expert / independent | 0 / 0 / 0; +7 / 0 / 0 | Câștigul strict cerut lipsește |
| enum17 | independent | +7 / 0 / 0 | Câștigul strict cerut lipsește |

Rotațiile aceleiași împărțiri sunt corelate. Aceste cifre descriu loturile declarate, nu superioritate universală. Inventarul original este `protocol/rejected-strategies.json`; receptele și verdictele stricte rămân neschimbate.

## Greșeala minimă cu expected independent

Origine development: seed 76207, loc 0, independent împotriva V8, mâna 15 de șapte cărți. Atu treflă. Contractul de 1 este deja realizat. Rămân QH și 10D; în levata curentă s-au jucat 9S și JD. Lipsind pică și atu, ambele aruncări sunt legale.

V8 aruncă QH în această stare. Oracolul independent enumeră toate cele 96 de distribuții compatibile cu informația publică și toate continuările legale: această alegere permite un rezultat de 1 **sau 2** levate. Aruncarea lui 10D, cu păstrarea QH, produce exact 1 în toate continuările. Este o evitare demonstrată a riscului unei levate în plus. Partida țintită se termină tot cu 104 puncte și 18 contracte exacte înainte/după; nu pretindem puncte câștigate în acea realizare concretă.

Expert Safety10 alegea deja 10D aici. Garda comună îi oferă aceeași verificare universală; acest exemplu nu reprezintă o greșeală nouă demonstrată a Expertului.

`CertifiedContractRegressionTest` păstrează patru cazuri mici: alegerea sigură; lipsa unei garanții; contract deja imposibil; buget epuizat. Expected-ul provine din oracole independente, nu dintr-un golden arbitrar al noului jucător.

## Domeniu și cost

Modelul acceptat de licitare și joc rămâne intact. Garda poate înlocui o carte numai cu patru jucători, două cărți proprii rămase, cel mult zece cărți necunoscute și cel mult 256 de distribuții **complete**. Următoarea carte proprie este atunci forțată; nu combinăm strategii incompatibile care ar necesita cunoașterea cărților ascunse. Fiecare continuare adversă legală este verificată. Culoarea și atuul obligatoriu sunt respectate, fără obligație inventată de suprataiere.

În lipsa demonstrației se păstrează exact propunerea vechiului planner: fără schimbări de licitare, RNG, obiectiv ori bugete de eșantionare. Limita de 40.000 de intrări în căutare produce fallback, nu certitudine. Enumerarea parțială nu se tratează drept univers complet: un control cu 7.560 de distribuții trebuie să refuze certificarea la limita 256. Alte trei controale verifică exact mulțimile de 165 / 180 / 130 de distribuții pentru pool-uri de 8 / 9 / 10 cărți.

Licitația rămâne o problemă probabilistică. Cele patru regresii de onoruri promovate demonstrează levate finale inevitabile și scorul ratării; nu demonstrează automat că licitația inițială a fost greșită. Contractele 2–8 au și controale explicite de licitație legală, prefix public și ultima licitație interzisă.

## Colecții și criterii distincte

- Regresie implicită: 520 de componente, fără turnee, mining sau diagnostice cunoscute roșii. Cele 20 noi verifică licitare, binding, onoruri, universuri complete, bugete și certificare.
- Quick: subset de componente, fără turnee; numărul și durata se citesc din rularea efectivă.
- Learning R11: opt-in, contra Expert Safety10 și independent V8 înghețați. R7 rămâne disponibil numai cu protocol istoric explicit.
- `scoreConsistencyDiagnosticTest`: cercetare opt-in. Șapte contraexemple de schimbare a deciziei la translația comună a scorurilor rămân deschise; varianta de normalizare a regresat și nu a fost promovată.
- Datele brute și variantele respinse rămân în arhiva externă. Setul activ este un manifest development verificat prin SHA, cel mult 96 de jocuri, cu origini protejate și deduplicare pe traseu. Heldout nu devine material de învățare.

Criteriul strict SAFETY cere un câștig agregat de contracte exacte 2–8, fără pierderi de contracte/victorii pe familie. Corecția locală certificată are un criteriu separat, declarat înainte de evaluare: cele opt controale independente trec, regresia nativă completă trece și nicio familie nu pierde puncte, victorii sau contracte exacte. Acceptarea locală fără câștig strict nu este denumită „joc mai puternic”. Planul și sursele au fost înghețate înainte de seed-urile 107000–107015.

Raportarea publică selecții întregi de capturi și inventarul complet al surselor. Verdictul unui experiment respins rămâne FAIL, cu rândurile originale, chiar dacă o corecție locală este acceptată. Rendererul și JAR-ul aparțin J0; J4 optimizează doar inputul. Rularea offline nu produce capturi noi de Android și nu dovedește acceptanță vizuală interactivă.

## Verificarea finală pe date noi

Seed-uri 107000–107015, câte 128 de comparații pereche pentru fiecare rol: în total 256 de perechi / 512 partide de 24 de mâini. Ambii jucători au **delta zero** la puncte, victorii ponderate și contracte exacte 2–8, separat contra Expert Safety10 și independent V8. Toate partidele trec oracolul regulilor și scorului. Corecția locală certificată este acceptată în ambele roluri; criteriul statistic strict rămâne `REGRESSION` pentru lipsa câștigului strict. Lotul este consumat și exclus de la învățare.

| Verificare | Cazuri / eșecuri / erori / omise |
|---|---|
| Regresie implicită, surse identice cu cele integrate | 520 / 0 / 0 / 0 |
| Runner quick efectiv, subset al regresiei | 156 / 0 / 0 / 0 |
| Binding profiluri de arenă | 3 / 0 / 0 / 0 |
| Contracte consumator J18 fizic | 28 / 0 / 0 / 0 |
| Runner R11, selecție explicită de o pereche development | 1 / 0 / 0 / 0 |
| Lot nou gameplay, ambele roluri | 256 / 0 / 0 / 0 |

Nu adunăm colecțiile care se suprapun drept reguli unice. Scripturile au 64 de controale distincte trecute: rularea comună de 62 plus cele două controale noi de păstrare a verdictelor FAIL, cu întregul fișier de opt controale al colectorului reverificat.

Setul activ verificat are 96 de jocuri / 5.877.183 bytes, 48 de origini protejate și 757/757 de categorii ale selectorului. Arhiva brută nu a fost ștearsă sau rescrisă. Manifestul activ este development; niciun joc heldout nu este copiat în el.

Erorile de preflight sunt păstrate: prima lansare manuală a evaluării nu primise fișierul freeze și a refuzat execuția înaintea oricărei partide. Sursele nu au fost schimbate și nu s-au eliminat scoruri nefavorabile. O lansare de verificare a runnerului fără protocol existent și un quick anterior integrării rămân de asemenea istorice, fără să fie amestecate cu rezultatele finale legate de surse.
