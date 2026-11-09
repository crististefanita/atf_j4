# Whist Lab 0.15.0 — ediție Android

## Pe scurt

Versiunea adaugă redarea automată limitată la mâna curentă. După scor, jucătorul revine la control manual pentru mâna următoare.

## Identitate

- Pachet: `ro.whistlab.app`, `versionCode` 21, `minSdk` 24, `targetSdk` 36.
- APK: 1.226.245 octeți; SHA-256: `e8b38d53c4b9a83da3801e609ebe8ea1006e39e4531280b26f89da99a8dfffdf`.
- Certificat: Android Debug; canalul este `TEST`, fără cheie administrată pentru producție.
- Sursa aplicației: commit `3ddbe838ff9d10e2db445ca9a396033ccf3cc92b`.

## Descărcare

- [Descarcă APK 0.15.0](https://whist-lab-apk.totaloatcake.chatgpt.site/v0.15.0/WhistLab-0.15.0.apk) · [Checksum SHA-256](https://whist-lab-apk.totaloatcake.chatgpt.site/v0.15.0/WhistLab-0.15.0.apk.sha256).
- Pagina, APK-ul și checksum-ul au răspuns HTTP 200 la cereri HTTPS anonime fără cookie sau autorizare; dimensiunea și SHA-256 coincid cu manifestul.

## Calificare

- Componente: 1.099 invocări în șapte selecții; zero FAIL, ERROR sau SKIP. Fiecare selecție are un Extent HTML canonic.
- E2E Android: un flux pe API 28, în română, patru jucători, Whist românesc 1–8–1 și Hard. AutoGame termină mâna curentă și lasă următoarea mână la control manual.
- Raportul E2E păstrează șapte capturi inline Base64; XML și PNG intermediare nu sunt copiate separat.
- [Rapoarte și proveniență](reports/README.md) · [Manifestul de calificare](release-validation.json).

[Catalogul release-urilor](../README.md)
