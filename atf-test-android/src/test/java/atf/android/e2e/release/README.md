# Acceptanța licitației pe APK semnat

## Pe scurt

Colecția verifică legarea recomandării de interfața Android reală. Politica AI și cazurile exhaustive rămân în componentele aplicației; acest parcurs verifică selecția vizibilă, alegerea manuală, dialogul și contractul salvat.

| Nivel | Intrare și contract | Limită |
|---|---|---|
| Componentă | [Decodorul](BidChoiceObservation.java) citește o singură observație XML; [probele sale](BidChoiceObservationComponentTest.java) refuză selecții multiple, confirmări contradictorii și valori ilegale. | Nu citește un dispozitiv și nu estimează licitația. |
| E2E | [Scenariul](ReleaseBidScenario.java) configurează prin UI Whist românesc, patru locuri și Hard; joacă prefixul de o carte și verifică recomandarea la două cărți. | Nu injectează checkpoint-uri sau extras de debug și nu citește mâinile private adverse. |
| E2E AutoGame | [Scenariul](autogame/AutoGameReleaseScenario.java) pornește AutoGame din meniul real, verifică finalizarea mâinii la ecranul scorului și confirmă că următoarea mână revine la controlul manual. | Rulează pe APK-ul candidat identificat și cere aceeași rezervare exclusivă și restaurare a datelor; componentele nu înlocuiesc această probă. |
| E2E interfață comună | [Meniul și profilul](WhistReleaseHomeE2eTest.java) verifică RO/EN, anularea și salvarea numelui, apoi preferințele după relansare. | Folosește același backup verificat și nu pornește o partidă. |
| Predare | [Testul](WhistReleaseBidE2eTest.java) păstrează cauza originală și publică observațiile, comparațiile și PNG-urile prin raportarea UI comună. | Publicarea raportului nu substituie restaurarea datelor de către runner. |

## Precondiții și restaurare

[Contextul](ReleaseBidContext.java) cere rezervarea exclusivă curentă, același serial, identitatea APK-ului și o copie verificată a datelor private înainte de pornirea unui joc nou. Runnerul pregătește `setup-release.json`, `device-lease.json` și `app-data-before.tar`; hash-ul copiei și obligația restaurării sunt explicite.

- `setup-release.json` păstrează serialul, API-ul, calea pachetului instalat, `requestedApkSha256`, `backupSha256`, `backupScopeValidated=true` și `restoreRequired=true`. O comandă de descoperire eșuată nu înseamnă absența datelor.
- Runnerul restaurează datele și configurația chiar după eșec și verifică identitatea APK-ului păstrat la final. Un eșec al restaurării rămâne separat de cauza inițială și blochează acceptanța.
- Selecția neconfirmată trebuie păstrată după închiderea dialogului în aceeași sesiune. Contractul confirmat și starea publică trebuie păstrate după oprirea procesului și reluare.
- Lipsa emulatorului ori a copiei nu devine succes sau dovadă E2E. Un preflight neexecutat rămâne `PENDING` în dosarul candidatului.

## Execuție

Testele se selectează explicit prin `WhistReleaseBidE2eTest` și `WhistReleaseHomeE2eTest`, cu `atf.android.enabled=true`, `whist.records.runRoot`, `atf.android.artifacts` și serialul rezervat. [Suita E2E primară](../../suites/AndroidE2eSuite.java) păstrează proprietatea testelor, iar [suita componentelor](../../suites/AndroidComponentSuite.java) descoperă separat probele decodorului.

[Calificarea unui release](../../../../../../../../docs/testing/ATF_ReleaseQualification.md) deține ordinea verificărilor, capturile inline, delimitarea rapoartelor și promovarea dosarului.
