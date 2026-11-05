# Dovezile livrării 1.9.2

## Pe scurt

Patch față de 1.9.1, construit în TMP din commit-ul `6e8f3f38e94f0513597b197bc8fa60ce73ee371b`. Dosarul păstrează selecția acestei livrări, fără repetarea regresiei complete.

[Catalogul livrărilor](../README.md) · [Strategia comună de testare](../../docs/testing/README.md)

## APK

- **[Descarcă WhistLab-1.9.2.apk](https://github.com/crististefanita/atf_j4/releases/download/v1.9.2/WhistLab-1.9.2.apk)** · [Checksum](https://github.com/crististefanita/atf_j4/releases/download/v1.9.2/WhistLab-1.9.2.apk.sha256).
- `versionCode 12`, `minSdk 24`, `targetSdk 36`; 775,288 bytes.
- SHA-256: `39d31c4433488ee8d77499f196a888eb8ef230ca5d36940bd66ee9ac5a3feb94`.
- Semnătura păstrează cheia de upgrade; copia locală curentă și checksum-ul sunt în `dist`, excluse din Git.

## Verificări

| Arie | Rezultat și dovadă |
|---|---|
| Geometrie și ordine | 28 teste trecute în aplicație: proporție 5:7, mână stabilă de 1–13 cărți, levată independentă de 3–6 jucători, arc superior și tură reală în toate variantele; XML în `test-results/app/`. |
| Oracole ATF | 75 teste trecute: înălțime și lățime exacte, geometrii deformate respinse, viewport comun și audit fără transport; XML în `test-results/atf/`. |
| Build | `assembleRelease`, `lintVitalRelease` și selecția componentelor; [jurnal](logs/release-192-build.log). |
| Pachet | CRC, SDK-uri, [semnătură](logs/apk-verification.log), [aliniere](logs/apk-alignment.log) și cheie de upgrade verificate. |

[Manifestul](release-validation.json) identifică rezultatele, guvernanța și distribuția. [Proveniența](source-provenance.json) confruntă fișierele Android cu sursa commit-ului construit și declară diferențele paralele excluse.

## Diagnostic și limite

- Un test inițial al geometriei cu șase locuri folosea pozițiile vechi; oracolul a fost corectat pentru ordinea circulară și selecția finală a trecut. [Jurnalul inițial](logs/app-initial-seat-oracle-diagnostic.log) păstrează diagnosticul.
- Execuția JVM directă este un control suplimentar al acelorași 28 de teste; nu este numărată încă o dată.
- Nu se preia implicit calificarea nativă a versiunii precedente. Acceptanța live se declară separat, cu profilul și binarul efectiv.
- API 37 și telefonul fizic nu sunt calificate prin aceste componente; modificările paralele necomise nu intră în APK.

[Procesul patch-ului](../../docs/platform/ATF_AndroidDelivery.md#release-de-mentenanță) definește selecția și predarea.
