# Componente Maven — verificarea sursei

## Pe scurt

**Execuție nouă pe un snapshot de sursă, fără calificarea APK-ului din acest dosar.** Fiecare raport corespunde unei suite primare Maven; unitățile, Gradle și E2E au selecții separate.

## Rapoarte canonice

| Modul | Suită | Invocări JUnit | Raport | Metadate |
|---|---|---:|---|---|
| atf-validation | atf.validation.suites.ValidationComponentSuite | 19 | [Extent](atf-validation/component-report.html) | [Execuție](atf-validation/execution.json) |
| atf-test-android | atf.android.suites.AndroidComponentSuite | 246 | [Extent](atf-test-android/component-report.html) | [Execuție](atf-test-android/execution.json) |
| atf-tests | atf.tests.suites.AtfComponentSuite | 266 | [Extent](atf-tests/component-report.html) | [Execuție](atf-tests/execution.json) |

## Proveniență și citire

Modulele provin din execuții distincte pe același snapshot de sursă. Indexul declară originile; modulele neafectate rămân identice, iar o reverificare nu rescrie încercarea anterioară cu FAIL.

Sursa live a avansat după înghețare; [indexul](index.json) enumeră diferențele. Acest dosar califică numai snapshot-ul identificat, fără transferul verdictului către modificările ulterioare.

- **Comparații:** HTML-ul păstrează comparațiile evaluate, inputurile complete și receipt-ul fiecărei invocări. Adaptorul confruntă profilul capturat cu artefactul publicat, apoi lansatorul verifică egalitatea identităților și rezultatelor JUnit–HTML.
- **Proveniență:** [indexul](index.json) și [manifestul unic al surselor](source-inputs.json) identifică snapshot-ul și fișierele tehnice. Manifestul nu înlocuiește expected/actual; fragmentele inputului rămân integral în raport, cu preview-ul aserțiunilor separat.

## Limite

- Inventarul inițial omite 19 surse din pachetul legitim `release`. Indexul păstrează separat hash-urile observate în snapshot și comparația live; suplimentul nu schimbă fingerprint-ul capturat în HTML. [Corecția lansatorului și controalele necesare](../../../../../docs/feedback/ATF_Governance.md#acoperirea-inventarului-surselor) rămân deschise.
- Suitele focalizate nu sunt executate din nou peste populația primară.
- Nu s-a instalat ori executat Android; rezultatele nu se transferă APK-ului istoric.
- XML-urile și observațiile istorice unice rămân în dosarul lor, cu identitatea originală. Această execuție nu reconstruiește comparații care nu au fost capturate atunci.

[Indexul release-ului](../../README.md) · [Strategia comună](../../../../../docs/testing/README.md)
