# Acceptanța licitației pe APK semnat

## Pe scurt

Colecția verifică legarea recomandării de interfața Android reală. Politica AI și cazurile exhaustive rămân în componentele aplicației; acest parcurs verifică selecția vizibilă, alegerea manuală, dialogul și contractul salvat.

| Nivel | Intrare și contract | Limită |
|---|---|---|
| Componentă | [Decodorul](turn/BidChoiceObservation.java) citește o singură observație XML; [probele sale](turn/BidChoiceObservationComponentTest.java) refuză selecții multiple, confirmări contradictorii și valori ilegale. | Nu citește un dispozitiv și nu estimează licitația. |
| E2E | [Scenariul](ReleaseBidScenario.java) configurează prin UI Whist românesc, patru locuri și Hard; verifică recomandarea și alegerea unei alternative pe prima mână cu două opțiuni legale. Joacă o mână pregătitoare dacă dealerul are o singură opțiune. | Nu injectează checkpoint-uri sau extras de debug și nu citește mâinile private adverse. |
| Alegere manuală sub dialog | [Componenta de reluare](../../../../../../../../whist-lab-app/app/src/test/java/ro/whistlab/app/bidding/HumanBidResumeComponentTest.java) verifică dialogurile suprapuse și răspunsul AI întârziat. | Pe APK, scenariul de licitare păstrează trecerea prin dialogul de scor și confirmarea manuală. Testul dedicat istoricului deține verificarea nativă a legendei. |
| E2E AutoGame | [Scenariul](autogame/AutoGameReleaseScenario.java) verifică poziția bifei, finalizarea automată la scor și revenirea la control manual. Pe mâna următoare compară chenarele între licitare și joc, marcajul unic al actorului și vizibilitatea cărții. | Așteaptă stări stabile ale interfeței. Verificarea marcajului în timpul turelor rapide aparține componentelor proiecției. |
| E2E masă cu trei locuri | Scenariul imbricat din [WhistReleaseBidE2eTest](WhistReleaseBidE2eTest.java) verifică primul contract de 17 cărți în Coborâre și urcare, accesul la toate cele 17 cărți, prin derularea mâinii când lățimea o cere, apoi compară dreptunghiurile locurilor după tranziția la joc. | Folosește un singur profil Android și nu substituie matricea de componente pentru rezoluții, densități și fonturi mărite. |
| E2E responsive pe release | Aceeași trecere reală de la licitare la joc rulează pe profilurile configurate, cu restaurarea afișajului la final. | Matricea cu fixture-uri sintetice rămâne în testul responsive pe build-ul debug. |
| Meniu, realizări, profil și istoric | [Testul interfeței comune](WhistReleaseHomeE2eTest.java) verifică RO/EN, reluarea aceleiași mâini în istoricul de scor, salvarea numelui prin dialogul Android și afișarea după relansare și deschiderea catalogului și a detaliului unei realizări. Componenta prezentării verifică ordinea, numărătoarea, progresul reîncărcat și textele RO/EN. Componenta profilului verifică anularea, persistența simulată și refuzul scrierii. | Fluxurile folosesc aceeași limită de izolare a datelor; catalogul nu pornește o partidă și nu schimbă progresul. |
| Predare | [Testul](WhistReleaseBidE2eTest.java) păstrează cauza originală și publică observațiile, comparațiile și PNG-urile prin raportarea UI comună. | Publicarea raportului nu substituie restaurarea datelor de către runner. |

## Așteptarea turei

[Așteptarea licitației și jocului](turn/ReleaseTurnWaits.java) folosește [ceasul controlat al driverului](../driver/waiting/UiStateAwaiter.java). Recomandarea folosește chiar observația care satisface condiția. Tapul caută cartea în cea mai recentă ierarhie, iar după comandă driverul citește din nou.

Dialogul de realizări poate reînnoi o singură dată termenul așteptării scorului.

[Componenta cu ADB simulat](turn/ReleaseTurnWaitsComponentTest.java) verifică ordinea comenzilor, limita de timp, ultima observație și propagarea cauzei originale. Scenariul pe APK verifică trecerea vizibilă dintre licitare, joc și scor.

## Izolarea datelor și teardown

[Contextul](ReleaseBidContext.java) cere rezervarea exclusivă curentă, același serial, identitatea APK-ului și o limită verificată a datelor înainte de orice partidă nouă. Alegeți una dintre cele două modalități; chitanțele nu pot pretinde backup și izolare secundară în aceeași rulare.

