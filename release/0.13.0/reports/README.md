# Dovezi componente — 0.13.0

Fiecare fișier HTML este raportul Extent canonic al unei singure colecții Gradle. Capturile PNG sunt încorporate în HTML ca base64; nu există director paralel de imagini sau copii ale raportării JUnit. Rapoartele de componente nu sunt E2E.

| Raport | Nivel | Rezultat |
|---|---|---:|
| `android-bidding.html` | Componentă Android · Bidding | 248 trecute |
| `android-commands.html` | Componentă Android · Commands | 108 trecute |
| `android-display.html` | Componentă Android · Display | 85 trecute |
| `android-history.html` | Componentă Android · History | 61 trecute |
| `game-core-rules-session.html` | Componentă JVM · reguli și sesiune (filtrată) | 44 trecute |
| `ios-engine-bridge.html` | Componentă JVM · punte motor iOS | 3 trecute |

Rezultatele au ieșit din Gradle cu identități JUnit/HTML corespondente și un singur HTML canonic per task. E2E nu a fost rulat. Starea APK-ului și limitarea verificării full core sunt în [validarea release-ului](../release-validation.json).
