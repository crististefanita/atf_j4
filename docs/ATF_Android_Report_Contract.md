# J4 — contractul raportului Android și al componentelor

## Pe scurt

Un singur HTML agregă execuțiile, iar fiecare invocare rămâne identificabilă.
Cititorul verifică inputul, expected/actual, verdictul și captura în raport,
fără să deschidă XML, JSON, properties ori alte artifacte externe.

Acesta este contractul țintă al consumerului; [feedback-ul activ](feedback/ATF_Android_Alignment_Feedback.md) delimitează migrarea încă necesară.

| Owner | Păstrează | Nu mutăm în acel owner |
|---|---|---|
| J4 | captură ADB, extragere stare, oracol independent, reguli, receptele și compoziția | implementarea HTML, CSS sau JS a Reporting |
| J0 Reporting | DTO-uri neutre, tabele, disclosure, timing capturat, PNG inline și regresii generice | parser Android, regulile jocului, interpretarea UI sau citirea automată a XML-ului |
| J1 | coordonarea cerințelor comune și handoff-ul | un al doilea owner al implementării J4 |

[Contractul generic](../../atf_j0/docs/ATF_Inline_Audit_Contract.md) descrie API-ul existent și profilul fără referințe.

## Reader without repository access

The HTML is the complete audit deliverable for a reader who has no J4 checkout, filesystem or source browser. Every claimed test explains its purpose, effective setup and inputs, evaluated preconditions, ordered steps, independent expected result, actual observation and final disposition. Missing capture detail is explicit and cannot be repaired by a descriptive title.

| Reading level | Content |
|---|---|
| primary | meaningful test/step names, selected rule variant, parameters and concise outcome/limitations |
| comparisons | standard ASSERTION / EXPECTED / ACTUAL / RESULT table, with the evaluated verdict and required bounded/full evidence |
| context | initial state, input or action, resulting state, units and inline rule/notation legend |
| technical provenance | original case identity and optional deduplicated checksums/build inventory; no checksum is presented as a business oracle |

Use native assertion steps for offline component comparisons; HTTP requests require actual HTTP evidence. SQL observations remain SQL facts and Android actions remain UI evidence; consistency means common tables/disclosures/status/timing, not fabricated transports. A summary that an independent replay returned PASS is clearly labeled as an evaluator summary and does not substitute detailed game assertions.

An external-reader acceptance opens the isolated HTML alone and reconstructs why at least one representative test passed or failed. It checks setup, actual input, independent rule, final comparison and partial-failure evidence without resolving a path or opaque rule code. Producer-side contract tests enforce this content; neutral renderer regressions stay in J0.

## Structura componentelor

| Nivel | Informație obligatorie | Proiecție |
|---|---|---|
| invocare | clasă, metodă, parametri/case ID și seed atunci când este relevant | scenariu propriu, inclusiv pentru nume identice |
| SETUP | profil, intrări și precondiții evaluate | observații neutre plus assertions de setup când există |
| TEST BODY | stare înainte, input, stare după, expected independent și rezultatul evaluat o dată | context semantic complet plus tabel standard de patru coloane, colapsabil |
| TEARDOWN | cleanup real și cauza secundară separată | assertions/diagnostic, păstrând cauza inițială |
| numărători | cazuri JUnit, jocuri model, mutări, assertions și branches | populații distincte; un exhaustiv într-o metodă nu devine mii de teste |

- PNG nu este obligatoriu la componentă și nu se fabrică pentru aspect de E2E.
- O intrare Surefire cu nume/status nu poate reconstrui comparațiile care nu au fost capturate.
- Traiectoria unui model se atașează invocării reale care a executat-o; nu se adaugă un test PASS fictiv de rezumat.
- Două scenarii globale cu sute de observations nu reprezintă automat sutele de invocări afișate în inventar.

## One artifact, original execution identities

One accepted run produces one autonomous HTML, including assets and all evidence required for its claims. Copying that HTML alone must preserve the audit; a ZIP, index of file links or raw XML dump does not meet this profile.

| Boundary | Required composition |
|---|---|
| invocation | one scenario for each real class/method/parameter identity; preserve PASS, FAIL, ERROR and SKIP distinctly |
| shared context | selection, input fingerprints and scoped metrics; never an invented PASS summary test |
| model trajectory | comparisons belong to the invocation that evaluated them; group by hand/trick without changing original order |
| optional evidence | require only receipts for the selected tests; a model-only selection must not require an unrelated AI benchmark |
| capture completeness | preserve the original outcome and state which comparison details were not captured; missing details cannot become invented assertions |
| execution provenance | fresh components and archived E2E remain separate runs even when reviewed together |

The target receipt records run ID, invocation ID/parameters, input fingerprints, evaluated comparison name/expected/actual/status, failure cause and ordered checkpoint ownership. Capture method/phase intervals at execution and identify the population of every aggregate metric. Persist the partial receipt on failure; the publisher projects it without rerunning the business oracle.

