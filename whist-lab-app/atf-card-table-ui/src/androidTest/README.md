# Componente native ale mesei și cărților

## Pe scurt

Testele instrumentate verifică geometria reală a rândurilor în layout-ul Android, pentru dimensiuni și scalări de font reprezentative.

Matricea măsoară componentele Android comune la 3–6 locuri, 240, 320 și 393 dp, cu font normal, 1,5× și 2×. Numele cu diacritice, statisticile Whist, etichetele complete și simbolurile Δ/Σ intră în același chenar.

Fiecare rând trebuie să aibă text integral, fără elipsă, în lățimea alocată. Chenarele trebuie să încapă în masă și să nu se intersecteze.

Construiți în workspace-ul extern, instalați APK-ul de test izolat și porniți runnerul nativ.

```powershell
.\gradlew.bat --offline :atf-card-table-ui:assembleDebugAndroidTest
adb shell am instrument -w ro.cardtable.ui.test/ro.cardtable.ui.seat.SeatFrameComponentInstrumentation
```

Linia `ATF_NATIVE_SEATS` consemnează măsurătorile și rezultatul fiecărui rând. Câmpul `cardHighlights` verifică evantaiul la 280 și 393 dp cu font 1, 1,5 și 2. Proba compară aceleași limite și acțiuni cu și fără marcaj și măsoară ridicarea unei cărți dezvăluite.

`gameTitleBars` verifică 320, 360, 393 și 600 dp cu font 1, 1,5 și 2, în română și engleză. Fiecare caz măsoară poziția bifei, ținta tactilă și o singură intenție după atingere.

[Strategia comună de testare](../../../../docs/testing/README.md)
