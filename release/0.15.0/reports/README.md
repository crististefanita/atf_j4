# Dosarul de verificare Whist Lab 0.15.0

## Pe scurt

Dosarul reunește selecțiile de componentă, fluxul AutoGame pe APK-ul identificat, proveniența build-ului și dovada descărcării publice.

## Componente ATF

| Selecție | Invocări | Extent | Receipt |
|---|---:|---|---|
| atf-validation | 19 | [HTML](component/atf-validation/component-report.html) | [Execuție](component/atf-validation/execution.json) |
| atf-test-android | 257 | [HTML](component/atf-test-android/component-report.html) | [Execuție](component/atf-test-android/execution.json) |
| atf-tests | 316 | [HTML](component/atf-tests/component-report.html) | [Execuție](component/atf-tests/execution.json) |
| whist-lab-app-bidding | 248 | [HTML](component/whist-lab-app-bidding/component-report.html) | [Execuție](component/whist-lab-app-bidding/execution.json) |
| whist-lab-app-commands | 112 | [HTML](component/whist-lab-app-commands/component-report.html) | [Execuție](component/whist-lab-app-commands/execution.json) |
| whist-lab-app-display | 86 | [HTML](component/whist-lab-app-display/component-report.html) | [Execuție](component/whist-lab-app-display/execution.json) |
| whist-lab-app-history | 61 | [HTML](component/whist-lab-app-history/component-report.html) | [Execuție](component/whist-lab-app-history/execution.json) |

Total: 1.099 invocări de componentă; toate au PASS, fără FAIL, ERROR sau SKIP. Fiecare selecție are propriul Extent HTML și propriul receipt.

## Flux Android

[Extent E2E semnat — AutoGame limitat la mâna curentă](e2e/signed-release-auto-game/report.html) · [Receipt](e2e/signed-release-auto-game/execution.json)

APK-ul a fost instalat peste 0.14.0 pe Android API 28. Fluxul Romanian 1–8–1, cu patru jucători și Hard, verifică oprirea automată la ecranul scorului și revenirea la control manual pentru mâna următoare. Cele șapte capturi sunt inline Base64 în HTML; PNG-urile și XML-ul temporare nu sunt păstrate separat.

## Sursă, build și distribuție

- [Manifestul intrărilor APK](apk-inputs.json); [intrările componentelor](component/source-inputs.json), [rularea agregată](component/source-component-run.json) și [proveniența](component/source-provenance.json).
- [Identitatea APK](diagnostics/apk-identity.json); [compatibilitatea de instalare](diagnostics/signing-compatibility.json); [jurnalul Gradle](diagnostics/build.log).
- [Verificarea descărcării publice](distribution-verification.json) consemnează pagina, APK-ul și checksum-ul descărcate anonim.
- [Catalogul proiectului](../../README.md) · [Strategia comună de testare](../../../docs/testing/README.md).
