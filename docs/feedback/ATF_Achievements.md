# Realizări Whist — verificări deschise

## Pe scurt

Prioritatea rămasă este calificarea Android a ecranului comun și validarea generatorului YAML pentru intrări negative. ID-urile vechi, progresul versionat, eligibilitatea, pragul procentual exact, acumularea simultană și reluarea fără credit dublu au verificări de componentă. [Contractul Realizărilor](../game/ATF_Achievements.md) · [Testele de prag și compatibilitate](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementComponentTest.java)

## P1 · Calificarea Android a ecranului comun

**Responsabil · adaptorul și acceptanța Android**

Proba APK-ului semnat acoperă revenirea din catalog și închiderea detaliului. Adaptorul creează o stare nouă la fiecare deschidere, astfel că derularea și detaliul selectat se pierd. [Proba Android](../../atf-test-android/src/test/java/atf/android/e2e/release/WhistReleaseHomeE2eTest.java) · [Adoptarea ecranului](../../whist-lab-app/app/src/main/java/ro/whistlab/app/MainActivity.java)

- Păstrează poziția listei și detaliul deschis la recrearea ecranului. Verifică restaurarea pe profil compact, cu font mărit, în RO/EN.
- Adaugă un traseu scurt de la o mână decontată la notificare. O relansare înainte de confirmare păstrează notificarea. După confirmare, progresul rămâne salvat și notificarea dispare la reluare.

## P1 înainte de revizia catalogului · Compatibilitate și YAML

**Responsabil · catalogul și progresul salvat**

Starea recalculează deblocarea după pragul curent, iar codec-ul cere ca ID-urile salvate să existe în catalog. Componenta verifică acum păstrarea celor 40 de ID-uri istorice și round-trip pentru progres, replay și notificări. Înaintea oricărei schimbări de prag sau retrageri de ID, adăugați o migrare versionată care păstrează câștigurile vechi și nu emite notificări noi. [Starea progresului](../../whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/WhistAchievementState.java) · [Codec-ul](../../whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/WhistAchievementCodec.java) · [Testele de compatibilitate](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementComponentTest.java)

- Generatorul trebuie să respingă o metrică necunoscută și o pereche incompatibilă precum `EXACT_STREAK` cu `SUM`. Fiecare eroare indică ID-ul în YAML înainte de compilarea Java. [Generatorul](../../whist-lab-app/app/achievement-catalog.gradle)
- La extindere, declară eligibilitatea după variantă, locuri și reguli opționale. Un obiectiv despre atu sau despre o decizie manuală cere dovada acțiunii în checkpoint. Adaugă căutare și filtre când catalogul se extinde, folosind aceeași stare restaurabilă.

## P2 · Validarea ordinii cu jucători umani

**Responsabil · calibrare și produs**

Într-o evaluare manuală, verifică dacă ordinea grupelor ghidează firesc jucătorii și dacă regulile din detalii se înțeleg fără explicații externe. Păstrează rezultatele în cercetarea de produs. Ajustează pragurile după un semnal repetat și după migrarea compatibilă a progresului salvat. [Protocolul de calibrare](../game/ATF_Achievements.md)
