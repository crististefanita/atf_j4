# Runda V4 — licitații publice, două politici și partide noi

Run: `C:\work\whist_back\ai-training\20261102-171252-round4\`.
Profil: Whist românesc, patru jucători, serie exactă +10 / serie ratată −5.
Totul a rulat fără ADB, scrcpy sau interfața jocului.

## Schimbarea algoritmilor

Expertul și plannerul independent condiționează acum licitația de o carte de
ofertele deja publice. Enumerarea distribuțiilor posibile folosește numai mâna
proprie, cartea de atu publică și licitațiile cunoscute. Oferta finală forțată nu
este tratată ca indiciu despre puterea cărții.

Modelul folosește 3.766 de observații deduplicate din 307 fișiere development
anterioare acestei runde. Nu folosește heldout. Parametrii sunt rate agregate
pe priorul combinatoric și poziția lider/follower, cu Beta(1,1); nu sunt seed-uri
sau distribuții memorate. Manifestele păstrează fișierele și hash-urile.

Cele două implementări ale posteriorului sunt separate: Expert folosește
tipurile engine-ului, plannerul folosește numai cărți numerice și date publice.
Oracolul de reguli/scor este separat de ambele politici.

## Iterații și selecție

Referințele sunt politicile V3 înghețate înainte de schimbare, inclusiv fallback-ul
de fezabilitate al plannerului. Paritatea este verificată pe o partidă completă:
376 de decizii de carte și 96 de licitații; singura schimbare intenționată a
politicilor active este în licitațiile de o carte.

| Iterație | Model | Expert: puncte / win share în plus | Planner: puncte / win share în plus |
|---|---|---:|---:|
| bid01 | likelihood limitat la 0,05…0,95 | +83 / 0 | +147 / +1,5 |
| bid02 | likelihood limitat la 0,01…0,99 | +107 / 0 | +171 / +1,5 |

Fiecare iterație are 128 de perechi development, seed-uri 4500…4507 și toate
cele patru locuri. `bid02` este ales înainte de deschiderea heldout-ului.
Politica nu a fost modificată pe baza rezultatelor finale.

## Verificare pe distribuții noi

Heldout: seed-uri 71000…71015, locuri rotite, adversari V3 fixați. O pereche
execută două partide complete: referința și candidatul în același loc și pe
aceleași distribuții. Criteriul cere win share fără regresie în fiecare familie,
scădere medie de scor cel mult două puncte și progres strict agregat per rol.

| Rol | Adversari | Perechi | Win share V3 → V4 | Puncte V3 → V4 |
|---|---|---:|---:|---:|
| Expert | 3 Expert V3 | 64 | 16 → 16 | 5.415 → 5.459 |
| Expert | 3 planner V3 | 64 | 20 → 24 | 5.482 → 5.628 |
| Planner | 3 Expert V3 | 64 | 23 → 23,5 | 5.555 → 5.640 |
| Planner | 3 planner V3 | 64 | 16 → 20,5 | 5.412 → 5.569 |

Ambele roluri trec criteriul. Expert: **+190 puncte și +4 win share**; planner:
**+242 puncte și +5 win share**, pe câte 128 de perechi. Egalitățile împart
victoria. Sunt 16 seturi noi de distribuții, cu observații corelate prin rotație
și familie; rezultatul finit nu demonstrează joc perfect sau victorie inevitabilă.

## Dovezi și testare

- 341 de teste game-core PASS, zero erori/eșecuri/skip; includ posteriorul,
  oferta forțată, două implementări și paritatea referințelor.
- 256 de invocări JUnit parametrizate heldout PASS, cu două partide fiecare.
- 8 contracte Node PASS: integritate/replay, observații fără cărți ascunse,
  export heldout refuzat, familii fixe, regresie și receipt lipsă/contradictorie.
- Contractele consumer de raportare verifică ownerul, ordinea, ultimul rând,
  FAIL→PASS, celula actual, timing, SKIP/ERROR, input lipsă și referințe inline.
- Whole-core: 800/822 linii și 966/1051 ramuri măsurate de JaCoCo.
- APK-ul este construit offline; nu există acceptanță nouă pe Android în V4.

Corpusul are acum **2.161 de partide**, 117 seturi distincte de distribuții,
51.864 de mâini și 812.536 de cărți: 1.393 development și 768 heldout.
Sunt păstrate toate partidele, inclusiv înfrângerile. Setul 71000…71015 este
consumat; ajustările viitoare cer un set nou, rezervat înainte de tuning.

## Raport Extent și limita randării

`learning-heldout-extent-r2.html` conține 257 de invocări reale: 256 de perechi
JUnit și o evaluare Node a criteriului. Nu adaugă un test PASS fictiv de rezumat.
Cele **136.202 comparații** au context, verdict și ordine pe owner. Timpul capturat
al jocurilor/auditului este distinct de publicare; nu sunt fabricate faze temporizate.

Compoziția reutilizabilă este în main Java, prin API-ul Reporting. Preview-ul
rămâne 240; valorile lungi sunt complete în același checkpoint. `contextPointer`
indică datele inline din acel checkpoint, iar `valueRef` harta sa locală. Internarea
compară identitatea payload-ului, fără a deduce expected-ul din PASS. Acceptorul
decodifică referințele și verifică exact valorile originale. Nu există referințe
cross-owner sau fișiere externe necesare auditului.

Prima publicare a depășit 2 GiB în FreeMarker. Rerularea folosește internare fără
pierderi și 3 GiB; HTML-ul are 144.325.590 bytes. Primul acceptor a refuzat greșit
preview-ul JSON compactat de renderer; controlul corect compară tokenii JSON,
păstrând cifrele și conținutul string-urilor. Încercările inițiale sunt arhivate.

Biblioteca fizică: **Core C3 / Reporting J15**, SHA-256 Reporting
`7b3c9df7d22b4dd50eefeb1e72358b4baec35cc3b24ac13d531bd1a101d3adf0`.
`reporting-watch.jsonl` înregistrează citirile live ale POM-ului, binarului și
feedback-urilor. Nu se consumă automat un JAR candidat din staging-ul altui owner.

Auditul DOM incremental trece pentru proprietari, valori, ordinea/multiplicitatea
rândurilor, verdict și timing. **Randarea interactivă nu este acceptată:** TAB MCP
refuză protocolul `file:` prin politica URL. Nu s-a folosit server local, alt
browser ori altă cale de ocolire. Starea rămâne
`STRUCTURAL_AUDIT_PASS_BROWSER_PENDING`, distinctă de rezultatul jocurilor.
