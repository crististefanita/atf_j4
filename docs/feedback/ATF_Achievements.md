# Realizări Whist — criterii rămase

## Pe scurt

Catalogul și progresul au contracte proprii în [documentația realizărilor](../game/ATF_Achievements.md). Acest registru păstrează numai verificările încă necesare pentru compatibilitatea YAML/runtime și pentru ecranul Android; pragurile și identitățile existente nu se schimbă fără migrare versionată.

## P1 · Validarea catalogului YAML înainte de generare

Generatorul respinge schema și câmpurile de bază, dar verificarea unei metrici necunoscute sau a unei perechi metrică/agregare incompatibile poate ajunge până la compilarea Java generată. Refuzul trebuie să indice ID-ul și valoarea YAML, înainte să fie produsă sursa.

- Definește setul de metrici admise și combinațiile legale cu agregările într-o singură regulă coerentă cu catalogul runtime.
- Acoperă prin teste pozitive și negative: metrică necunoscută, agregare necunoscută, pereche invalidă, grup gol, ID duplicat, câmp lipsă și text RO/EN gol.
- Verifică faptul că un fișier valid generează aceeași ordine, aceleași ID-uri și aceleași definiții tipizate folosite de aplicație.
- Nu elimina și nu redenumi ID-uri stabile fără migrare care păstrează progresul și coada de notificări.

**Surse:** [generatorul Gradle](../../whist-lab-app/app/achievement-catalog.gradle), [catalogul YAML](../../whist-lab-app/app/src/main/achievement-rules/whist-achievements.yaml), [testele de componentă](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementComponentTest.java).

## P1 · Restaurarea stării ecranului comun pe Android

Adaptorul Whist creează o stare nouă a ecranului la fiecare deschidere. Componentele catalogului și detaliului nu demonstrează, singure, că poziția listei și obiectivul selectat se păstrează la recrearea ecranului sau după revenirea dintr-o notificare.

- Păstrează starea de navigare și selecția în proprietarul Android potrivit ciclului de viață, fără să schimbi ordinea sau conținutul catalogului.
- Verifică RO/EN, selectarea unui obiectiv, derularea până la un rând, recrearea ecranului și revenirea la aceeași poziție și selecție.
- Verifică separat coada durabilă a notificărilor: oprirea înainte de confirmare nu o pierde, confirmarea nu acordă progres din nou, iar reluarea nu afișează duplicate.
- Actualizează proba APK-ului semnat pentru etichetele efective „Înapoi/Back” și „Închide/Close”, cu checkpoint-uri distincte pentru catalog și detaliu.

**Surse:** [adaptorul Android](../../whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/presentation/WhistAchievementsScreen.java), [ecranul comun](../../whist-lab-app/atf-card-table-ui/README.md#realizări), [acceptanța Android](../../atf-test-android/src/test/java/atf/android/e2e/release/WhistReleaseHomeE2eTest.java), [testele de stare și notificare](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementNoticeCoordinatorComponentTest.java).

## P2 · Calibrarea ordinii cu jucători umani

Simularea offline măsoară ocaziile și ratele pentru politicile declarate, nu cât de naturală este ordinea pentru jucătorii umani. Păstrează separat observarea umană de calibrarea AI și schimbă pragurile numai după un semnal repetat.

- Folosește protocoale RO/EN și criterii uniforme pentru înțelegerea descrierilor, dificultate și progres.
- Nu amesteca rezultate umane, date de dezvoltare și holdout-ul simulării.
- Orice schimbare de prag sau eligibilitate păstrează ID-urile și trece printr-o probă de compatibilitate a progresului salvat.

**Surse:** [modelul și protocolul de calibrare](../game/ATF_Achievements.md#verificare), [suita offline](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementOfflineCalibrationTest.java).
