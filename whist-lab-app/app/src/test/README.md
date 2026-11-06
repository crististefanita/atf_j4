# Componentele JVM ale aplicației Android

## Pe scurt

Aceste teste verifică politicile și modelele aplicației fără instalare sau emulator. [Strategia comună](../../../../docs/testing/README.md) deține acceptarea rezultatului; [arhitectura aplicației](../main/java/ro/whistlab/app/README.md) delimitează integrarea Android rămasă.

## Familii de verificare

| Familie | Intrare controlată și oracol |
|---|---|
| Layout și text | Dimensiuni, densitate, font și număr de locuri; limite și măsurători independente. |
| Persistență și istoric | Comenzi legale, checkpoint JSON, solduri și migrare; refuzul datelor corupte. |
| Ciclu de viață | Ceas, executor, navigare și callback-uri controlate; anulare și reluare fără efect repetat. |
| Colectarea levatei | Geometrie, jucători, ultima levată și evenimente întârziate; starea partidei se păstrează. |

## Execuție și limite

- **Execuție:** dintr-o copie temporară a aplicației, [configurația Gradle](../../build.gradle) selectează componentele prin `whist.displayComponents=true`. SDK-ul este necesar compilării; testele rulează pe JVM, fără comenzi ADB.

- **Raster:** [colecția dedicată](../rasterTest/README.md) folosește JDK-ul grafic complet și o sarcină distinctă. Imaginile sale sunt probe de componentă, separate de fontul, pixelii și interacțiunea Android reale.

- **Dovezi:** artefactele și rezultatele aparțin execuției externe. [Calificarea unui release](../../../../docs/testing/ATF_ReleaseQualification.md) decide selecția nativă necesară după verificările locale.

[Aplicația](../main/java/ro/whistlab/app/README.md) · [Strategia comună](../../../../docs/testing/README.md) · [Componente display](../../../../docs/testing/ATF_DisplayComponents.md)
