> Current delivery: only the independently specified late-card correction is in production. Statistical bid-evidence weights were rejected and removed from production. UI is outside this iteration.

# Round 6 — lost-game regression analysis (offline only)

## Română

Analiza pornește de la partidele V4 pierdute din corpusul de development, nu de la interfață. Au fost inspectate 95 de partide și 1.520 de poziții în care jucătorul mai avea două cărți. Certificatorul exhaustiv, folosit numai în teste, a găsit patru alegeri care nu garantează contractul deși există o alternativă garantată. Mâinile observate în acele patru partide au fost exacte; testele nu pretind că acele alegeri au pierdut mâna înregistrată. Ele construiesc o continuare adversă compatibilă și verifică separat toate distribuirile posibile.

Două ratări observate au fost reduse și la continuările înregistrate ale ultimelor două levate, la mâini de 5 și 3 cărți. Cu contract zero deja îndeplinit, alegerea veche păstra valetul de pică, respectiv dama de cupă, și câștiga ultima levată nedorită. Aruncarea cărții mari păstrează 7-ul, iar pe aceeași continuare legală scorul de bază se schimbă din −1 în +5. Acest exemplu nu presupune că adversarii vor repeta orice acțiune după un input diferit; turneele noi îi lasă să răspundă prin algoritmul lor normal.

Corecția structurală folosește numai informație publică. Cu două cărți din aceeași culoare și rezultatul levatei curente fixat, păstrează cartea mare numai dacă mai trebuie câștigată o levată ulterioară; altfel păstrează cartea mică. Rezultatul curent este fixat dacă ambele cărți pierd deja sau, la ultimul jucător, dacă ambele câștigă. Nu se aplică pe prima carte a levatei, cu culori diferite, cu o singură carte ori când una câștigă și alta pierde. Obligația de culoare și de atu rămâne prioritară. Aceasta este o decizie strategică, nu o regulă nouă a jocului.

Implementarea Expertului folosește Card și GameRules; jucătorul independent folosește propriile numere de cărți și propriul evaluator. Oracolul din teste enumeră liste de cărți și continuări, fără a apela GameRules sau corecția verificată. Cele patru fixture-uri au expected literal și 336, 300, 200, respectiv 9 distribuiri compatibile. Regresia reproduce și alegerea V4 a rolului original înainte de a verifica noua alegere. Alte 14 exemple verifică toate gardurile și alegerea rangului, iar două verifică explicit levatele și scorul de bază.

Experimentele cu licitațiile sunt separate. belief02 a aplicat o pondere slabă la alegerea contractului și a fost respins. cardbelief03 păstrează licitarea V4 și folosește informația licitațiilor deja declarate numai la evaluarea cărților. Ponderea fiecărui adversar este 0,25 + 0,75 / (1 + eroare²) față de un model înghețat, antrenat anterior numai pe development. Nu este o probabilitate calibrată și nu dezvăluie cărți reale. Toate distribuirile fezabile au pondere pozitivă. Mâna inițială ipotetică este reconstruită adăugând cărțile deja jucate public, fără dublarea levatei curente. Fără licitații observate ponderea este neutră; profilul de o carte rămâne neschimbat.

În development au fost folosite șase seed-uri noi, 6400–6405, toate cele patru locuri, doi adversari V4 ficși și ambele roluri: 96 perechi per candidat. late01 nu a schimbat scorul sau victoriile; belief02 a regresat; cardbelief03 a câștigat +66/+20 de puncte, dar a pierdut 1/2 unități de victorie. Gate-ul de development rămâne REGRESSION. cardbelief03 a fost ales pentru evaluare independentă și înghețat înainte de lotul 81000–81015; nu a fost promovat pe baza punctelor medii. Registrul extern leagă lotul de candidatul și hash-urile sale, împiedicând reutilizarea ca heldout nou pentru alt candidat.

Lotul final are 256 perechi, 512 partide complete: 16 seed-uri × 4 locuri × 2 familii × 2 roluri. Gate-ul de promovare nu se schimbă după rezultat: nicio pierdere de win share pe familie, scădere medie de scor cel mult două puncte/partidă, plus progres agregat strict și contribuție pozitivă a mâinilor 2–8 pentru fiecare rol. Rezultatele finale sunt în receptele și raportul rulării, fără a deduce tăria globală dintr-un eșantion finit.

## English

The analysis uses retained lost V4 development games, with no UI. It inspected 95 games and 1,520 two-card decision positions. Four risky choices admit a contract failure in a compatible hidden continuation despite an available guaranteed alternative. The recorded hands themselves were exact; these fixtures do not falsely attribute the observed loss to those choices. Their independent list-based oracle checks all feasible worlds and legal remaining opponent actions.

