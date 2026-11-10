# Dovezile Whist Lab 0.19.1

## Pe scurt

Acestea sunt rapoartele canonice Extent, receipt-urile și proveniența buildului pentru ediția Android 0.19.1.

## Suite de componente

Cele opt selecții primare au fiecare un raport Extent HTML și un receipt de execuție. [Indexul suitelor](component/README.md) leagă raportul de selecția sa.

## Snapshot și proveniență

`component/source-inputs.json`, `component/source-provenance.json` și `component/source-component-run.json` fixează sursa comună pentru selecții. `apk-inputs.json` leagă APK-ul de commit și de hash-urile sursei.

## E2E

E2E nu a fost rulat. Schimbarea este exclusiv geometrică; această calificare acoperă componentele și nu declară E2E ca PASS.

## Diagnostic APK

`diagnostics/` păstrează jurnalul buildului și verificările de identitate, aliniere, semnătură, compatibilitate de upgrade și CRC. `release-validation.json` inventariază hash-urile payload-ului.

[Versiunea](../README.md) · [Catalogul livrărilor](../../README.md) · [Strategia comună de testare](../../../docs/testing/README.md)
