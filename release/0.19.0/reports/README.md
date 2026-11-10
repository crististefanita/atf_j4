# Dovezile Whist Lab 0.19.0

## Pe scurt

Această pagină identifică rapoartele canonice Extent, receipt-urile și proveniența buildului.

## Suite de componente

Cele opt selecții primare au câte un raport HTML Extent și un receipt unic. [Indexul suitelor](component/README.md) leagă fiecare raport de execuția sa.

## Snapshot și proveniență

component/source-inputs.json, component/source-provenance.json și component/source-component-run.json confirmă că toate selecțiile au folosit aceeași sursă fixată. apk-inputs.json leagă buildul APK de commit și de hash-urile surselor.

## E2E

E2E nu a fost rulat la cererea acestei livrări. Lipsa raportului E2E este explicită și nu reprezintă un rezultat PASS.

## Diagnostic APK

diagnostics/ conține jurnalul buildului și verificările de identitate, aliniere, semnătură, compatibilitatea upgrade-ului și integritatea arhivei. Raportul cu hash-uri din release-validation.json acoperă exact fișierele livrate.

[Versiunea](../README.md) · [Catalogul livrărilor](../../README.md) · [Strategia comună de testare](../../../docs/testing/README.md)
