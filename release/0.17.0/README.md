# Whist Lab 0.17.0 — ediție Android

## Pe scurt

Versiunea adaugă un control vizibil pentru Auto-play lângă licitare. Așezarea se adaptează spațiului disponibil, iar opțiunea revine la control manual pentru mâna următoare.

## Identitate și acces

- Pachet: ro.whistlab.app, versionCode 23, minSdk 24, targetSdk 36.
- Canal: Android TEST, certificat Android Debug.
- APK-ul este publicat în GitHub Releases; verificarea de distribuție s-a făcut prin contul GitHub autentificat.

## Calificare

- Cele șapte suite de componentă au trecut 1.119 teste; fiecare are un raport Extent canonic.
- E2E-ul Auto-play a trecut pe APK-ul candidat: 1 test, fără eșecuri, erori sau omisiuni, cu capturi inline.
- După test, APK-ul 0.15.0 și datele sale au fost restaurate și verificate.
- Descărcarea APK-ului publicat are receipt cu HTTP 200, hash și CRC verificate.

[Rapoarte și proveniență](reports/README.md) · [Manifestul versiunii](release-validation.json) · [Catalogul livrărilor](../README.md)