# Testele Android Whist Lab

Acest modul verifică jocul nostru: testele fără emulator pentru componentele
și oracolele Whist Lab, plus scenarii UI activate explicit care acționează
elementele afișate în aplicația ro.whistlab.app. Interacțiunile cu aplicația
Contract Whist sunt în proiectul J5.

## Organizare

| Zonă | Responsabilitate |
|---|---|
| src/main/java | Audituri Whist Lab reutilizabile și validări pure fără ADB. |
| AndroidUiDriver | Acțiuni Android, ierarhie proaspătă, capturi și măsurarea inputului. |
| AndroidUiFlowTrace | Checkpoint-uri ordonate, observații, comparații și receipt parțial la eșec. |
| WhistTestEvidence | Proiecția checkpoint-urilor și aserțiunilor jocului în raport. |
| RomanianMatchOracle | Așteptări independente pentru reguli și scorul românesc. |
| OriginalWhistState + OriginalMatchOracle | Adaptor de snapshot J5 și oracol comparativ folosite numai pentru replay-ul Whist Lab; fără input în aplicația-model. |
| WhistChallenger | Decizie own/public pentru adversarul de verificare offline. |
| Suitele Android | Selecții explicite pentru scenariile Whist Lab. |

POM-ul modulului adaugă sursele game-core și ai-lab numai la testele obișnuite.
Profilul responsive-sanity izolează clasele necesare capturilor de layout și
verifică APK-ul instalat; nu reconstruiește aplicația.

## Testare fără emulator

Dintr-o copie temporară a proiectului, cu POM-ul părinte, bibliotecile locale
și config:

    mvn -B -pl atf-test-android -am "-Dtest=RomanianMatchOracleTest,WhistChallengerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

Aceste teste validează calcule și contracte. Nu pretind o partidă pe Android,
o captură live sau acceptanță vizuală. Motorul complet și evaluările offline
sunt documentate în game-core/README.md și TEST_COLLECTIONS.md.

## E2E pentru Whist Lab

CompleteRomanianExpertE2eTest joacă mâini normale prin meniul Whist Lab.
WhistResponsivePlaySanityE2eTest pornește din meniul normal și verifică profile
de ecran, primul joc real și layout-urile-limită. CompleteWhistLabMatchE2eTest
reia în UI-ul Whist Lab intrările unei partide înregistrate și confruntă
mutările, levatele și scorul cu un oracol independent.

Replay-ul comparativ consumă date J5 numai ca fixture-uri. Nu pornește
Contract Whist și nu folosește cărți ascunse ca intrări pentru AI-ul Whist Lab.

Pentru fiecare acțiune UI, raportul păstrează acțiunea trimisă separat de
starea observată după ea. Captura și aserțiunile aparțin aceluiași checkpoint.
Un tap ADB reușit nu dovedește singur că jocul a acceptat licitația sau cartea.

## Rapoarte

[Contractul de raportare pentru consumerul Whist Lab](../docs/ATF_Android_Report_Contract.md)
definește dovezile cerute jocului. Rapoartele, capturile și trace-urile sunt
salvate în directorul extern configurat, nu în surse sau target. Defectele
generice ale contractului ori rendererului Reporting se raportează la
[J0 feedback](../../atf_j0/docs/feedback/README.md); J4 păstrează numai
comportamentul și verificările proprii jocului.

## Legături

[Verificarea UI pe mai multe profile](../docs/WHIST_RESPONSIVE_UI_SANITY.md) ·
[Testarea Android a jocului](../docs/ANDROID_TESTING.md) ·
[Feedback Whist Lab](../docs/feedback/README.md)