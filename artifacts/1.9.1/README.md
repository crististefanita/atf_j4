# Dovezile livrării 1.9.1

## Pe scurt

Patch de mentenanță față de 0.1.9, construit din commit-ul `5ae98e8cc1863c7513f0d7b8396087f19c0bc6cb`. Verificarea este țintită; dosarul păstrează numai rezultatele acestei livrări.

[Catalogul livrărilor](../README.md) · [Strategia comună de testare](../../docs/testing/README.md)

## APK

- **[Descarcă WhistLab-1.9.1.apk](https://github.com/crististefanita/atf_j4/releases/download/v1.9.1/WhistLab-1.9.1.apk)** · [Checksum](https://github.com/crististefanita/atf_j4/releases/download/v1.9.1/WhistLab-1.9.1.apk.sha256).
- `versionCode 11`, `minSdk 24`, `targetSdk 36`; 775,288 bytes.
- SHA-256: `ada43f8d5673e1330bf6555abde727d2c4705baf5e25748b032a46b9cf33b7ab`.
- Copia locală este în `dist`, exclusă din Git; semnarea păstrează cheia de upgrade a versiunii precedente.

## Verificarea patch-ului

| Control | Rezultat și dovadă |
|---|---|
| Componente UI | 21 teste trecute: chenare, geometria mesei, cărțile levatei și coloanele istoricului; XML în `test-results/app/`. |
| Controlul căutării AI | 4 teste trecute: buget, anulare, proprietarul scope-ului și conservarea deciziilor; XML în `test-results/core/`. |
| Guvernanță | 14 teste distincte trecute pe snapshot; [execuția inițială](logs/governance-snapshot-external-links-diagnostic.log) și [rerularea linkurilor](logs/release-governance.log), XML în `test-results/governance/`. |
| Construire | `assembleRelease` și `lintVitalRelease` trecute în TMP; [jurnal](logs/release-build.log). |
| Pachet | CRC, identitate, [semnătură](logs/apk-verification.log), [aliniere](logs/apk-alignment.log) și cheia de upgrade verificate. |

[Manifestul livrării](release-validation.json) păstrează selecția exactă, guvernanța și distribuția. [Proveniența surselor](source-provenance.json) identifică snapshot-ul construit și diferențele din copia de lucru paralelă.

## Distribuție

- [Descărcarea autentificată prin API](github-download-verification.json): mărimea, SHA-256 și CRC coincid cu APK-ul semnat.
- [Descărcarea autentificată în browserul MCP](github-authenticated-browser-download.json) a trecut: fișier complet, SHA-256 identic și CRC valid; release-ul este afișat ca **Latest**.
- [Notele publicate](release-notes.md) au fost reverificate separat prin cele cinci teste de prezentare.

## Limitele verificării

- [Diagnosticul draftului](logs/draft-url-governance-diagnostic.log) păstrează respingerea adreselor temporare `untagged` la prima verificare prematură a publicării; metadatele draftului și rezultatele publicării sunt separate.
- Nu s-a rulat regresia completă și nu s-a ocupat emulatorul rezervat de altă execuție; această livrare nu are E2E sau capturi native noi.
- [Rapoartele versiunii precedente](../0.1.9/README.md) rămân reper istoric, fără a califica implicit noul APK.
- Modificările paralele încă necomise rămân separate de commit-ul construit; hash-urile surselor și tag-ul permit reproducerea binarului.

[Procesul patch-ului](../../docs/platform/ATF_AndroidDelivery.md#release-de-mentenanță) definește verificările și documentele necesare.
