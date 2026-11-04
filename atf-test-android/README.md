# Testele Android J4

Modulul verifică aplicația instalată prin ADB și publică dovezile prin API-ul
UI al Reporting J18. Captura, interpretarea interfeței și regulile Whist aparțin
J4; HTML-ul, viewerul PNG și deduplicarea aparțin Reporting. Vezi
[contractul raportului](../docs/ATF_Android_Report_Contract.md) și
[feedback-ul consumatorului](../docs/feedback/ATF_Android_Alignment_Feedback.md).

## Harta de citire

| Componentă | Responsabilitate |
|---|---|
| `AndroidUiDriver` | interacțiuni măsurate, ierarhie proaspătă, captură integrală; o ierarhie veche nu justifică următorul click |
| `AndroidUiFlowTrace` | checkpoint-uri ordonate, acțiuni reale, observații și comparații evaluate, inclusiv receipt parțial la failure |
| `AndroidUiFlowReportPublisher` | compoziție prin API-ul public J18 și auditul fiecărui owner; fără renderer propriu |
| `RomanianMatchOracle` | reconstrucția independentă a distribuției, mutărilor legale, levatelor și scorurilor |
| `CompleteRomanianExpertE2eTest` | partidă nouă prin meniul normal; numele clasei este istoric, nivelurile live sunt `NORMAL` și `HARD` |
| `WhistLabGameplayE2eTest` | fluxuri scurte pentru masă, licitare, meniuri, o carte, 13 cărți și pauza levatei |
| `WhistDenseLayoutE2eTest` | Patru profile, separat pentru 13 indici și nouă butoane la patru jucători. Fiecare captură este legată de aserțiunile sale. |
| `WhistMenuLayoutE2eTest` | Navigare prin meniurile normale EN/RO, reguli la cerere, scor și revenire. |
| `HandIndexAudit` | Suport pur în `src/main/java/atf/android/whist/evidence`. Compară banda expusă și pixelii celor două linii. Componentele acoperă și capturile cu letterboxing. |

Driverul și compoziția scenariilor sunt specifice testelor Android.
Clientul ADB și conversia PNG reutilizabile sunt în `atf-android/src/main/java`.
Motorul aplicației rămâne în `whist-lab-app/game-core`; oracolul testului nu
delegă verdictul către motorul verificat.

## Contractul dovezilor

[Ultimul raport Extent Whist Lab](../docs/feedback/whist-extent/README.md) conține linkul local către HTML, scenariul, verdictul și data UTC a raportului.
Publisherul actualizează indexul după audit, inclusiv pentru un joc eșuat cu diagnostic valid. Componentele, aplicația originală și fișierele din afara directorului declarat al rulării nu înlocuiesc raportul.

Pentru un build în TMP setați `ATF_J4_FEEDBACK_DIRECTORY` la directorul `docs/feedback` al checkout-ului live înainte de Maven.
Opțiunea JVM `atf.android.feedbackDirectory` are prioritate. În checkout, directorul se detectează automat.
E2E-ul declară `atf.android.artifacts` sau `atf.match.evidenceDir` și activează `atf.android.enabled`. Linkul deschide fișierul pe mașina unde se află dovezile. Curățarea acelui director îl invalidează până la următoarea publicare.

Arhiva permanentă păstrează separat ultima acceptanță pe scope și ultima încercare. [Publicarea și replay-ul offline](../docs/WHIST_UI_REPORT_HANDOFF.md)

- `ACTION` este emis pentru interacțiunea executată de driver. Citirea ecranului
  este `OBSERVE`; un checkpoint de observație nu primește un click inventat.
- Comparațiile apar în tabelul standard ASSERTION / EXPECTED / ACTUAL / RESULT.
  Tabelele se deschid la cererea cititorului. Valorile de comparație trebuie să
  încapă în preview-ul configurat; nu scurtăm o valoare până când pare validă.
- Capturi identice pot avea un singur PNG Base64 și mai multe linkuri locale
  `data-atf-screenshot-ref`. Auditorul rezolvă linkul din fiecare checkpoint
  către PNG-ul exact și verifică multiplicitatea. Numărul URI-urilor Base64 nu
  reprezintă numărul capturilor.
- Testele componentă folosesc imagini distincte și identice; linkul corupt este
  respins în ambele cazuri. Imaginile sintetice sunt fixture-uri pentru acest
  contract, niciodată capturi declarate drept execuție Android.
- Observațiile lungi păstrează subpanoul complet; auditorul verifică celula și
  numărul rândului, inclusiv normalizarea JSON, newline-urile și ultima valoare.
  Un FAIL capturat nu este șters de finalizarea ulterioară cu PASS.
- Un PASS funcțional nu închide feedback-ul vizual. Timpii originali ai fazelor,
  migrarea rapoartelor legacy și completitudinea contextului au criterii separate
  în feedback-ul consumatorului.

## Rulare focalizată

Matricea nouă, versiunea APK și dovezile sunt în [acceptanța UI 0.1.3](../docs/WHIST_UI_013_ACCEPTANCE.md).

Build-ul Maven/Gradle se execută într-o copie din `%TEMP%`, conform runnerelor
din `scripts`. Nu rula comenzile de build în checkout-ul live. E2E necesită
`atf.android.enabled=true` și dispozitivul rezervat rulării curente.

| Selecție | Parametri suplimentari |
|---|---|
| `AndroidUiFlowReportPublisherTest,RomanianMatchOracleTest` | fără emulator; contractele raportului și ale regulilor |
| `CompleteRomanianExpertE2eTest` | `atf.android.fullMatch=true`, `atf.android.handLimit=6`, `atf.android.aiDifficulty=NORMAL`, `atf.match.evidenceDir` absolut în TMP |
| `WhistLabGameplayE2eTest#thirteenCardBiddingTableFitsAcrossPortraitDisplayProfiles` | `atf.android.artifacts` absolut în TMP; restaurează rezoluția și densitatea inițiale |

Limita de mâini este 1–24; o probă cu șase mâini este un pilot, nu acceptanța
partidei complete. Testul pornește o distribuție nouă și nu injectează seed,
cărți ori rezultate. `HARD` este nivelul implicit. Pachetul instalat se poate
selecta prin `atf.android.appPackage`, cu activitatea prin
`atf.android.mainActivityClass`; orice variantă izolată se identifică explicit
în receipt și nu este echivalată cu APK-ul livrat fără verificarea provenienței.

Profilul de rezoluție ROG 7 reproduce `1080×2448`, conform
[specificației ASUS](https://rog.asus.com/nz/phones/rog-phone-7/spec/).
Densitatea de 440 dpi este o alegere de test pe emulator, nu o măsurare a
telefonului utilizatorului. Matricea include și viewport-ul de tabletă de
600 dp; testarea pe un telefon fizic rămâne o acceptanță distinctă.