Legacy JUnit outcomes alone cannot supply this receipt. A frozen producer snapshot can attest specific persisted comparisons for a diagnostic review, but that narrow proof is not a general receipt decoder or production acceptance.

## Structura E2E cu poze

| Pas | Dovezi în raport | Verificare |
|---|---|---|
| GIVEN | aplicație/build, precondiția și starea inițială | sursa observată este explicită |
| WHEN | acțiune, target semantic și interval măsurat | acțiunea nu este confundată cu observația |
| THEN | expected/actual, verdictul evaluării și PNG original | cadrul aparține aceleiași invocări/faze/checkpoint |
| failure | ultima stare sigură, captura disponibilă, cauza și trace-ul parțial | fără conversie în PASS sau înlocuirea cauzei prin eșecul publisherului |

Caption-ul este semantic, de exemplu starea după reluare, nu numele unui fișier.
Dacă mai multe cărți se joacă între două cadre, raportul declară fereastra
observată; nu inventează o imagine pentru fiecare carte.

### Proiecția UI pe checkpoint, la fel ca Playwright

Fiecare checkpoint Android capturat se publică prin `UiExtentReportSession.UiPhase.step`,
în faza și ordinea în care a rulat. Același nod Gherkin expandabil deține tabelul
standard ACTION/TARGET/EXPECTED/OBSERVED/TIME/RESULT, PNG-ul inline și tabelul
ASSERTION/EXPECTED/ACTUAL/RESULT. Nu mutați imaginile într-o galerie sau assertions
într-o fază agregată: cititorul trebuie să poată audita o stare fără să ghicească
care captură și care verdict îi aparțin.

- Înregistrați interacțiunile Android efectiv expediate (launch, tap, select, swipe,
  back) ca `ACTION`, cu timpul capturat al dispatch-ului și rezultatul comenzii ADB.
  Rezultatul ulterior al UI rămâne în assertions; acceptarea comenzii nu certifică
  singură schimbarea de stare.
- Păstrați observația UI de după acțiuni separat de ACTION, dar în același checkpoint.
  O verificare fără input poate fi `OBSERVE`; nu inventați un click doar ca să existe
  un rând în tabelul de acțiuni.
- Atașați PNG-ul full-display checkpoint-ului care l-a produs. Captura și assertions
  folosesc statusul local al checkpoint-ului; statusul journey-ului nu recolorează
  retrospectiv toate cadrele anterioare.
- PNG-urile identice au un payload canonic și câte un link la fiecare checkpoint.
  Auditul rezolvă `data-atf-screenshot-ref` către imaginea exactă; numără separat
  bytes-urile unice și linkurile, fără să ceară duplicarea Base64.
- Valorile lungi din ACTION/OBSERVE au disclosure imbricat. Auditul parcurge DOM-ul
  întregii celule, inclusiv ultima valoare, și compară forma redactată/normalizată
  prin API-ul public. Un prim `</details>` nu delimitează tabelul exterior.
- FAIL-ul unui checkpoint, al unei acțiuni ori al unei comparații și diagnosticul
  inițial supraviețuiesc unui apel ulterior `finish(PASS)`. Rezultatul funcțional
  și rezultatul publicării rămân distincte; eșecul auditului nu devine PASS global.
- XML/properties rămân probe locale de suport. Receipt-ul public poate conține
  fingerprint-uri SHA-256 colapsate, dar nu linkuri `file:` și nici dump-uri brute;
  checksum-ul este proveniență tehnică, nu oracol de business.
- Un failure fără checkpoint publicat primește diagnostic explicit FAIL. Nu derivați
  un PASS din raportul generat, dintr-o imagine disponibilă sau din statusul JUnit.

Contractul se verifică atât ca componentă a publisherului pe receipt sintetic, cât și
într-o rulare Android: ordinea fază/pas, tabelele expandabile, captura inline proprie
pasului, assertions originale, acțiunile fără duplicare și diagnostic FAIL parțial.

Implementarea live J4 este `AndroidUiFlowReportPublisher` din `atf-test-android`;
testele sale și `AndroidUiDriverActionTraceTest` rămân în `src/test`. Proba API 28
din 3 noiembrie 2026 a publicat șase checkpoint-uri pentru masa cu opt cărți și
două pentru mâna cu treisprezece cărți. Auditul verifică receipt-ul, identitatea
și numărătorile, fiecare titlu/acțiune/assert, bytes PNG inline și absența
linkurilor locale. Timpul Tap/Back/Swipe măsoară numai dispatch-ul ADB, separat
de pauza folosită pentru stabilizarea UI. Rapoartele și capturile rămân în folderul
configurat prin `atf.android.artifacts` (implicit `%TEMP%`), nu în sursele J4.

## Full-display Android screenshots

Capture the entire physical Android display at each reported checkpoint, including every edge, without cropping to the app, a card or a region. Preserve the original PNG bytes and orientation through publication; analysis regions may be derived separately and must not replace the audit image.

