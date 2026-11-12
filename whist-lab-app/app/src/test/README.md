# Componentele JVM ale aplicației Android

## Pe scurt

Aceste teste verifică politicile și modelele aplicației fără instalare sau emulator. [Strategia comună](../../../../docs/testing/README.md) deține acceptarea rezultatului; [arhitectura aplicației](../main/java/ro/whistlab/app/README.md) delimitează integrarea Android rămasă.

## Familii de verificare

| Familie | Intrare controlată și oracol |
|---|---|
| Layout și text | Dimensiuni, densitate, font și număr de locuri; rezervarea maximului programului complet, inclusiv 17 cărți la trei locuri, ancore stabile între faze și titlul accesibil pe cel mult două rânduri. |
| Adaptoare UI comune | [Textele și callback-urile](java/ro/whistlab/app/screens/WhistMenuContentComponentTest.java) și [toate fețele de carte](java/ro/whistlab/app/cards/WhistCardFaceComponentTest.java) păstrează semantica Whist la frontiera bibliotecii. |
| Selecția licitației | [Matricea contextuală](java/ro/whistlab/app/bidding/HumanBidMatrixComponentTest.java): 1–8 cărți, 3–6 locuri și fiecare poziție de licitare; observație publică, evaluare Hard și oracol aritmetic independent pentru legalitate. |
| Acțiunile la licitare | [Componentele de layout](java/ro/whistlab/app/bidding/AutoGameBidActionComponentTest.java) verifică rândurile centrate, conturul redus separat de zona tactilă, densitățile și spațiul recuperat pentru textul mărit. [Componenta meniului](java/ro/whistlab/app/screens/GameMenuContentComponentTest.java) verifică opțiunea de oprire în RO și EN, numai în timpul automatizării. |
| Continuitatea licitației | [Salvare și reluare](java/ro/whistlab/app/bidding/HumanBidResumeComponentTest.java): codec JSON real, recomandare recalculată, alegere manuală prioritară și contract confirmat păstrat; livrările întârziate nu schimbă sesiunea. |
| Licitație incertă | [Factori de context](java/ro/whistlab/app/bidding/HumanUncertainBidComponentTest.java): atu și carte întoarsă, ținte adverse publice, scor relativ și plata seriilor. Oracolele disting schimbarea legitimă a utilității de schimbarea accidentală a lumilor simulate. |
| Realizări | [Faptele, regulile și persistența](java/ro/whistlab/app/achievements/WhistAchievementComponentTest.java), [proiecția textului](java/ro/whistlab/app/achievements/WhistAchievementPresentationComponentTest.java) și [coordonarea notificărilor](java/ro/whistlab/app/achievements/WhistAchievementNoticeCoordinatorComponentTest.java): catalog complet, ID-uri persistente, eligibilitate după variantă/masă/reguli, praguri exacte, progres simultan, reluare fără credit dublu, format RO/EN și notificări restaurabile. |
| Calibrarea realizărilor | [Jocurile offline pereche](java/ro/whistlab/app/achievements/WhistAchievementOfflineCalibrationTest.java) folosesc politicile AI reale pe seed-uri repetabile, compară măsurile pe partidă cu progresul cumulativ și verifică persistența după replay. Sunt excluse din colecția rapidă; `:app:testDebugAchievementCalibration` rulează separat, pe intervale externe și disjuncte, pentru 3–6 locuri și variante românești 1–8–1/8–1–8. Rezultatele măsoară auto-jocul, eligibilitatea și realizările pe meci și nu estimează reușita umană. |
| Persistență și istoric | Comenzi legale, checkpoint JSON, solduri și migrare; refuzul datelor corupte. |
| Ciclu de viață | Ceas, executor, navigare și callback-uri controlate; anulare și reluare fără efect repetat. |
| Actorul mesei | [Proiecția Whist](java/ro/whistlab/app/table/content/WhistTableContentComponentTest.java) verifică actorul la licitare și joc pentru 3–6 locuri, marcajul în AutoGame, lipsa marcajului la colectarea levatei și dezactivarea coerentă a comenzilor umane. |
| Colectarea levatei | Geometrie, jucători, ultima levată și evenimente întârziate; starea partidei se păstrează. |

## Execuție și limite

[Componentele bibliotecii UI](../../../atf-card-table-ui/README.md#verificare) au sarcina Gradle proprie. Filtrul de afișaj al aplicației include adaptoarele din `screens` și `cards`; testele existente de geometrie rămân la integrarea Whist și nu sunt numărate din nou ca teste noi ale bibliotecii.

- **Selecție focalizată:** `:app:testDebugUnitTest --tests 'ro.whistlab.app.bidding.*'` include matricea, reluarea și cazurile mici ale estimării. Fixture-urile avansează motorul prin comenzi legale, cu scor și carte întoarsă reale, fără arhivă externă sau emulator.

- **Realizări:** `:app:testDebugAchievementsComponents` este colecția primară pentru catalog, măsurători și stare. `:app:testDebugCommandsComponents` acoperă ordinea în care partida completă persistă rezultatele derivate.

- **Calibrare opțională:** `:app:testDebugAchievementCalibration` primește `whist.achievementCalibrationPhase`, `whist.achievementCalibrationFirstSeed`, `whist.achievementCalibrationMatches` și `whist.achievementCalibrationOutput`. `whist.achievementCalibrationPlayers` acceptă 3–6, `whist.achievementCalibrationMode` acceptă `ROMANIAN_WHIST` sau `LONG_ROMANIAN`, iar `whist.achievementCalibrationSeriesBonus` acceptă 5 sau 10; valoarea configurată controlează bonusul și penalizarea de serie. Seriile și Joc la 0 pot fi comutate prin `whist.achievementCalibrationStreaks` și `whist.achievementCalibrationZero`. `whist.auditOutput` și `whist.auditSource` aleg raportul Extent și identitatea sursei. Ieșirea rămâne în afara depozitului; comparația 5/10 folosește aceleași seed-uri în fiecare fază, iar holdout-ul folosește seed-uri noi. Sarcina nu este inclusă în colecția primară și nu rulează E2E.

- **Execuție:** dintr-o copie temporară a aplicației, [configurația Gradle](../../build.gradle) selectează componentele prin `whist.displayComponents=true`. SDK-ul este necesar compilării; testele rulează pe JVM, fără comenzi ADB.

- **Raster:** [colecția dedicată](../rasterTest/README.md) folosește JDK-ul grafic complet și o sarcină distinctă. Imaginile sale sunt probe de componentă, separate de fontul, pixelii și interacțiunea Android reale.

- **Dovezi:** artefactele și rezultatele aparțin execuției externe. [Calificarea unui release](../../../../docs/testing/ATF_ReleaseQualification.md) decide selecția nativă necesară după verificările locale.

[Aplicația](../main/java/ro/whistlab/app/README.md) · [Strategia comună](../../../../docs/testing/README.md) · [Componente display](../../../../docs/testing/ATF_DisplayComponents.md)

## Proiecția Whist a mesei comune

[WhistTableContentComponentTest](java/ro/whistlab/app/table/content/WhistTableContentComponentTest.java) verifică toate modurile Whist și mesele de 3–6 locuri: faptele și accesibilitatea, actorul la licitare/joc/AutoGame/colectare, ordinea și identitatea cărților, controlul uman dezactivat și levata publică completată. Testul folosește motorul Whist real și rămâne în aplicație.

Componentele din bibliotecă nu importă această sesiune. Ele verifică prezentarea neutră; fiecare adaptor păstrează oracolele propriilor reguli, alegeri, secvențe și punctaj.
