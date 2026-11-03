# Colecții de teste și promovarea exemplelor

## Colecțiile curente — 3 noiembrie 2026

Sursa curentă este `config/test-collections.json`. Runnerul `scripts/run-whist-collection.ps1` expune quick (136 de cazuri, subset), regression (500), strategy, documentation, learning și arena. Learning folosește RoundSeven/schema7; vechile secțiuni R6 sunt istoric. Arena este opt-in, cu patru strategii distincte per masă; cele trei controale de reluare a profilurilor sunt în afara regresiei zilnice. Build-ul JVM din `whist-lab-app/algorithm-build` nu configurează Android SDK.

[Protocolul și rezultatele A/B](AI_STRATEGY_ARENA.md) descriu aceleași împărțiri, locurile rotite, criteriile distincte pe rol și cele opt seed-uri independente ale ligii. Catalogul [RO](rules/AI_HAND_PATTERNS_RO.md) / [EN](rules/AI_HAND_PATTERNS_EN.md) separă ipotezele de onoruri de regulile jocului.

Setul activ curent are 94 de jocuri (5.719.972 bytes), 43 de origini protejate și 757/757 de categorii declarate. Manifestul și jocurile sunt verificate prin SHA-256. Aceasta este acoperirea taxonomiei selectorului, nu acoperirea tuturor stărilor Whist. Restul arhivei rămâne extern, inclusiv pierderile; heldout nu intră în setul de învățare.

`scripts/whist-report-input.mjs` construiește inputul limitat înaintea compoziției Java: capturi copiate integral, inventar de populație, suite native și audit independent al jocurilor. Politica de publicare a rularii este explicită, cu detalii complete pentru selecția furnizată și buget mic pentru amprentele opționale. Defectele istorice ale capturilor se declară, nu se normalizează. J0 deține în continuare JAR-ul și rendererul.

Colecțiile sunt definite în config/test-collections.json. Configurația prezentării este separată: config/game-evidence.yml; setările neutre Reporting rămân în atf.yaml. Reporting este biblioteca fizică J16 din lib, deținută de J0. J4 selectează și explică inputul; nu modifică HTML/CSS/JS ori JAR-ul.

| Colecție | Când | Țintă orientativă | Ce verifică |
|---|---|---|---|
| quick | La modificarea regulilor/scorului/help-ului | 2 min | Limite, atu obligatoriu, scor/serii, checkpoint invalid, exemple RO/EN și posteriorul de o carte |
| regression | Înainte de promovare | 5 min | Suita offline completă: oracole, modele, istoricul stării, strategii și regresii; fără turnee de învățare |
| documentation | La schimbarea regulilor și explicațiilor | 1 min | 32 de exemple în RO/EN; expected literal independent, actual și verdict capturate; ajutor generat din catalogul folosit de Android |
| learning | Numai pentru un experiment declarat | 15 min | Candidate vs politici înghețate, toate locurile rotite, toate JSON-urile inclusiv pierderile; criteriu de promovare separat |
| e2e | Acceptanță explicită pe device, sub lease exclusiv | 45 min | Meniu, licitare, carte legală, levată, scor, mâna următoare, final, reluare; PNG integral inline Base64 |

Duratele sunt bugete de planificare, nu timeout-uri care transformă rulările incomplete în PASS. Se măsoară timpul real. Ordinea recomandată: quick la editare; regression înainte de promovare; documentation la schimbări de reguli/help; learning numai când există o ipoteză nouă; E2E după acceptanța componentelor. Nu adăugăm toate seed-urile istorice în regresia zilnică.

## Runner izolat

Din PowerShell: scripts/run-whist-collection.ps1 -Collection quick. Valorile sunt quick, regression, documentation, learning. Runnerul copiază sursele într-un TMP propriu, salvează SHA-256 și rezultate native într-un folder nou din C:/work/whist_back/collections. Nu construiește în checkout, nu oprește procese străine și nu publică implicit un HTML masiv. Learning cere Candidate și CorpusRoot explicite; heldout cere și CandidateFreeze anterior deschiderii lotului. Erorile păstrează logurile și rezultatul FAIL.

E2E rămâne în atf-test-android, opt-in și serializat. Folosiți runner-ele Android existente numai cu lease și staging propriu; nu considerați skip-urile sau capturile istorice drept o execuție nouă.

## Din învățare în regresie

1. Păstrăm toate partidele JSON și receptele. Loturile heldout nu se exportă pentru antrenament.
2. scripts/whist-casebook.mjs selectează exclusiv din development: cel mai mare câștig și cea mai mare regresie pentru fiecare familie, plus exemple cu erori de contract la fiecare dimensiune 2–8. Partidele selectate și observațiile publice rămân în whist_back.
3. Eticheta acțiunii observate nu înseamnă mutare optimă. Un câștig de meci nu demonstrează că fiecare alegere a fost corectă. Outcome-ul viitor este separat de observation; adversarul nu primește cărțile private.
4. Un exemplu intră în regresia implicită doar când avem un expected independent: regulă explicită, soluție exactă de final mic sau invariant justificat. Reducem la cel mai mic contraexemplu util; îi dăm ID, regulă/defect, origine și motiv. Nu fixăm alegerile unei politici ca golden doar fiindcă a câștigat.
5. Nu relaxăm gate-ul după eșec și nu alegem numai familiile favorabile. Înghețăm candidatul înainte de lotul nou; un heldout consumat devine istoric de evaluare, niciodată un nou test final neatins.

