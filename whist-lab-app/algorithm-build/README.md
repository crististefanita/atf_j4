# Offline algorithm build / Build offline pentru algoritmi

Run from a test-owned TMP copy of whist-lab-app:

~~~powershell
.\gradlew.bat -p algorithm-build :game-core:quickRegressionTest --offline --no-daemon
.\gradlew.bat -p algorithm-build :game-core:test --offline --no-daemon
~~~

This entrypoint includes the existing game-core project and its independently implemented coach test sources. It configures neither the Android app nor the Android Gradle plugin; ANDROID_HOME is not required. It does not change the Android build. JDK 17+ and locally cached JUnit/Jackson/Jacoco dependencies are required. Dependency absence fails explicitly in offline mode.

Learning uses the explicit roundSevenTest task, a fresh external output directory, declared seed range and immutable candidate identity. Never run a historical tournament through the default test task. Learning has no Jacoco agent: it measures strategy progression; coverage belongs to component regression. Tests and output paths are identical to the normal game-core project. Do not run both entrypoints concurrently in the same staging directory.

Colecțiile quick/regression/documentation/learning folosesc același cod și aceleași reguli. Artefactele de execuție rămân în TMP și whist_back; nu se construiește în checkout. Nicio captură Android sau acceptanță UI nu este dedusă din această suită.
