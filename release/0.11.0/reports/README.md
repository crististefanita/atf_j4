# Auditul livrării 0.11.0

## Pe scurt

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

Proveniența leagă execuțiile de APK-ul instalat și confirmă restaurarea stării inițiale. Raportarea include capturile reale inline. [Proveniența Android](diagnostics/native-provenance.json)

Dovezile sunt organizate pe binar. Selecția acoperă fluxurile Android enumerate mai sus.

## Reproducere și retenție

- Manifestul și arhiva fixează snapshot-ul, inclusiv bibliotecile fizice. [Manifestul surselor](apk-inputs.json) · [Arhiva exactă](diagnostics/source-snapshot.zip)
- XML-ul componentelor a servit confruntării temporare JUnit–HTML. Dosarul păstrează HTML-ul canonic al fiecărei selecții, iar JUnit-ul nativ păstrează identitatea și duratele originale.
- Rapoartele califică APK-ul publicat cu tagul `v0.11.0`. Verificarea distribuției separă descărcarea prin API, proba în browserul autentificat și răspunsurile anonime. [Manifestul livrării](../release-validation.json) · [Verificarea distribuției](distribution-verification.json)
- Buildul Android și selecția JVM a motorului au aceeași amprentă. Snapshot-ul indică testele modificate pentru agregarea capturilor, iar fișierele HTML păstrează amprentele înregistrate. [Reconcilierea surselor](diagnostics/source-scope-reconciliation.json) · [Reîmprospătarea snapshot-ului](diagnostics/snapshot-refresh.json)

## Revizia HTML finală

Cele 14 rapoarte canonice au fost inspectate în browserul MCP prin UI-debug read-only pe loopback. Recepția fixează hash-urile și controalele observate, inclusiv comparația licitației și vizualizatorul PNG. [Recepția reviziei](diagnostics/browser-review.json)

- Directorul de revizie conține HTML-urile. Autonomia statică a fost verificată separat de revizia în browser.
- Inventarul complet corespunde snapshot-ului calificat cu override-ul declarat de versiune. [Reconcilierea surselor](diagnostics/source-reconciliation.json)
- JAR-ul fizic și sursele de execuție coincid, iar arhiva păstrează exact fișierele din rulare. [Verificarea metadatelor Reporting](diagnostics/live-library-metadata.json)

## Sursele livrării

Tagul indică sursele calificate din commitul `61a16105244e2504d7c0102956ef3e00019c0ca3`.

[Legarea de Git](diagnostics/git-source-binding.json) verifică fișierele sursă și conversia CRLF în LF la indexare. Arhiva păstrează octeții folosiți la build.

[Catalogul livrărilor](../../README.md) · [Strategia comună](../../../docs/testing/README.md)