| Owner | Acceptance criterion |
|---|---|
| J4 capture | decode PNG, verify physical dimensions/orientation and byte identity from capture to the correct invocation/checkpoint |
| J0 viewer | use the available browser viewport for the overlay; fit the whole image with its aspect ratio and no clipping, distortion or internal scroll for the initial view |
| inspection | explicit original-size/zoom mode may scroll; returning to fit or closing retains the current owner, disclosures, scroll and keyboard focus |
| portrait in landscape | side margins are legitimate; filling the viewport must not remove the top, bottom or edges of the Android screen |

Current ADB screencap captures the full display; the generic viewer and return-context fixes are verified in [J16](ATF_Reporting_J16_Verification.md). J4 must regenerate its reports with that binary and independently verify physical capture dimensions and checkpoint ownership. Generic PNG fixtures do not certify a new Android execution.

## Agregare lizibilă, fără file browser

- Parserul J4 transformă XML/JUnit/JaCoCo/properties în modele semantice înaintea publicării.
- HTML-ul public primește artifacts gol. XML-urile, logurile și receptele pot rămâne arhivă de execuție, fără linkuri ori căi folosite ca substitut al dovezii.
- Fields conțin identificatori și rezumate scurte. Bodies JSON/TEXT păstrează contextul complet necesar auditului, în secțiuni denumite semantic.
- Nu se înlocuiește un link XML cu un dump XML gigantic. Coverage devine contoare/scope; receptele devin intrări și rezultate structurate.
- Expected/actual și verdictul nu sunt ascunse numai într-un text brut; comparațiile apar în grupul standard Assertions.
- Preview-ul rămâne 240. Bugetul fiecărui corp trebuie să permită dovezile obligatorii, iar un conținut omis nu este declarat COMPLETE.

## Adaptive detail and evidence budgets

The [implemented presentation contract](ATF_Game_Report_Presentation.md) captures one immutable policy from config/game-evidence.yml and generic Reporting settings for all six publishers. AUTO and SUMMARY select whole learning invocations with an explicit omitted population; the five legacy publishers retain their supplied scope. FULL retains every supplied invocation and requested optional detail within positive budgets, or refuses complete-audit acceptance.

| Boundary | Invariant |
|---|---|
| generic settings | assertion preview stays 240; body budget is positive and never overridden with an unlimited constant |
| native grouping | bounded assertion rows and observation fields, complete ordered TEXT/JSON parts, native disclosures |
| mandatory audit | all supplied comparisons, compared inputs, original verdict, failure cause and PNGs remain available in the declared scope |
| optional context | cumulative item/character budget, including upstream fingerprints; original/retained/omitted counts and reasons |
| completeness | INCOMPLETE publication refusal survives close; the captured business outcome is a separate fact |
| responsibility | missing legacy receipts, independent business oracles and measured Android timing remain J4 capture requirements |

[Permanent contracts and rendering evidence](ATF_Game_Report_Verification.md) cover the physical C3/J16 pair, individual/combined publication and deliberate negative controls. No game-specific keys, HTML rewriting or private renderer are introduced into J0.

## Timp, status și acceptor

| Criteriu | Regula |
|---|---|
| execuție | măsurată la callback/journey, transmisă prin executionTiming |
| publicare | măsurată separat; construirea HTML-ului nu este durata jocului |
| observație | INFO fără comparații inventate |
| comparație | verdictul calculat înainte de Reporting, inclusiv FAIL după preview |
| acceptor | verifică exact ownerul, ordinea, multiplicitatea, ultima comparație și bytes PNG |
| intrare absentă | date obligatorii lipsă, zero cazuri ori all-SKIP nu închid acceptanța ca PASS |

Căutarea globală html.contains sau un set global de hash-uri nu refuză
întotdeauna mutarea unei dovezi la alt scenariu. Un PNG integru cu meniul și
Resume Game disabled infirmă propoziția de reluare, chiar dacă pragul raster trece.

## Ordinea livrării

1. Refuzați false PASS-ul resume în componente offline, cu meniul/disabled/stale/contended ca negative.
2. Capturați modele de evidence per invocare și publicați assertions/timing fără reconstruire din statusul JUnit.
3. Înlocuiți Artifact(uri) și observeFile brut cu proiecția semantică inline.
4. Refolosiți o compoziție J4 mică pentru individual/suită; suportul reutilizabil se află în main Java, iar testele rămân teste.
5. Verificați nominal/FAIL/SKIP/partial, sursa datelor și randarea în TAB MCP, apoi device numai sub lease exclusiv.

Regresia J0 este neutră și nu certifică Android. Rularea AI înghețată C3/J13
rămâne istorică; adoptarea unui JAR ulterior are dovadă distinctă.

[Criteriile active](feedback/ATF_Android_Alignment_Feedback.md) păstrează constatările și probele concrete.
