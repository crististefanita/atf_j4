# Rapoarte Whist Lab 0.14.0

## Pe scurt

Dosarul leagă APK-ul curent de raportarea componentelor, rezultatul instalării native și dovada descărcării publice. [Fișa versiunii](../README.md) identifică APK-ul și destinația lui directă.

## Suite Extent ATF

| Suită | Invocări | Raport HTML | Receipt |
|---|---:|---|---|
| Validare comună | 19 | [Extent](component/atf-validation.html) | [Execuție](component/atf-validation-execution.json) |
| Adaptor Android | 257 | [Extent](component/atf-test-android.html) | [Execuție](component/atf-test-android-execution.json) |
| Arhitectură și guvernanță | 293 | [Extent](component/atf-tests.html) | [Execuție](component/atf-tests-execution.json) |

Cele trei suite însumează 569 de invocări reușite. Capturile incluse în HTML sunt URI-uri Base64; dosarul nu păstrează copii PNG separate. [Inventarul surselor execuției](component/source-inputs.json) și receipt-urile leagă numărul de teste de rapoartele calculate.

## Componente Gradle

518 invocări Android și ale bibliotecii UI sunt păstrate ca rezultate JUnit originale, grupate în `test-results/gradle/`. Acestea sunt o colecție distinctă de suitele ATF, nu copii ale rapoartelor Extent de mai sus.

## Instalare nativă

- [Identitate și verificări pe emulator](diagnostics/native-identity.json)
- [Rezultat JUnit pentru actualizare și pornire](test-results/native/TEST-ro.whistlab.release.SignedUpgradeSmoke.xml) — 4 verificări.

## Distribuție și proveniență

- [Descărcare anonimă, octeți și SHA-256](distribution-verification.json)
- [Inventarul surselor APK](apk-inputs.json)
- [Manifestul de release](../release-validation.json)
- [Catalogul curent](../../README.md) · [Strategia comună de testare](../../../docs/testing/README.md)
