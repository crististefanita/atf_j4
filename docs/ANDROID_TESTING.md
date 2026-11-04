# Testarea Android

Testele de domeniu, contractele adaptorului și acceptanța pe dispozitiv au puncte de intrare separate. Comenzile offline nu presupun un emulator disponibil.

## Sursele și responsabilitățile

| Zonă | Sursă | Responsabilitate |
|---|---|---|
| Domeniu | [game-core](../whist-lab-app/game-core) | Reguli, scor, stare salvată și decizii AI |
| Adaptor | [atf-android](../atf-android) | Comenzi ADB delimitate și validarea capturilor PNG |
| Fluxuri UI | [atf-test-android](../atf-test-android) | Interacțiuni, observații și oracole independente |
| Aplicație | [whist-lab-app](../whist-lab-app) | Interfața Android și integrarea domeniului |

## Pregătire fără dispozitiv

Din rădăcina proiectului:

```powershell
.\scripts\run-whist-collection.ps1 -Collection context
.\scripts\run-whist-collection.ps1 -Collection regression
```

Aceste comenzi folosesc build-ul JVM izolat. Detaliile colecțiilor și ale rezultatelor native sunt în [TEST_COLLECTIONS](TEST_COLLECTIONS.md).

Contractele adaptorului se pot selecta explicit în Maven:

```powershell
mvn -o -pl atf-android -am test `
  '-Dtest=AndroidComponentSuite' `
  '-Dsurefire.failIfNoSpecifiedTests=false'
```

Sunt necesare JDK-ul declarat în `pom.xml`, bibliotecile locale și dependențele deja disponibile în repository-ul Maven. Pentru un repository separat, transmite `-Dmaven.repo.local`.

[Runnerul aplicației originale](../scripts/run-original-romanian-hard.ps1) are și o pregătire offline proprie:

```powershell
.\scripts\run-original-romanian-hard.ps1
```

Fără `-WithAndroid`, rulează numai clasele de componentă declarate în script.

Nu revendică lease-ul, nu trimite ADB și nu începe o partidă. `-JavaHome`, `-Maven`, `-MavenRepository` și `-TestHeapMax` selectează uneltele și limitele acestei execuții. Rezultatele se salvează într-un director extern nou.

## Activare explicită pe dispozitiv

Acceptanța UI cere confirmarea disponibilității exclusive a emulatorului și parametrii mediului corect. Valorile implicite pentru `-AdbExecutable`, `-Serial` și `-AdbServerSocket` sunt declarate în script; ele nu sunt detectate automat.

Pentru aplicația originală, după confirmarea dispozitivului liber:

```powershell
.\scripts\run-original-romanian-hard.ps1 `
  -WithAndroid -ExclusiveDeviceConfirmed
```

Runnerul califică mai întâi componentele, revendică exclusiv lease-ul, verifică versiunea instalată acceptată de parser și pornește testul live. Testul verifică și tokenul, serialul și directorul propriu al lease-ului înainte de interacțiuni. Eliberarea lease-ului se face sub lock și numai pentru tokenul propriu.

[AndroidE2eActivationExtension](../atf-test-android/src/test/java/atf/android/e2e/AndroidE2eActivationExtension.java) dezactivează testele Android fără `atf.android.enabled=true`. Fluxul original complet are suplimentar opt-in-ul său, confirmarea exclusivității și lease-ul propriu. Un test dezactivat sau sărit nu reprezintă o acceptanță UI reușită.

`run-deep-tests.ps1`, `run-complete-match.ps1` și `run-romanian-expert.ps1` sunt fluxuri active: pot conecta ADB, construi sau instala aplicația și porni interacțiuni.

Nu au aceeași barieră `-WithAndroid` și nu sunt comenzi de pregătire offline. Verifică parametrii și coordonarea dispozitivului înainte de folosirea lor.

`run-rule-components.ps1` include și construirea APK-ului și un turneu; pentru componentele JVM restrânse folosește colecțiile de mai sus.

Închiderea ferestrei scrcpy nu oprește Android sau comenzile ADB.

## Continuarea aceleiași partide

`-ContinueFrom` al runnerului original indică o execuție păstrată. Folosește un `-RunRoot` nou și aceleași condiții de acces exclusiv. Continuarea verifică partida existentă, prefixul observat și eventuala acceptare a ultimului input; nu presupune că un tap trebuie repetat.

Capturile, logurile și verdictul tentativei anterioare rămân păstrate. Eroarea anterioară este proveniență distinctă, nu un rezultat rescris al noii continuări. O partidă incompatibilă cu prefixul păstrat trebuie respinsă.

## Contractul observațiilor

Deciziile asupra aplicației originale trec prin [OriginalRomanianPublicDecision](../atf-test-android/src/test/java/atf/android/e2e/OriginalRomanianPublicDecision.java): numai mâna proprie și informația publică. Auditul de după mutare poate verifica separat toate cărțile pentru proprietate, legalitate, câștigător și scor; aceste date nu devin intrări ascunse pentru AI.

[OriginalRomanianDecisionReceipt](../atf-test-android/src/test/java/atf/android/e2e/OriginalRomanianDecisionReceipt.java) confruntă cartea sau licitația aleasă cu acțiunea efectiv observată. Reușita comenzii ADB nu dovedește singură acceptarea inputului. Prefixul public este comparat integral, chiar când afișarea sa folosește lungime și SHA-256.

Pentru Whist Lab, verificările stării salvate urmăresc [GameSession](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/GameSession.java), [ScoreHistory](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ScoreHistory.java) și [GameSessionStore](../whist-lab-app/app/src/main/java/ro/whistlab/app/GameSessionStore.java).

Acceptanța ecranului de scor trebuie să confrunte datele tuturor jucătorilor pentru fiecare mână: licitația, punctele mâinii și totalul cumulativ. Existența structurii sau trecerea componentelor nu dovedește afișarea corectă pe dispozitiv.

Fluxurile păstrează checkpoint-uri ordonate, acțiuni măsurate, comparații și PNG-uri complete.

[AndroidUiFlowTrace](../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java) definește manifestul, iar [publisherul UI](../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java) păstrează imaginile inline Base64 și verdictul original. Randarea vizuală se verifică separat de integritatea datelor.

Pentru orice execuție, distinge explicit componentele trecute, APK-ul construit, aplicația instalată, fluxul UI executat și obiectivul partidei.

Finalizarea unei partide cu reguli corecte și câștigarea ei sunt rezultate diferite.
