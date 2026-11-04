# Build JVM pentru reguli și algoritmi

## Pe scurt

Această intrare Gradle include proiectul existent `game-core`, fără configurarea
aplicației sau a pluginului Android. Relația este definită în
[settings.gradle](settings.gradle); sursele și task-urile rămân în
[game-core/build.gradle](../game-core/build.gradle).

Sunt necesare JDK 17 sau mai nou, wrapperul Gradle și dependențele JVM ale
testelor. `ANDROID_HOME` nu este necesar aici. `--offline` folosește cache-ul
local și eșuează explicit dacă lipsește o dependență.

Dintr-o copie proprie din `%TEMP%` a `whist-lab-app`:

```powershell
.\gradlew.bat -p algorithm-build :game-core:quickRegressionTest --offline --no-daemon
.\gradlew.bat -p algorithm-build :game-core:regressionTest --offline --no-daemon
.\gradlew.bat -p algorithm-build :game-core:contextRegressionTest --offline --no-daemon
.\gradlew.bat -p algorithm-build :game-core:tasks --all --offline --no-daemon
```

`game-core` adaugă sursele jucătorului independent numai în classpath-ul testelor.
Task-ul implicit exclude turneele, mining-ul și cercetările opt-in conform
build-ului curent. `contextRegressionTest` selectează componentele contextuale;
nu joacă un turneu complet.

[Runnerul colecțiilor](../../scripts/run-whist-collection.ps1) citește [catalogul](../../config/test-collections.json), creează o copie temporară și alege task-ul după colecție și protocol.

Pentru `learning` sunt obligatorii candidatul, corpusul extern și protocolul explicit. Reluarea parțială declară exact cazurile lipsă și păstrează aceeași sursă; evaluarea neatinsă cere freeze și rezervare înainte de deschidere. Task-urile istorice nu sunt alegeri implicite pentru un experiment nou.

XML-ul nativ și fișierele de build apar în `game-core/build` din copia temporară.

Pentru capturarea componentelor se poate furniza un director extern nou prin `whist.componentEvidence`; ID-urile capturilor nu se suprascriu. Nu rulați simultan cele două intrări Gradle în aceeași copie, deoarece folosesc aceleași directoare de ieșire.

Această intrare nu construiește, instalează sau controlează o aplicație Android.

Verificarea modelului, puterea statistică pe partide noi și acceptanța interfeței au criterii diferite.
