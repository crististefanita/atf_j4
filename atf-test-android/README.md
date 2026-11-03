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

Suportul de aici este specific acceptanței Android și rămâne în sursele de test.
Clientul ADB și conversia PNG reutilizabile sunt în `atf-android/src/main/java`.
Motorul aplicației rămâne în `whist-lab-app/game-core`; oracolul testului nu
delegă verdictul către motorul verificat.

## Contractul dovezilor

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