Two additional observed misses isolate the last two tricks of five- and three-card hands. After an already fulfilled zero bid, retaining the spade jack or heart queen won an unwanted final trick. Discarding the high card retains the seven; the same legal recorded continuation changes base points from −1 to +5. Live algorithmic tournaments, rather than a fixed replay, establish what happens when opponents respond to changed actions.

The structural fix applies only to two same-suit cards with the current trick outcome fixed: both already lose, or both win at the last seat. Retain high only when another future trick is needed; otherwise retain low. Leading, mixed suits, one-card holdings and split winner/loser alternatives retain ordinary planning. Mandatory suit/trump rules remain unchanged. The runtime uses this small rank rule; exhaustive proof stays test-only.

The observed-bid evidence experiment is explicitly separate from the structural fix. The previously rejected future-bid predictor is not used to invent future bids. cardbelief03 leaves bidding unchanged and softly weights card-play worlds using already declared bids. Its positive heuristic weight is not a calibrated posterior and never removes a feasible world. Initial hypothetical hands are reconstructed from public plays with current-trick deduplication. Four component cases cover evidence, neutrality, reconstruction and calibration-domain guards.

Development and untouched evaluation have distinct receipts. Development exposed lower win share despite higher mean points; this remains visible. Nine policy source files were frozen before evaluating seeds 81000–81015. The same conservative per-family progression criterion is applied independently, with no retuning after evaluation. Two workers use separate build/output folders and disjoint role/game IDs. Final game rules, every played card and every score are checked by an integer oracle independent of either player.

The J16 report includes every new captured regression, representative complete paired games, every non-PASS invocation and the full-population quality gate. It declares all omitted IDs. Other native JUnit cases are not presented as individually audited comparisons. Summed captured intervals are labelled separately from wall time; publication is separate. No app, emulator, APK install or UI test is part of this iteration.

Run evidence: C:/work/whist_back/ai-training/20261102213353-round6.

## Final measured results / Rezultate finale

| Check | Result | Interpretation |
|---|---|---|
| New local regressions | 20 PASS | Four certified risky decisions, fourteen rank/domain boundaries, two observed misses |
| Full offline JVM regression | 395 PASS, 0 FAIL/ERROR/SKIP | Includes the 20 new cases; other native cases are not individually reconstructed in HTML |
| Statistical experiment, seeds 81000–81015 | Expert −201 points / −8.5 win share; independent −264 / −0.5 | Rejected, not deployed |
| Structural fix alone, seeds 92000–92007 | 128 pairs / 256 full games; points and win share unchanged in every family | Local correctness accepted; no empirical superiority claim |
| Strict progression gate for local fix | REGRESSION / no strict gain | Preserved as FAIL in Extent; never rewritten into a stronger-play PASS |
| Latest J16 consumer contracts | 47 PASS | Reconciled with live row/field grouping, body and optional-context budgets |
| Corpus integrity | 4,529 games / 155 distinct deals / 1,702,904 played cards, PASS | Includes every new loss and rejected experiment |

The statistical experiment and local fix have separate frozen plans and separate reserved evaluation ranges. The local algorithm is byte-identical to late01, fixed before either evaluation range was opened. The local correctness plan was declared before seeds 92000–92007; its acceptance does not relax or relabel the original strict strength gate.

The final J16 artifact contains 26 captured invocations and 2,219 original comparisons: all 20 new regression cases, four representative complete A/B pairs, and two distinct quality gates. It declares 150 supplied captures and 124 omitted game invocations. Native regression totals are explicitly aggregate-only. The HTML is 5,238,239 bytes; its source verdict is FAIL because strict statistical improvement was not demonstrated, while local correctness acceptance is PASS.

Reports: C:/work/whist_back/ai-training/20261102213353-round6/final-composition/algorithm-regressions-and-new-games-j16.html and cardbelief03/rejected-card-bid-model-j16.html. Both use physical Reporting J16 SHA-256 02b798a5848d5167feca16a0977f4f30b7047bb49ea666200aa7d0ea11731367. Static owner/value/verdict/timing/scope audits pass; no interactive browser or Android acceptance is claimed.

Research-only BidEvidence classes and their four tests remain on the test classpath, excluded from default regression and quick collections. They are runnable explicitly through bidEvidenceResearchTest. Exhaustive mining is likewise opt-in; the runtime correction does not enumerate worlds.

Learning collection now requires an explicit V6 protocol JSON. The runner forwards its seed range, checks frozen source digests before reserving evaluation, and refuses a range owned by another candidate. This prevents a previously consumed test set from being described as new.

Quick collection verified through the live isolated runner: 125 tests PASS, 109227 ms for the measured Gradle interval. The complete runtime regression remains 395 cases.
