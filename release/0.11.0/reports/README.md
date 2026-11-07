# Auditul candidatului 0.11.0

## Domeniu și citire

Componentele Gradle publică inputuri efective, comparații evaluate și identitatea invocării în HTML. Unitățile, componentele și guvernanța păstrează categoria declarată. Turneele și corpusurile externe nu fac parte din această selecție.

| Selecție canonică | Invocări JUnit | Proveniență |
|---|---:|---|
| [app-commands](component/app-commands.html) | 105 | Nouă; GRADLE_COMPONENT_V1 |
| [app-bidding](component/app-bidding.html) | 248 | Nouă; GRADLE_COMPONENT_V1 |
| [app-history](component/app-history.html) | 61 | Nouă; GRADLE_COMPONENT_V1 |
| [app-display](component/app-display.html) | 83 | Nouă; GRADLE_COMPONENT_V1 |
| [core](component/core.html) | 190 | Nouă; GRADLE_COMPONENT_V1 |
| [independent](component/independent.html) | 10 | Nouă; GRADLE_COMPONENT_V1 |
| [raster](component/raster.html) | 9 | Nouă; GRADLE_COMPONENT_V1 |
| [shared-ui](component/shared-ui.html) | 14 | Nouă; GRADLE_COMPONENT_V1 |
| [audit-bridge](component/audit-bridge.html) | 12 | Nouă; GRADLE_COMPONENT_V1 |
| [governance](component/governance.html) | 210 | Nouă; JUPITER_COMPONENT_V1 |
| E2E focalizat Android | 3 | Execuție pe API 28 cu APK-ul exact verificat prin SHA-256 |

Total unic: **945**. Controalele negative interne, verificările de pixeli și perechile matricilor nu sunt invocări JUnit suplimentare.

## Matrice și imagini

Oracolele de scor, istoric, geometrie și raster evaluează toate combinațiile declarate. Raportul grupează comparațiile reușite pe invariant și reține valorile și poziția primei nepotriviri. [Rasterul JVM](component/raster.html) include PNG-uri sintetice inline; [marcajele Android](component/native-history-marks.html) verifică 54 de fixture-uri native separate de fluxurile de joc.

## Verificări Android

- [Licitație, alegere manuală, istoric și reluare](e2e/signed-release-bidding.html).
- [Profil și localizare](e2e/signed-release-home-profile.html).
- [Pornire și configurație](e2e/signed-release-smoke.html).

[Proveniența](diagnostics/native-provenance.json) leagă execuțiile noi de APK-ul instalat și confirmă restaurarea stării inițiale. Raportarea include capturile reale inline. Dovezile altui binar rămân în dosarul său. Selecția nu califică o partidă completă sau toate dispozitivele.

## Reproducere și retenție

[Manifestul surselor](apk-inputs.json) și [arhiva exactă](diagnostics/source-snapshot.zip) fixează snapshot-ul, inclusiv bibliotecile fizice. XML-ul componentelor a servit confruntării temporare JUnit–HTML; dosarul păstrează numai HTML-ul canonic al fiecărei selecții. JUnit-ul nativ păstrează identitatea și duratele originale.

Candidatul este local PREPARED; catalogul activ și publicarea remote rămân distincte.

[Reconcilierea surselor](diagnostics/source-scope-reconciliation.json) confirmă aceeași amprentă pentru buildul Android și selecția JVM a motorului. [Reîmprospătarea snapshot-ului](diagnostics/snapshot-refresh.json) identifică testele modificate pentru a agrega capturile exhaustive. Amprentele capturate în HTML-uri nu sunt rescrise.

## Revizia HTML finală

Cele 14 rapoarte canonice au fost inspectate în browserul MCP prin UI-debug read-only pe loopback. Directorul de revizie conține doar HTML-uri. [Recepția reviziei](diagnostics/browser-review.json) fixează hash-urile și controalele observate, inclusiv comparația licitației și vizualizatorul PNG. Autonomia statică este verificată separat; revizia nu a interceptat rețeaua.

[Reconcilierea surselor](diagnostics/source-reconciliation.json) confruntă inventarul complet cu snapshot-ul calificat, cu override-ul declarat de versiune. 

[Actualizarea paralelă a metadatelor Reporting](diagnostics/live-library-metadata.json) a fost verificată separat. JAR-ul fizic și sursele de execuție coincid; arhiva păstrează exact fișierele din rulare.

## Sursele livrării

Tagul indică [sursele calificate](https://github.com/crististefanita/atf_j4/tree/61a16105244e2504d7c0102956ef3e00019c0ca3).

[Legarea de Git](diagnostics/git-source-binding.json) verifică fișierele sursă și conversia CRLF în LF la indexare. Arhiva păstrează octeții folosiți la build.

[Catalogul livrărilor](../../README.md) · [Strategia comună](../../../docs/testing/README.md)
