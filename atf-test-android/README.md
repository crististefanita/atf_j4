# Testele și adaptoarele de scenarii Android

## Pe scurt

Modulul combină verificări componente fără dispozitiv cu scenarii Android
activate explicit. Aplicația originală și Whist Lab au pachete, scenarii și
proveniențe distincte. O verificare a uneia nu certifică automat cealaltă.

## Organizare

| Componentă | Responsabilitate |
|---|---|
| [src/main/java](src/main/java/) | Audituri pure pentru dovezi și geometrie, reutilizabile fără ADB. |
| [AndroidUiDriver](src/test/java/atf/android/e2e/AndroidUiDriver.java) | Interacțiuni reale, ierarhie proaspătă și legarea acțiunilor de checkpoint-uri. |
| [AndroidUiFlowTrace](src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java) | Ordinea checkpoint-urilor, observații, comparații evaluate și receipt parțial la eșec. |
| [RomanianMatchOracle](src/test/java/atf/android/e2e/RomanianMatchOracle.java) | Oracol independent pentru reguli și scorul partidei paralele. |
| [OriginalRomanianAudit](src/test/java/atf/android/e2e/OriginalRomanianAudit.java) | Interpretarea și auditul evidenței aplicației originale. |
| [WhistChallenger](src/test/java/atf/android/e2e/WhistChallenger.java) | Proiecția own/public pentru adversarul independent. |
| [Suitele Android](src/test/java/atf/android/suites/) | Selecții explicite de scenarii; sursele claselor definesc activarea lor. |

Clientul ADB și conversia PNG sunt în [atf-android](../atf-android/src/main/java/).
[POM-ul modulului](pom.xml) adaugă `ai-lab` și `game-core` numai la compilarea
testelor. Oracolul trebuie să păstreze calculul expected separat de motorul
verificat, chiar dacă ambele sunt disponibile în classpath.

## Componente fără emulator

Dintr-o copie proprie din `%TEMP%`, cu POM-ul părinte, modulele necesare și
directoarele `lib` și `config`:

```powershell
mvn -B -pl atf-test-android -am "-Dtest=OriginalRomanianAuditTest,OriginalRomanianPublicDecisionTest,RomanianMatchOracleTest,WhistChallengerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Aceste selecții verifică logică și contracte. Nu pretind o partidă nouă pe
Android sau capturi reale de dispozitiv. Versiunile dependențelor și opțiunile
JVM sunt definite de [POM-ul părinte](../pom.xml).

## Scenarii live

[CompleteRomanianExpertE2eTest](src/test/java/atf/android/e2e/CompleteRomanianExpertE2eTest.java) joacă Whist Lab prin configurarea normală.

Numele clasei este de compatibilitate; nivelurile disponibile sunt Normal și Hard. Proprietățile `atf.android.enabled` și `atf.android.fullMatch` activează execuția, iar `atf.android.handLimit` delimitează pilotul față de partida completă. Pachetul, activitatea, serialul ADB și directorul extern al dovezilor trebuie să identifice instanța verificată.

[OriginalRomanianHardLiveE2eTest](src/test/java/atf/android/e2e/OriginalRomanianHardLiveE2eTest.java) are propriile opțiuni live, confirmare exclusivă și lease legat de serial și directorul rulării.

[Runnerul originalului](../scripts/run-original-romanian-hard.ps1) descrie parametrii actuali și continuarea unei partide existente. Fără activarea live se execută pregătirea offline. Nu porniți un scenariu nou pentru a înlocui un link lipsă sau o dovadă parțială.

Scenariile de layout și meniuri sunt separate de o partidă completă.

O probă scurtă nu devine acceptanța tuturor mâinilor. Respectarea regulilor, scorul, victoria și verificarea vizuală sunt rezultate distincte.

## Dovezi și predare

`ACTION` descrie interacțiunea executată; citirea ecranului este `OBSERVE`.

Comparațiile păstrează expected, actual și verdictul real. O finalizare ulterioară nu șterge un FAIL. Capturile Android păstrează PNG-ul original inline Base64; fixture-urile sintetice pentru componente nu sunt prezentate drept poze live.

[Contractul dovezilor](../docs/ATF_Android_Report_Contract.md) și
[predarea rezultatelor existente](../docs/WHIST_UI_REPORT_HANDOFF.md) definesc
publicarea, proveniența și separarea ultimei acceptanțe de ultima încercare.
Detaliile de publicare sunt în
[AndroidUiFlowReportPublisher](src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java).
Un document de overview nu înlocuiește metadata și hash-urile unei execuții.