## Raportul ca document de audit

AUTO păstrează toate invocările mici, până la pragurile inclusive configurate de populație, comparații și context. Pentru o populație mare selectează primele ID-uri deterministe per familie, toate invocările non-PASS și gate-ul întregii populații. Se păstrează ordinea execuției originale. Alegerea exemplelor pentru raport nu depinde de cât de frumos arată scorul; casebook-ul pedagogic, care include explicit extremele pozitive și negative, este o colecție diferită.

SUMMARY declară selecția de exemple. FULL păstrează toate invocările furnizate; nu face downgrade silențios. În toate profilele, fiecare invocare inclusă are comparațiile complete, inputul obligatoriu, regula, verdictul original, cauza și timing-ul capturat. Titlurile includ identitatea invocării ca să nu se contopească scenarii cu același nume semantic.

Raportul publică executat/selectat/omis, inventarul ID-urilor și politica efectivă. Un HTML selectat NU pretinde audit detaliat al jocurilor omise. Contextul tehnic comun se publică o dată; amprentele opționale se deduplică și se păstrează ca înregistrări întregi numai în bugetul cumulat. Fiecare omisiune are număr și motiv.

Preview-ul nativ rămâne 240. Bugetul body este pozitiv și explicit în atf.yaml; publisherul refuză INCOMPLETE înainte de a crea HTML dacă o dovadă obligatorie nu încape. Nu se taie JSON-ul, nu se ascund originale în DOM și nu se mută inputuri obligatorii în linkuri externe. Pentru un caz foarte mare trebuie selectate granițe semantice mai mici ori mărit bugetul configurat; această implementare nu fragmentează automat o singură structură arbitrară.

Auditul static verifică exact ownerul, rândurile/ultima comparație, valorile complete, outcome-ul, timpul, glosarul și inventarul selecției. Randarea interactivă este o acceptanță separată. Migrarea publisherilor legacy și false PASS-ul vechi de resume rămân criterii deschise în docs/feedback/ATF_Android_Alignment_Feedback.md; noul pipeline nu le declară rezolvate prin distribuirea J16.

## Ajutor RO / EN

Catalogul HowToPlay din game-core este sursa comună pentru meniul Android și docs/rules/WHIST_RO.md, WHIST_EN.md. În meniu: Ajutor / How to play → Română / English. Limba este păstrată la redeschidere. Numai ajutorul este bilingv; restul interfeței are în continuare etichetele existente. Preferința limbii nu modifică partida salvată, scorul sau setările jocului.

Colecția documentation verifică obligația de culoare chiar după un atu, tăierea obligatorie fără obligație de suprataiere, discard/fără atu, câștigătorul, ultima licitație, scor exact zero și ratare în ambele sensuri, bonusurile +5/+10, ratarea −5, mâna de o carte care păstrează/rupe seria, programul și pachetul redus.

## Fișiere și acoperire UI

C:/work/1/atf_j4/atf-test-android/artifacts a fost mutat în C:/work/whist_back/atf-test-android-artifacts-20261102. Toate cele 38 de fișiere au fost verificate înainte/după prin cale relativă, dimensiune și SHA-256. Rapoartele istorice nu au fost rescrise; eventualele lor căi locale vechi rămân istorice.

| Flow Android | Test / stare |
|---|---|
| Setup, bonus 5/10/classic, reluare opțiuni | RomanianMenuOptionsE2eTest existent |
| Help RO/EN, atu/suprataiere, redeschidere limbă | Adăugat în același test; compilarea și execuția device se declară separat |
| Partidă românească Expert completă, scor pe fiecare mână | CompleteRomanianExpertE2eTest și WhistLabRomanianExpertE2eTest existente |
| Original ↔ Whist Lab, replay | CompleteOriginalMatchE2eTest / CompleteWhistLabMatchE2eTest existente |
| Resume raster legacy | Defect de oracol cunoscut; nu este gate de promovare |

Această schimbare produce componente și APK offline. Nicio acceptanță Android nouă nu este dedusă din build ori din documentație.

## R6 — small regressions and protected evaluation

Quick/default regression now include the 20 late-card counterexamples and guards. Four statistical-model tests are research-only, excluded by default. Historical R6 learning used roundSixTest and required -ProtocolFile pointing to a V6 JSON plan with development/heldout.firstSeed and seeds; heldout also requires -CandidateFreeze. Source hashes and the external holdout-use registry prevent candidate or range reuse. alwaysIncludeCollections in game-evidence.yml keeps regression/documentation cases complete even when the learning population is sampled. Existing rowsPerGroup, fieldsPerGroup and optionalMaxItems remain effective. See AI_ROUND6_ANALYSIS.md for measured outcomes and the explicit lack of a strength claim.
