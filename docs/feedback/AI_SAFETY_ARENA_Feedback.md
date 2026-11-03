# AI / colecții / input Reporting — feedback constructiv

Revizie: 3 noiembrie 2026. Owner: J4 pentru domeniu, algoritmi, colecții și input; J0 pentru biblioteca Reporting. Interfața nu este modificată în această iterație.

| Prioritate / problemă | Dovadă și decizie | Criteriu verificabil |
|---|---|---|
| P0 — denumirea „safe” poate ascunde regresii | Independent Safety10 pierde 192 puncte și 8,5 ponderi de victorie față de V8 în evaluarea separată. Respins, chiar cu +4 contracte exacte cumulat. | Nu promovați după nume, un seed favorabil sau agregat care ascunde o familie pierdută. Criteriile rămân cele declarate înainte de experiment. |
| P0 — versiunea acceptată diferă între roluri | Expert Safety10 și independent V8 au alte freeze-uri, loturi și obiective de acceptanță. | Raportul, sursele și launcherul de test trebuie să declare exact ambele versiuni. Independentul experimental rămâne exclusiv în test. |
| P0 — informație viitoare în euristici | Licitația completă nu este observabilă la începutul licitării; rezultatul și cărțile ascunse sunt etichete de analiză. | Catalogul folosește prefixul licitațiilor la BID, toate contractele la PLAY. Niciun predictor nu primește deal-ul complet sau outcome-ul. |
| P1 — acoperiri supraestimate sau numărate de două ori | K+x, Q+xx și secvențele proprii se suprapun. Tăierea și discardul schimbă frecvențele. | Context distinct, secvențe comune, fără însumarea onorurilor. Cele 19 familii sunt ipoteze, nu un evaluator perfect al mâinii. |
| P1 — A/B confundă locul sau bugetul cu strategia | Arena folosește aceleași împărțiri, opt aranjamente, toate locurile; factorialul are bugete egale. | Raportați opt seed-uri independente, nu 128 distribuții independente. Liga mixtă nu este ablație izolată. |
| P1 — câștigul observat devine golden arbitrar | Casebook-ul de 24 de exemple include erori și succese; starea este NEEDS_INDEPENDENT_EXPECTATION. | Regresia cere expected independent și caz minim. Paritatea de snapshot dovedește binding-ul profilului, nu optimalitatea. |
| P1 — execuție întreruptă tratată ca PASS | Două JSON-uri corupte după întrerupere sunt în quarantine; tentativele originale sunt păstrate. | Audit separat, SHA și inventar; se reiau doar cazurile neînchise. Un FAIL închis nu dispare prin retry. |
| P1 — raport mare din input redundant | Colectarea curentă furnizează 56 de capturi întregi din 426 de surse, cu inventar complet și audit independent pe 578 de jocuri. | Nu rescrieți capturi, nu eliminați eșecuri din cohorte acceptate. Declarați populația, detaliile selectate, omisiunile și motivul lor. |
| P1 — checkpoint-uri cu același owner în capturile de arenă | Prima publicare a fost refuzată. Cele 128 de capturi istorice păstrează defectul și sunt inventariate ca NOT_PUBLISHABLE. Două exemple au fost reexecutate după corecția producătorului J4. | Owner distinct pentru profiluri și programul jocului; validare în producător și colector. Nu rescrieți datele vechi și nu relaxați contractul Java/J0. |
| P2 — J4-RPT-14: heap încărcare | Inputul curent este limitat înainte de Java. Loaderul generic continuă să materializeze toate capturile pe care le primește. | Îmbunătățire parțială în J4; nu închideți cerința generică de streaming și nu cereți J0 o schimbare de renderer pentru această problemă de input. |
| P2 — „V4” hardcodat în descrierea domeniului | Textul J4 al reader guide/checkpoint numea greșit baseline-ul. | Textul trimite la versiunile din parametrii reali. JAR-ul și randarea J0 rămân nemodificate. |

Închis în această iterație: build JVM fără SDK, separare quick/regression/strategy/documentation/learning/arena, criterii explicite pe rol, protecția loturilor consumate, profiluri eterogene și trei reluări de binding, catalog RO/EN contextual și calibrare inactivă, selecție de input înainte de compoziție. Rezultatele native și statice rămân distincte de acceptanța interactivă.

Pentru iterația următoare: deduceți teste mici din erorile casebook-ului, cu oracol independent; testați separat influența ponderilor de onoruri; folosiți un protocol și un lot nou. Nu lansați din nou toate turneele istorice pentru o corecție locală. Nu măriți continuu bugetul Monte Carlo fără un control de cost și o măsurare pe mâini 2–8.

Nu a fost identificat un defect nou generic al bibliotecii Reporting în această iterație. Raportul consumă J17 distribuit de J0; cerințele de conținut și selecție de mai sus rămân în J4.


## R11 — rezultate offline noi

[Analiza R11](../AI_LOSS11_ACCEPTANCE.md) și [feedbackul dedicat](AI_LOSS11_Feedback.md) păstrează variantele respinse, demonstrația locală cu 96 de distribuții și lotul nou de 512 partide. Nu există câștig statistic strict pentru noua gardă pe acest lot: ambele roluri au delte zero. Corecția locală este acceptată separat, fără regresii observate pe familii. Componentele rămân 520; turneele și diagnosticul de scor sunt opt-in. Noua publicare folosește J18, fără schimbări de JAR sau renderer.
