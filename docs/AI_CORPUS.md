# Corpus de partide și antrenament fără interfață

Datele sunt în `C:\work\whist_back\training-corpus\`, în afara J4. Un fișier
JSON simplu reprezintă o partidă completă. Prima partidă Android verificată este
`games/development/android-round-v2-21049369348797.json`: 24 de mâini, 96 de
licitații, 94 de levate și 376 de cărți; scor `[63,90,110,74]`.

La închiderea rundei V4: **2.161 de partide, 117 seturi distincte de distribuții,
812.536 de cărți**, toate validate. Sunt 1.393 în development și 768 în heldout.
Rezultatele, politicile îmbunătățite și limita auditului vizual sunt în
[acceptanța V4](AI_CORPUS_ROUND4_ACCEPTANCE.md). [V3](AI_CORPUS_ROUND3_ACCEPTANCE.md)
rămâne o referință istorică separată.

Fiecare mână are distribuțiile, dealerul, atuul/cartea întoarsă, licitațiile în
ordine, cărțile fiecărei levate în ordine, câștigătorul și scorurile pe locuri.
Scorul separă baza contractului, premiul/penalizarea seriei, totalul și contorul.
Cărțile folosesc coduri `AS`, `KH`, `10C`, `7D`; S/H/C/D sunt pică/inimă/treflă/caro.
Seed-ul este string, pentru a păstra exact un întreg Java long în orice cititor.

Distribuțiile complete sunt **date după partidă**. Nu sunt intrare pentru
politica de joc: aceasta primește numai mâna proprie și informația publică.
Această separare rămâne testată chiar când recorderul poate vedea tot jocul.

## Colectare și validare

`ComponentCorpusCollectorTest` rulează fără ADB, emulator sau capturi. În prima
colectare sunt 64 seed-uri 800…863, fiecare cu locul plannerului rotit 0…3:
256 de partide cu cele două politici V2 înghețate. Înfrângerile sunt păstrate.
`RoundTwoFreezeParityTest` verifică copiile politicilor înainte de modificări.

```powershell
.\gradlew.bat --no-daemon --console=plain :game-core:corpusTest `
  '-Pwhist.corpusDir=C:\work\whist_back\training-corpus' `
  '-Pwhist.corpusSeeds=64' '-Pwhist.corpusFirstSeed=800' `
  '-Pwhist.corpusVariant=V2_BASELINE'
```

Comanda se rulează dintr-un staging al `whist-lab-app`, pentru a ține build-ul
în TMP. Fișierele închise sunt imutabile; rerularea verifică și sare peste ID-urile
existente. `scripts/whist-corpus.mjs` exportă partida Android și verifică JSON-ul
independent de motor: proprietatea cărților, culoare/atu, actori, câștigători,
licitația finală, pachetul, programul mâinilor, scorul și seriile.

Opt mutanți ai primei partide sunt refuzați: carte duplicată, actor greșit,
câștigător greșit, total greșit, bonus greșit, resetare greșită, levată trunchiată
și suma interzisă la ultima licitație. Receipt-ul este în `quality-checks/`.

Pentru citire și replay fără JVM sau Android, din J4:

```powershell
node scripts/whist-corpus-cli.mjs validate C:\work\whist_back\training-corpus
node scripts/whist-corpus-cli.mjs replay C:\work\whist_back\training-corpus\games\development\android-round-v2-21049369348797.json
node --test scripts/whist-corpus.test.mjs
```

`examples <game.json> <new-file.jsonl>` exportă 472 de decizii pentru o partidă
de patru jucători: 96 de licitații și 376 de mutări. Fiecare rând separă
`observation`, `action` și `outcome`. Doar `observation` este intrarea AI:
mâna proprie, licitațiile deja anunțate, cărțile publice, contoarele și programul
public. Nu conține seed-ul sau cărțile adversarilor. `outcome` este eticheta
cunoscută după partidă; nu se transmite jucătorului. Exportul pentru antrenament
refuză partiția heldout. Exporturile existente nu sunt suprascrise.

Exemplul deja generat este
`C:\work\whist_back\training-corpus\examples\android-round-v2.jsonl`.
`reference-audit.json` din run-ul V3 conține 2.085 de comparații detaliate ale
aceleiași partide, folosite și în tabelele native Extent.

## Separarea datelor

`games/development` poate fi folosit pentru îmbunătățire; `games/heldout` este
evaluare rezervată; `games/reference` păstrează referințe. `index.json` este
derivat și poate fi refăcut. Statisticile `policies` folosesc numai development.
Aceleași distribuții nu pot apărea în partiții diferite: validatorul compară
amprenta tuturor distribuțiilor, nu numai numele seed-ului.

Un seed rotit prin patru locuri dă patru partide, dar nu patru distribuții
independente. Numărătorile pe politică sunt expuneri pe locuri: un joc cu trei
Expert contribuie cu trei expuneri Expert. Nu prezentăm aceste observații drept
trei meciuri independente sau dovadă de joc perfect.

Pentru runda V3, protocolul este înghețat înainte de evaluare în
`C:\work\whist_back\ai-training\20261102-round3\PROTOCOL.json`: dezvoltare
2000…2007, evaluare finală 60000…60015, ambele cu locuri rotite. Se măsoară separat
progresul plannerului și al Expertului față de adversari V2 fixați.

R6 evidence and rejection ledger: [analysis RO/EN](AI_ROUND6_ANALYSIS.md). The local fix has 20 new captured regression cases; 256 fresh full games showed no score/win change. The statistical model failed on its separate 512-game evaluation and was not deployed. Both ranges are reserved permanently in the external holdout-use registry.
