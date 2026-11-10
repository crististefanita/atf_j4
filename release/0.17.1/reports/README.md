# Dovezile pentru Whist Lab 0.17.1

## Pe scurt

Calificarea urmărește centrarea licitării și intrarea în automatizarea mâinii pe APK-ul semnat.

## Execuții

| Selecție | Rezultat | Dovadă |
|---|---|---|
| `atf-validation` | 19 trecute | [Extent](component/atf-validation/component-report.html) · [Execuție](component/atf-validation/execution.json) |
| `atf-test-android` | 257 trecute | [Extent](component/atf-test-android/component-report.html) · [Execuție](component/atf-test-android/execution.json) |
| `atf-tests` | 333 trecute | [Extent](component/atf-tests/component-report.html) · [Execuție](component/atf-tests/execution.json) |
| `whist-lab-app-bidding` | 5 trecute | [Extent](component/whist-lab-app-bidding/component-report.html) · [Execuție](component/whist-lab-app-bidding/execution.json) |
| `whist-lab-app-commands` | 3 trecute | [Extent](component/whist-lab-app-commands/component-report.html) · [Execuție](component/whist-lab-app-commands/execution.json) |
| `whist-lab-app-display` | 16 trecute | [Extent](component/whist-lab-app-display/component-report.html) · [Execuție](component/whist-lab-app-display/execution.json) |
| `whist-lab-app-history` | 11 trecute | [Extent](component/whist-lab-app-history/component-report.html) · [Execuție](component/whist-lab-app-history/execution.json) |
| Auto-play pe Android | 1 scenariu trecut | [Extent](e2e/signed-release-auto-game/report.html) · [Execuție](e2e/signed-release-auto-game/execution.json) |

## Proveniență și scop

- Build în TMP cu `assembleDebug`, `assembleRelease` și `lintVitalRelease`. Ediția TEST păstrează certificatul ediției 0.17.0.
- Sanity pe Android API 28, cu pornirea automatizării din licitare, scorul mâinii și revenirea la control manual. [Restaurarea APK-ului și a datelor](e2e/signed-release-auto-game/restoration.json)
- [Intrările APK](apk-inputs.json) și [componentele](component/README.md) identifică același snapshot. [APK și CRC](diagnostics/apk-identity.json) · [Certificat](diagnostics/signing-compatibility.json)
- Guvernanța linkurilor relative a verificat referințele și căile în etapa de pregătire.

[Catalogul livrărilor](../../README.md) · [Strategia comună](../../../docs/testing/README.md)

## Distribuție

APK-ul complet și checksum-ul au fost descărcate prin GitHub autentificat. HTTP 200, dimensiunea, SHA-256 și CRC-ul tuturor intrărilor coincid cu binarul calificat. [Dovada descărcării](distribution-verification.json)

Adresa a fost extrasă din README-ul principal publicat pe GitHub.

[Extent pentru acceptanța descărcării](download-extent.html)
