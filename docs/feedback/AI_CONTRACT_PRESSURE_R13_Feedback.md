# R13/R14 — feedback J4, reguli și acceptanță

Owner: J4. Offline. J0 nu primește o cerere de patch: J18 fizic este consumat fără modificări.

| Prioritate | Constatare verificată | Cerință constructivă |
|---|---|---|
| P0 | Modelele ponderate pierd contracte deși victoria agregată poate crește. Mixul înghețat:−178 puncte,+3,5 victorii,−16 exacte2–8. | Nu activați modelul și nu relaxați gate-ul după citirea rezultatului. Păstrați source freeze, gate-ul FAIL și lotul consumat. |
| P0 | Egalitatea mediei−2,6 pentru AS/AD nu implică o îmbunătățire sigură în fiecare lume. | Garda activă cere minimum candidat cel puțin maximum baseline, pe fiecare lume și toate răspunsurile legale. Nu înlocuiți această dovadă cu un eșantion sau o medie. |
| P0 | Contractele recuperabile nu trebuie sacrificate pentru a arde adversari. | Activați salvarea numai după imposibilitatea numerică sau limita publică de atuuri majore. Testele trebuie să includă non-activarea și bugetul epuizat. |
| P1 | Cartea întoarsă și cărțile neîmpărțite schimbă distribuția posibilă. | Păstrați ledgerul de cărți publice și undealt separat. Nu distribuiți automat toate cărțile necunoscute adversarilor. |
| P1 | Unele vechi teste fixau identitatea numerică a estimatorului sau o carte de fallback. | În candidatul experimental, separați unitatea punctelor de identitatea modelului. Originalele496/2 rămân. În corecția activă R14, păstrați toate aserțiunile vechi:492/0, fără acele ajustări. |
| P1 | Invocările din experimente diferite pot avea aceleași ID-uri locale. | Inputul raportului păstrează capturile originale și separă loturile fără coliziuni; nu rescrieți istoria. Un viitor harness poate include candidate în ID înainte de execuție. Aceasta este responsabilitate J4, nu un defect J0 reprodus. |
| P2 |64 perechi noi ale corecției au urme identice, fără ramură nouă observată în acel lot. | Declarați zero delte și zero urme schimbate; demonstrația de funcționalitate este cazul public explicit. Nu spuneți progres statistic general sau joc perfect. |
| P2 deschis | Limita de câștig folosește numai atuurile sigur majore; nu rezolvă toate imposibilitățile pe culori laterale sau fără atu. | Extindeți numai prin contraexemple mici și limite publice demonstrabile; nu convertiți un ranking în certitudine. |
| P2 deschis | Cele7 diagnostice istorice de dependență RNG de translația scorului nu sunt reparate aici. | Nu pretindeți invarianta sau independența rotațiilor rezolvate. Orice schimbare nouă cere alt lot neatins. |

Colecții: regresie492, quick168 (subset, verificare live separată), research21 opt-in. Învățare:12 poziții publice, nu toate partidele rulate zilnic; manifestul activ100/55/759 rămâne intact. Testele UI sunt la alt owner și nu sunt executate în această iterație.
