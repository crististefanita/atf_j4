# Dovezile livrării 1.9.2

## Pe scurt

Patch față de 1.9.1, construit în TMP din commit-ul `9623d95b285291b0675a48e54610b56fcbe693c9`. Dosarul păstrează selecția acestei livrări, fără repetarea regresiei complete.

[Catalogul livrărilor](../README.md) · [Strategia comună de testare](../../docs/testing/README.md)

## APK

- **[Descarcă WhistLab-1.9.2.apk](https://github.com/crististefanita/atf_j4/releases/download/v1.9.2/WhistLab-1.9.2.apk)** · [Checksum](https://github.com/crististefanita/atf_j4/releases/download/v1.9.2/WhistLab-1.9.2.apk.sha256).
- `versionCode 12`, `minSdk 24`, `targetSdk 36`; 775,288 bytes.
- SHA-256: `6a4a50d895256c94254380083dffbb2fa3f2415ba92ba5f2133d8daf8926e81e`.
- Semnătura păstrează cheia de upgrade; copia locală curentă și checksum-ul sunt în `dist`, excluse din Git.

## Verificări

| Arie | Rezultat și dovadă |
|---|---|
| Geometrie și ordine | 30 teste trecute în aplicație: proporție 5:7, mână stabilă de 1–13 cărți, levată independentă de 3–6 jucători, arc superior și tură reală în toate variantele; XML în `test-results/app/`. |
| Oracole ATF | 77 teste trecute: înălțime și lățime exacte, geometrii deformate respinse, viewport comun și audit fără transport; XML în `test-results/atf/`. |
| Guvernanță | 17 teste trecute: API-uri Java, dosare, note, linkuri, stil și proză; [jurnal](logs/release-governance.log). |
| Build | `assembleRelease`, `lintVitalRelease` și selecția componentelor; [jurnal](logs/release-192-build.log). |
| Pachet | CRC, SDK-uri, [semnătură](logs/apk-verification.log), [aliniere](logs/apk-alignment.log) și cheie de upgrade verificate. |

[Manifestul](release-validation.json) identifică rezultatele, guvernanța și distribuția. [Proveniența](source-provenance.json) confruntă fișierele Android cu sursa commit-ului construit și declară diferențele paralele excluse.

## Raport nativ

- **[Extent: sanity compact API 28](reports/compact-api28/responsive-ui-sanity.html)** — un scenariu, cu checkpoint-uri pentru 13 cărți, patru cărți jucate și licitare; fără partidă completă.
- [Auditul offline al PNG-ului](reports/compact-api28/offline-hand-index-audit.json) verifică toți cei 13 indici: rang și culoare prezente, fără cerneală tăiată la margine. [Evaluatorul](replay/WhistOfflineHandIndices.java) folosește `HandIndexAudit` pe XML/PNG existente, fără ADB.
- [Identitatea APK-ului E2E](reports/compact-api28/apk-provenance-e2e-debug.json): varianta Debug permite fixture-urile deterministe; randarea folosește aceleași surse ale livrării. APK-ul Release semnat are verificare separată de instalare și pornire.
- [Smoke-ul APK-ului Release](reports/compact-api28/release-smoke.json) confruntă lansarea și hash-ul instalat cu binarul semnat. [Restaurarea](reports/compact-api28/app-data-restored.json) păstrează datele anterioare; arhiva privată nu este publicată.
- [Guvernanța API-urilor Java](logs/java-api-governance.log): trei teste trecute, inclusiv apeluri interzise și colectori permiși.

## Diagnostic și limite

- [Candidatul cu banda rotunjită de două ori](diagnostics/rejected-pixel-candidate/responsive-ui-sanity.html) păstrează eșecul de lățime. Corecția măsoară indicii direct în pixeli și separă în oracol banda expusă de ultima față completă.


- Un test inițial al geometriei cu șase locuri folosea pozițiile vechi; oracolul a fost corectat pentru ordinea circulară și selecția finală a trecut. [Jurnalul inițial](logs/app-initial-seat-oracle-diagnostic.log) păstrează diagnosticul.
- Execuția JVM directă este un control suplimentar al acelorași 28 inițiale de teste; nu este numărată încă o dată.
- Primele două încercări ale candidatului vechi au eșuat la lansare. [Crash-ul](diagnostics/rejected-candidate/crash.txt) identifică `Stream.toList()` indisponibil pe API 28; [raportul FAIL](diagnostics/rejected-candidate/report.html) rămâne separat de verificarea finală trecută. Corecția folosește iterare explicită, fără mărirea timeout-ului.
- Profilul nativ verificat este 720×1280 la 360 dpi, pe API 28. Profilurile înalte, API 37 și telefonul fizic au calificări separate. Modificările paralele ale jocului, comise între timp, sunt excluse din commit-ul izolat și enumerate în proveniență.

[Procesul patch-ului](../../docs/platform/ATF_AndroidDelivery.md#release-de-mentenanță) definește selecția și predarea.
