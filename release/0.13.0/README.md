# Whist Lab 0.13.0 — dosarul release-ului

## Stare

APK-ul Android a fost construit și verificat, apoi publicat cu tag-ul GitHub `v0.13.0` și pe un URL Site direct fără autentificare. Descărcarea anonimă a asset-ului GitHub a răspuns 404; folosiți linkul Site de mai jos. Ținta iOS este prezentă în cod, dar nu este calificată pe simulator.

- Pachet: `ro.whistlab.app`, `versionCode` 19, SDK 24–36.
- APK semnat și verificat cu aceeași identitate Android Debug ca distribuția anterioară; aceasta permite actualizarea instalării existente, dar cheia nu este o cheie de release administrată.
- SHA-256: `451d8c413988c9a533a8a39c1a644583723beb8351ee1ecf95410b2af759cfa0`.
- Teste: 502 componente Android, 44 teste filtrate pentru reguli și sesiune, 3 teste ale punții iOS; toate trecute. Fără E2E în această verificare.
- [Descarcă APK-ul fără autentificare](https://whist-lab-apk.totaloatcake.chatgpt.site/v0.13.0/WhistLab-0.13.0.apk) · [checksum SHA-256](https://whist-lab-apk.totaloatcake.chatgpt.site/v0.13.0/WhistLab-0.13.0.apk.sha256).
- [Release GitHub și tag-ul sursă](https://github.com/crististefanita/atf_j4/releases/tag/v0.13.0) · commit: `3146464afe6bd61ac66b5cae5c975f2451f79084`.
- [Rapoarte Extent componente](reports/README.md) · [Note de versiune](release-notes.md) · [Validare distribuție](release-validation.json)

Ținta SwiftUI iOS este inclusă în surse, dar rămâne candidat necalificat până la build și smoke test pe runner Apple. GitHub a refuzat pornirea workflow-ului cu HTTP 422 (`Actions has been disabled for this user`), deși setarea repository-ului este activă; fără o execuție nu pretindem compatibilitate verificată. Nu există IPA în acest dosar.
