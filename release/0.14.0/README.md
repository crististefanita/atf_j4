# Whist Lab 0.14.0 — ediție Android

## Pe scurt

APK-ul 0.14.0 este disponibil public pe Android. Fișa păstrează identitatea, instalarea verificată, testele și proveniența descărcării.

## Identitate

- Pachet: `ro.whistlab.app`, `versionCode` 20, SDK minim 24, SDK țintă 36.
- APK: 1.224.345 octeți; SHA-256: `1ca2ff6f7e2fe1d665a414beee1b8e2d46331c7c039a37b6e7189b3c1a2a9cb5`.
- Semnare: certificat Android Debug, aceeași amprentă ca instalarea existentă; nu este o cheie administrată de producție.

## Descărcare

- [Descarcă APK 0.14.0](https://whist-lab-apk.totaloatcake.chatgpt.site/v0.14.0/WhistLab-0.14.0.apk) · [Checksum SHA-256](https://whist-lab-apk.totaloatcake.chatgpt.site/v0.14.0/WhistLab-0.14.0.apk.sha256).
- Pagina, APK-ul și checksum-ul au fost descărcate fără autentificare; manifestul păstrează octeții și hash-ul observat.

## Verificare

- Componente Gradle: 518 invocări; XML-ul JUnit original este păstrat în `reports/test-results/gradle/`.
- Componente ATF: 569 invocări în trei rapoarte Extent; capturile sunt inline Base64, fără fișiere imagine paralele.
- Smoke nativ pe emulator: 4 verificări JUnit, în `reports/test-results/native/`.
- Actualizarea pe emulatorul API 28 a păstrat `firstInstallTime`; APK-ul extras din dispozitiv are hash-ul candidatului, iar `MainActivity` a pornit.
- Calificarea ATF și rapoartele Extent sunt legate din [indexul rapoartelor](reports/README.md).

[Catalogul curent](../README.md) · [Strategia de testare](../../docs/testing/README.md)
