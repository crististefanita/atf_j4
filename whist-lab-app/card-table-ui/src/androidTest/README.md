# Componente native ale chenarului

## Pe scurt

Testele instrumentate verifică geometria reală a rândurilor în layout-ul Android, pentru dimensiuni și scalări de font reprezentative.

Matricea măsoară componentele Android comune la 3–6 locuri, 240, 320 și 393 dp, cu font normal, 1,5× și 2×. Numele cu diacritice, statisticile Whist, etichetele complete și simbolurile Δ/Σ intră în același chenar.

Fiecare rând trebuie să aibă text integral, fără elipsă, în lățimea alocată. Chenarele trebuie să încapă în masă și să nu se intersecteze.

Construiți în workspace-ul extern, instalați APK-ul de test izolat și porniți runnerul nativ.

```powershell
.\gradlew.bat --offline :card-table-ui:assembleDebugAndroidTest
adb shell am instrument -w ro.cardtable.ui.test/ro.cardtable.ui.seat.SeatFrameComponentInstrumentation
```

Linia `ATF_NATIVE_SEATS` consemnează măsurătorile și rezultatul fiecărui rând.

[Strategia comună de testare](../../../../docs/testing/README.md)
