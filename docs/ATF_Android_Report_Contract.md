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

## Full-display Android screenshots

Capture the entire physical Android display at each reported checkpoint, including every edge, without cropping to the app, a card or a region. Preserve the original PNG bytes and orientation through publication; analysis regions may be derived separately and must not replace the audit image.

| Owner | Acceptance criterion |
|---|---|
| J4 capture | decode PNG, verify physical dimensions/orientation and byte identity from capture to the correct invocation/checkpoint |
| J0 viewer | use the available browser viewport for the overlay; fit the whole image with its aspect ratio and no clipping, distortion or internal scroll for the initial view |
| inspection | explicit original-size/zoom mode may scroll; returning to fit or closing retains the current owner, disclosures, scroll and keyboard focus |
| portrait in landscape | side margins are legitimate; filling the viewport must not remove the top, bottom or edges of the Android screen |

Current ADB screencap captures the full display; this does not prove the current viewer meets the viewport requirement. [Generic viewer criterion 138](../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-screenshot-viewport-138--imagine-integrală-în-spațiul-disponibil) remains open alongside the independent return-context defect 137.

## Agregare lizibilă, fără file browser

- Parserul J4 transformă XML/JUnit/JaCoCo/properties în modele semantice înaintea publicării.
- HTML-ul public primește artifacts gol. XML-urile, logurile și receptele pot rămâne arhivă de execuție, fără linkuri ori căi folosite ca substitut al dovezii.
- Fields conțin identificatori și rezumate scurte. Bodies JSON/TEXT păstrează contextul complet necesar auditului, în secțiuni denumite semantic.
- Nu se înlocuiește un link XML cu un dump XML gigantic. Coverage devine contoare/scope; receptele devin intrări și rezultate structurate.
- Expected/actual și verdictul nu sunt ascunse numai într-un text brut; comparațiile apar în grupul standard Assertions.
- Preview-ul rămâne 240. Bugetul fiecărui corp trebuie să permită dovezile obligatorii, iar un conținut omis nu este declarat COMPLETE.

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
