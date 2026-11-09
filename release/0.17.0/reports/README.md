# Rapoartele Whist Lab 0.17.0

## Pe scurt

Calificarea leagă șapte rapoarte Extent de componentă, un raport Extent E2E și identitatea APK-ului candidat de același commit și aceeași amprentă a sursei.

## Componente

Cele șapte selecții au trecut 1.119 teste: 19 pentru validare, 257 pentru infrastructura Android, 333 pentru guvernanța ATF și 510 în cele patru grupuri Gradle ale aplicației. Nu există eșecuri, erori sau teste sărite.

- [Rapoartele Extent și receipt-urile](component/README.md)
- [Manifestul APK și sursa](apk-inputs.json)
- [Manifestul calificării](../release-validation.json)

## E2E

Scenariul Signed release — Auto-play a instalat și identificat APK-ul 0.17.0, a jucat o mână românească 1–8–1 cu patru locuri la nivel Hard, a verificat controlul de Auto-play și revenirea la control manual pentru următoarea mână.

Rezultatul: **1 test trecut, 0 eșecuri, 0 erori, 0 omisiuni**; raportul Extent păstrează șapte capturi inline. [Raportul Extent](e2e/signed-release-auto-game/report.html) · [Receipt-ul de execuție](e2e/signed-release-auto-game/execution.json) · [Verificarea restaurării](e2e/signed-release-auto-game/restoration.json).

## Distribuție

Asset-ul a fost descărcat prin sesiune GitHub autentificată. HTTP 200, dimensiunea de 1.228.809 bytes, SHA-256 și CRC-ul tuturor celor 35 de intrări APK coincid cu binarul candidat. [Receipt-ul distribuției](distribution-verification.json) · [Asset-ul APK](https://github.com/crististefanita/atf_j4/releases/download/v0.17.0/WhistLab-0.17.0.apk).

[Catalogul livrărilor](../../README.md) · [Strategia comună de testare](../../../docs/testing/README.md) · [Ghidul de calificare](../../../docs/testing/ATF_ReleaseQualification.md)