- **Backup și restaurare:** `setup-release.json` păstrează serialul, API-ul, calea APK-ului, `requestedApkSha256`, `backupSha256`, `backupScopeValidated=true` și `restoreRequired=true`; arhiva `app-data-before.tar` trebuie să fie reală, nenulă și verificată. Runnerul restaurează datele după succes sau eșec și confruntă hash-ul.
- **Utilizator Android secundar temporar:** când sistemul nu permite backupul aplicației, [scriptul de izolare](../../../../../../../../scripts/Invoke-AtfAndroidDisposableUser.ps1) creează un utilizator secundar, îl deblochează și îi extinde temporar timpul până la stingerea ecranului, confirmă că Whist Lab nu era instalat acolo și instalează APK-ul candidat numai pentru acel utilizator. Teardown-ul elimină profilul și setarea sa, iar chitanțele dovedesc starea inițială a instalării din profilul original. Datele originale nu sunt șterse; scriptul nu adaugă conturi Google. Rădăcina dovezilor este obligatoriu în afara checkout-ului, iar runnerul refuză căile din proiect. APK-ul copiat temporar pe dispozitiv este șters imediat după instalare. Salvările de profil și joc rămân numai în datele private ale utilizatorului Android temporar, eliminat la teardown; nu se exportă fișiere de profil sau joc în ATF.
- După E2E, apelați faza `Teardown` a aceluiași script chiar și când testele au eșuat. Ea revine la utilizatorul original, elimină doar utilizatorul identificat de chitanța acestei rulări și emite `data-isolation-cleanup.json`. Lipsa chitanței `PASS` blochează calificarea.
- În profilul temporar, runnerul dezactivează serviciile Google de fundal care pot bloca System UI la inițializare. Chitanța listează pachetele dezactivate. Dacă există deja un APK cu același SHA-256, runnerul îl activează pentru profilul temporar prin `install-existing`.
- `setup-release.json` și `device-lease.json` rămân în rădăcina externă a rulării. Raportul E2E include chitanța de pregătire; dosarul release păstrează ambele chitanțe, iar manifestul le leagă prin hash.
- Selecția neconfirmată trebuie păstrată după închiderea dialogului în aceeași sesiune. Contractul confirmat și starea publică trebuie păstrate după oprirea procesului și reluare.
- Lipsa emulatorului ori a limitei de izolare nu devine succes sau dovadă E2E. Un preflight neexecutat rămâne `PENDING` în dosarul candidatului.

Apelați preflight-ul comun cu rezervarea activă și profilul selectat, înainte de instalare sau backup. [Comanda și criteriile](../../../../../../../../docs/testing/ATF_ReleaseQualification.md#preflight-android)

## Execuție

Testele se selectează explicit prin `WhistReleaseBidE2eTest` și `WhistReleaseHomeE2eTest`, cu `atf.android.enabled=true`, `whist.records.runRoot`, `atf.android.artifacts` și serialul rezervat. [Suita E2E primară](../../suites/AndroidE2eSuite.java) păstrează proprietatea testelor, iar [suita componentelor](../../suites/AndroidComponentSuite.java) descoperă separat probele decodorului.

Pentru utilizator temporar, pregătiți candidatul după ce directorul de rulare există și rezerva activă corespunde serialului:

```powershell
$device = @{
    Phase='Prepare'; RunRoot=$RunRoot; Serial=$Serial; AdbExecutable=$AdbExecutable
    LeasePath=$LeasePath; LeaseId=$LeaseId; ServerSocket=$ServerSocket
    ApkPath=$CandidateApk; ApkSha256=$CandidateSha256; ExpectedApi=28
    ExpectedDisplayProfile='1080x1920@440'
}
.\scripts\Invoke-AtfAndroidDisposableUser.ps1 @device
try {
    # Run the Maven E2E selection with whist.records.runRoot=$RunRoot.
} finally {
    $device.Phase = 'Teardown'
    $device.Remove('ApkPath'); $device.Remove('ApkSha256')
    .\scripts\Invoke-AtfAndroidDisposableUser.ps1 @device
}
```

Faza `Prepare` verifică versiunea, afișajul, hash-ul candidatului, profilul activ și rezervarea înainte să creeze utilizatorul. Teardown-ul refuză ID-uri diferite, rezervări schimbate sau ștergerea utilizatorului original.

[Calificarea unui release](../../../../../../../../docs/testing/ATF_ReleaseQualification.md) deține ordinea verificărilor, capturile inline, delimitarea rapoartelor și promovarea dosarului.
