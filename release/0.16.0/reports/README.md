# Rapoartele Whist Lab 0.16.0

## Structura dovezilor

Release-ul separă calificarea componentelor de validarea fluxului real. Fiecare selecție are propriul raport HTML Extent și propriul receipt de execuție; raportul E2E este separat, fiindcă validează APK-ul instalat, nu doar sursa.

| Nivel | Conținut | Legătura de audit |
|---|---|---|
| Componentă | Șapte selecții Maven și Gradle; 1.099/1.099 teste promovate. Capturile vizuale rămân inline în Extent. | Receipt per selecție; manifest și proveniență comune; agregat cu identitățile JUnit și hash-urile rapoartelor. |
| E2E Android | AutoGame jucat pe APK-ul instalat într-un emulator headless API 28; șapte capturi PNG inline în Extent. | Receipt-ul identifică APK-ul, commitul și amprenta sursei, rezultatul și hash-ul raportului. |
| Integritate și distribuție | Identitatea APK-ului, compatibilitatea de upgrade, descărcarea anonimă și inventarul payload-ului. | Manifestul release-ului leagă fiecare receipt și raport prin SHA-256. |

## Rapoarte principale

- [Selecțiile de componentă](component/README.md), fiecare cu [Extent](component/atf-tests/component-report.html) canonic și receipt-uri per grup.
- [AutoGame pe Android](e2e/signed-release-auto-game/report.html) și [receipt-ul execuției](e2e/signed-release-auto-game/execution.json).
- [Descărcarea publică](distribution-verification.json), [identitatea APK](diagnostics/apk-identity.json) și [compatibilitatea semnării](diagnostics/signing-compatibility.json).
- [Manifestul intrărilor APK](apk-inputs.json) și [proveniența componentelor](component/source-provenance.json).

## Cum se verifică

1. Deschideți raportul Extent al grupului urmărit; capturile sunt încorporate în HTML, fără director separat de imagini.
2. Confruntați numărul și rezultatele cu receipt-ul selecției și cu agregatul componentelor. JUnit-ul temporar nu este păstrat ca raport duplicat.
3. Pentru fluxul instalat, comparați receipt-ul E2E cu APK-ul din manifest și verificați cele șapte imagini PNG inline.
4. Verificați inventarul și receipt-urile în [manifestul versiunii](../release-validation.json). Verdictul RAG read-only este păstrat în dosarul extern al candidatului, nu copiat ca rezultat de test în acest payload.

[Catalogul proiectului](../../README.md) · [Strategia comună](../../../docs/testing/README.md)
