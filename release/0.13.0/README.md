# Whist Lab 0.13.0 — dosarul release-ului

## Stare

APK-ul a fost construit și verificat, apoi publicat la un URL direct fără autentificare. Release-ul GitHub și tag-ul se publică după calificarea simulatorului iOS pe commitul candidatului.

- Pachet: `ro.whistlab.app`, `versionCode` 19, SDK 24–36.
- APK semnat și verificat cu aceeași identitate Android Debug ca distribuția anterioară; aceasta permite actualizarea instalării existente, dar cheia nu este o cheie de release administrată.
- SHA-256: `451d8c413988c9a533a8a39c1a644583723beb8351ee1ecf95410b2af759cfa0`.
- Teste: 502 componente Android, 44 teste filtrate pentru reguli și sesiune, 3 teste ale punții iOS; toate trecute. Fără E2E în această verificare.
- [Descarcă APK-ul fără autentificare](https://whist-lab-apk.totaloatcake.chatgpt.site/v0.13.0/WhistLab-0.13.0.apk) · [checksum SHA-256](https://whist-lab-apk.totaloatcake.chatgpt.site/v0.13.0/WhistLab-0.13.0.apk.sha256).
- [Rapoarte Extent componente](reports/README.md) · [Note de versiune](release-notes.md) · [Validare distribuție](release-validation.json)

Ținta SwiftUI iOS este inclusă în surse, dar rămâne candidat necalificat până la build și smoke test pe runner Apple. Nu există IPA în acest dosar.
