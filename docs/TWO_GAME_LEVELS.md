# Normal și Hard

Meniul are exact două niveluri. `AiDifficulty.values()` expune numai `NORMAL` și `HARD`; lista din ecranul de configurare se construiește din aceste valori, fără schimbări de layout.

- **Normal** păstrează estimarea simplă și alegerea celei mai mici cărți legale.
- **Hard** folosește motorul Expert R11 acceptat, inclusiv posteriorul de o carte, planificarea de contract și certificatul public de final. Modelele experimentale R12 respinse nu sunt activate.

Numele vechi `EXPERT` din salvări JSON este migrat la `HARD`. Aliasul Java deprecated `AiDifficulty.EXPERT` păstrează compatibilitatea adaptoarelor vechi, dar nu este o valoare enum sau o a treia opțiune. Valorile necunoscute sunt respinse, fără a reseta silențios dificultatea. Celelalte date ale partidei nu sunt rescrise de această migrare.

## Ce este demonstrat

La o carte, AH conduce pe atu pică, cu 7S întors și fără licitații anterioare. Dintre cele 30 de cărți necunoscute, 23 nu sunt atu. Exact 1.771 dintre cele 4.060 de triple neordonate ale adversarilor nu au atu. Probabilitatea de câștig este sub 1/2, deci contractul 0 este optim pentru șansa de exactitate sub priorul uniform declarat. Hard alege 0; Normal păstrează estimarea sa 1.

AS și KS la atu pică garantează două levate. Hard licitează 2. Cazul de discard cu contractul 1 deja realizat păstrează exact contractul cu 10D, în toate cele 96 de distribuții fezabile și toate continuările legale; QH admite o levată în plus.

Acestea sunt demonstrații pe domenii finite. **Perfecțiunea generală a jocului cu informație ascunsă nu este demonstrată.** Pe celelalte stări, Hard folosește cel mai puternic motor acceptat; eticheta Hard nu transformă o estimare în certitudine.

## Verificare offline

486 de componente de domeniu, 7 de cercetare separate și 17 teste ale aplicației trec. Reducerea față de vechiul număr de cazuri reflectă eliminarea celor 40 de combinații parametrizate ale celei de-a treia dificultăți, plus șase contracte noi pentru niveluri/migrare/dispatch. O partidă completă de 24 de mâini produce aceleași licitații, cărți și scoruri în Hard și în motorul Expert R11 înghețat.

APK debug 0.1.1 este construit offline în staging propriu. Build-ul și testele JVM nu sunt prezentate drept execuție Android pe device. Interfața păstrează layoutul lucrat separat; numai maparea etichetelor și citirea dificultății salvate au fost adaptate.

Rezultatele experimentelor de forță R12 rămân [respinse și auditate separat](AI_ROUND12_ANALYSIS.md).

## Dovezi livrate

[Raportul Extent J18](C:/work/whist_back/ai-training/20261103-r12/reports/final-extent-report.html) conține 27 de capturi selectate din 365, 2.802 comparații și inventarele sursă. La pregătirea inputului sunt reluate independent 514 înregistrări de partidă: 512 din evaluarea finală R12 și perechea completă Hard/R11. Raportul are 6.272.901 bytes. Auditul structural trece; acceptanța vizuală interactivă nu a fost rulată. FAIL-ul capturat este cel real al gate-ului de forță R12, nu un eșec al migrării Normal/Hard.

[APK-ul debug 0.1.1 verificat](C:/work/whist_back/ai-training/20261103-r12/two-levels/WhistLab-0.1.1-normal-hard-debug.apk) și [sursele exacte](C:/work/whist_back/ai-training/20261103-r12/two-levels/source/whist-lab-app) sunt păstrate extern. Acest APK este snapshotul testat, nu distribuția curentă a lucrului de UI: între timp, ownerul interfeței a modificat integrarea pauzei levatei în MainActivity și versiunea la 0.1.2. Aceste schimbări concurente au fost păstrate. Enum-ul, dispatchul, etichetele și parserul salvării coincid byte-cu-byte cu snapshotul testat. Nu s-a instalat sau înlocuit APK-ul de pe device.

## R14 — motorul curent

Hard păstrează motorul R11 și adaugă numai [garda de dominanță publică certificată](AI_CONTRACT_SALVAGE_R14.md). Regresia curentă este492; modelele ponderate R13 sunt respinse și numai în research21. Normal și migrarea EXPERT→HARD rămân neschimbate. APK-ul0.1.1 de mai sus este un snapshot istoric; această iterație modifică numai algoritmul JVM și nu livrează un APK nou.
