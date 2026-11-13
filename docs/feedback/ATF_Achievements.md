# Realizări Whist — criterii rămase

## Pe scurt

Validarea YAML la build și compatibilitatea progresului sunt documentate în [contractul realizărilor](../game/ATF_Achievements.md). Lucrările de mai jos urmăresc restaurarea ecranului Android și calibrarea cu jucători umani.

## P1 · Restaurarea stării ecranului comun pe Android

Adaptorul Whist creează o stare nouă a ecranului la fiecare deschidere. Componentele catalogului și detaliului nu demonstrează, singure, că poziția listei și obiectivul selectat se păstrează la recrearea ecranului sau după revenirea dintr-o notificare.

- Păstrează starea de navigare și selecția în proprietarul Android potrivit ciclului de viață, fără să schimbi ordinea sau conținutul catalogului.
- Verifică RO/EN, selectarea unui obiectiv, derularea până la un rând, recrearea ecranului și revenirea la aceeași poziție și selecție.
- Componentele acoperă păstrarea cozii la închidere, reapariția notificării neconfirmate și refuzul unei confirmări depășite. Pe APK, recreează activitatea înainte și după confirmare și confruntă fiecare apariție cu starea cozii și progresul salvat. [Componentele notificării](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementNoticeCoordinatorComponentTest.java)
- Folosește checkpoint-urile existente pentru catalog și detaliu. Extinde proba cu recrearea activității în ambele limbi și verifică revenirea la același obiectiv, la aceeași poziție și cu etichetele vizibile corecte.

[Adaptorul Android](../../whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/presentation/WhistAchievementsScreen.java) · [Ecranul comun](../../whist-lab-app/atf-card-table-ui/README.md#realizări) · [Acceptanța Android](../../atf-test-android/src/test/java/atf/android/e2e/release/WhistReleaseHomeE2eTest.java)

## P2 · Calibrarea ordinii cu jucători umani

Simularea offline măsoară ocaziile și ratele pentru politicile declarate, nu cât de naturală este ordinea pentru jucătorii umani. Păstrează separat observarea umană de calibrarea AI și schimbă pragurile numai după un semnal repetat.

- Folosește protocoale RO/EN și criterii uniforme pentru înțelegerea descrierilor, dificultate și progres.
- Nu amesteca rezultate umane, date de dezvoltare și holdout-ul simulării.
- Orice schimbare de prag sau eligibilitate păstrează ID-urile și trece printr-o probă de compatibilitate a progresului salvat.

**Surse:** [modelul și protocolul de calibrare](../game/ATF_Achievements.md#verificare), [suita offline](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementOfflineCalibrationTest.java).
