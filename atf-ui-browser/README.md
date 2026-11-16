# Adaptor browser Card Table

## Pe scurt

Modulul servește o masă locală în browser, cu motorul JVM al jocului. Whist este adaptorul inclus. Rentz și Hold’em furnizează adaptoarele lor și folosesc același server, același afișaj și aceleași controale web.

| Strat | Responsabilitate |
|---|---|
| [Contractul jocului](src/main/java/ro/cardtable/browser/BrowserGame.java) | Publică locurile, cărțile permise, comenzile legale și punctajele. Validarea rămâne în motorul fiecărui joc. |
| [Serverul](src/main/java/ro/cardtable/browser/BrowserServer.java) | Servește resursele din aceeași origine. Izolează partidele pe filă, validează reviziile și respinge comenzile repetate. |
| [Geometria](src/main/java/ro/cardtable/browser/BrowserGeometry.java) | Aplică planul Java folosit în Android pentru locuri și evantai. Pe telefon măsoară viewportul util. Pe desktop fixează cadrul logic la 414 × 896, calculează scara după înălțimea utilă și centrează. |
| [Adaptorul Whist](whist/src/main/java/ro/whistlab/browser/WhistBrowser.java) | Folosește distribuirea, politicile AI și decontarea din motor. Recomandarea vine din aceeași selecție umană ca pe Android. |

## Whist pe GitHub Pages

Ediția statică distribuie motorul Java compilat cu TeaVM într-un Web Worker. Masa trimite aceleași comenzi și citește aceeași proiecție publică prin mesaje locale. După încărcarea resurselor, licitarea, AutoGame și decontarea nu fac cereri către un API.

| Contract preluat din Android | Aplicare în browser |
|---|---|
| [Tipografia antetului](../whist-lab-app/app/src/main/java/ro/whistlab/app/theme/GameHeaderTypography.java) și [paleta](../whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/theme/TableColors.java) | Adaptorul transmite rolurile textului și culorile. DOM-ul le aplică antetului, meniului și mesei. |
| [Rezervarea mesei](../whist-lab-app/app/src/main/java/ro/whistlab/app/table/TableLayoutMetrics.java) | Măsoară fereastra și păstrează același centru rezervat pentru toate mâinile variantei. |
| [Controalele de licitare](../whist-lab-app/app/src/main/java/ro/whistlab/app/bidding/BidGridMetrics.java) | Cifra și confirmarea păstrează ținta tactilă de 48 de unități logice. Conturul este retras în interior, iar numărul de coloane și rândul derulabil vin din politica APK. |
| [Ritmul mutărilor](../whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/turn/TurnPacing.java) | Fiecare carte automată apare după întârzierea definită de APK. Adaptorul redă actorul, cartea și colectarea în ordinea produsă de motor. |

[Contractul mesei](../whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/TableGeometry.java) primește dimensiunile măsurate și rezervarea fazei. Android aplică dreptunghiurile în ViewGroup, iar web le aplică în DOM.
[Curba de colectare](../whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/animation/TrickCollectionMotion.java) evaluează progresul pe Android și furnizează în web cadrele pe care browserul le interpolează.

| Date | Retenție în ediția statică |
|---|---|
| Limba | Opțiunea Română sau English se salvează în browser. Meniul, regulile, starea mesei și realizările folosesc textele din APK. |
| Profil, Realizări și clasamente | Browserul salvează numele, progresul și clasamentele în aceeași înregistrare locală cu partida. Clasamentele folosesc topul, filtrul după configurație și marcajele AI și Exemplu din APK. |
| Partidă | Meniul permite reluarea după redeschidere. Browserul salvează checkpointul la fiecare comandă acceptată și folosește aceeași validare ca APK-ul. |
| Datele site-ului | Ștergerea stocării site-ului sau schimbarea browserului pornește un profil nou. Dacă stocarea este dezactivată, sesiunea folosește memoria filei. |

- [Codec-ul checkpointului](../whist-lab-app/app/src/main/java/ro/whistlab/app/persistence/GameCheckpointCodec.java) reface distribuirea, levatele și istoricul la ultima comandă acceptată. Identitatea partidei rămâne stabilă și împiedică duplicarea rezultatelor. Un checkpoint deteriorat poate fi eliminat independent de profil și progres.
- Web Locks serializează scrierile între filele compatibile. O filă cu o revizie depășită recitește checkpointul înainte de următoarea comandă. Dacă browserul refuză scrierea, partida poate continua în memorie și afișează eroarea de stocare.
- Istoricul afișează toate mâinile și totalul după fiecare mână în coloana P. O diagonală roșie marchează licitația ratată. În coloana P, un chenar marchează bonusul de serie, iar un chenar roșu marchează penalizarea.
- Setările folosesc [aceleași valori implicite ca APK-ul](../whist-lab-app/app/src/main/java/ro/whistlab/app/screens/MatchSetup.java). Schimbarea limbii păstrează opțiunile de pe ecran, iar dialogul de reguli arată programul variantei pentru numărul selectat de jucători.
- În meniul jocului, scorul curent deschide istoricul când există mâini încheiate. La ultimul scor, **Joacă din nou** păstrează configurația și înlocuiește partida locală cu o distribuire și un identificator noi.
- Chenarele mesei preiau numele, licitația și levatele din proiecția APK. Marcajul auriu arată cărțile jucabile. Totalul apare în scor și istoric.
- [Adaptorul mâinii](src/main/java/ro/cardtable/browser/BrowserHandAdapter.java) trimite gesturile la modelul comun folosit în APK. Refuzul evidențiază cartea apăsată și opțiunile legale timp de 1.100 ms. La alegerea unei cărți legale, depunerea urmează ridicării de 150 ms. API-ul proiectează aceeași curbă și aceleași straturi ale evantaiului ca în APK.
- Fața compactă a cărții depuse și copia ei din colectare folosesc aceleași culori, contur și tipografie ca în adaptorul Android. Textul accesibil include jucătorul și cartea.
- Colectarea aplică temporizarea din modulul comun. Levata completă și numărătoarea actualizată a câștigătorului sunt în evenimentul de colectare. Mâna se recentrează imediat după depunere, iar masa stabilă conține cărțile din levata curentă. Redimensionarea poate înlătura desenul temporar, iar următoarea carte respectă intervalul deja programat.
- AutoGame se activează pentru mâna curentă. Debifarea anulează următoarea decizie în locul jucătorului. Mutările acceptate se încheie, iar meniurile și dialogurile suspendă următoarea decizie cât timp sunt deschise. Scorul sau revenirea la meniul principal sting bifa.

- Buildul cere PowerShell 7, JDK 21, Maven și mediul de generare a catalogului. `$WorkRoot` și `$EvidenceRoot` indică directoare externe.
- Proba Node.js verifică inventarul public și joacă prin motorul compilat. Pentru gesturi și redeschiderea profilului, rulați [scenariile Playwright](#verificare).

```powershell
$Site = .\atf-ui-browser\Build-WhistPages.ps1 -WorkRoot $WorkRoot
node .\atf-ui-browser\pages\verify.mjs $Site $EvidenceRoot
```

- Pentru modificările de licitare, AutoGame, limbă sau realizări, rulați `node .\atf-ui-browser\pages\verify-controls.mjs $Site $EvidenceRoot`. Proba folosește motorul compilat și înlocuiește temporizatoarele și stocarea browserului în cazurile controlate.
- Adăugați `--ui-only` pentru geometrie, ritmul mutărilor și parcursul vizual, când inventarul pachetului și partida completă au dovezi valabile.
- Pentru istoricul scorului și compatibilitatea checkpointului, generați datele legale cu [WhistRecordsFixture](pages/WhistRecordsFixture.java) și rulați [verificarea Pages](pages/verify-records.mjs) cu directorul site, dosarul de dovezi și fișierul JSON al datelor. Fixture-ul compilează cu classpath-ul lansatorului JVM și `ScoredHandFixture` din testele aplicației.
- Pentru limbă, setări, navigare și Joacă din nou, rulați `node .\atf-ui-browser\pages\verify-navigation.mjs $Site $EvidenceRoot`. Proba produce și un checkpoint local pentru verificarea vizuală a reluării de la scorul final.
- Pentru marcajul cărților, ordinea levatelor și redimensionare, rulați `node .\atf-ui-browser\pages\verify-effects.mjs $Site $EvidenceRoot`.
- Pentru reacția la apăsarea cărții, ridicare și fața compactă, rulați `node .\atf-ui-browser\pages\verify-card-adapter.mjs $Site $EvidenceRoot`. Proba și testul JVM al modulului UI consumă același [scenariu de gesturi](../whist-lab-app/atf-card-table-ui/src/test/resources/hand-feedback.tsv).
- Folosiți `--contract-only` pentru a verifica doar geometria pe faze și mostrele curbei din artefactul compilat. Pentru această selecție, omiteți executabilul browserului.

Publicați conținutul directorului `$Site` la rădăcina ramurii `gh-pages`. Configurați GitHub Pages să publice acea ramură de la rădăcină. Verificați dintr-o sesiune anonimă pagina, resursele și pornirea unei mâini înainte de actualizarea linkului din README. [Publicarea dintr-o ramură](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site)

Artefactele distribuției statice se află pe ramura de publicare. Fișierul [dependențelor TeaVM](pages/teavm-dependencies.xml) servește buildului Pages independent de reactorul Maven ATF.

## Pornire locală

Rulați comanda din proiect cu PowerShell 7, JDK 21 și mediul Gradle Android configurat pentru generarea catalogului. `$WorkRoot` indică directorul extern pentru build și datele profilului. Deschideți adresa completă din terminal în Edge sau Chrome.

```powershell
.\atf-ui-browser\Start-WhistBrowser.ps1 -WorkRoot $WorkRoot
```

Pentru un telefon conectat la rețeaua de test, `-ListenAddress $LocalLanAddress` selectează adresa locală accesibilă. Deschideți adresa completă în browserul telefonului.

## Datele de pe telefon

| Date | Unde se păstrează |
|---|---|
| Nume și identificator | Pe dispozitiv, pentru aceeași origine web, definită prin protocol, gazdă și port. Într-o filă nouă, Whist recuperează profilul. Copia browserului poate reface numele dacă fișierul său lipsește de pe server. |
| Realizări | În `$WorkRoot/whist-profile-data`, asociate identificatorului și creditate din rezultatele confirmate de motor. |
| Partidă | Independentă pe filă. Aceeași filă poate relua partida prin reîncărcare cât timp rulează procesul serverului. |

- Ștergerea datelor site-ului, schimbarea browserului sau schimbarea originii creează un profil nou. În modul privat, retenția urmează politica browserului.
- Whist folosește sarcina Android de generare a catalogului și dependența `org.json:json:20240205`. `-JsonLibrary` selectează JAR-ul din alt director, iar `-CatalogSource` refolosește ieșirea generată din același checkout.

## Bibliotecă pentru adaptoare

`lib/atf-ui-browser-0.2.0.jar` conține serverul, contractele Java, geometria partajată și resursele DOM. Rentz și Hold’em compilează numai sursele adaptoarelor proprii față de JAR și verifică suma SHA-256 la pornire. Arhiva `atf-ui-browser-0.2.0-tests.jar` conține probele comune de HTTP și revizii, verificarea ridicării și a colectării din JAR, scenariul de atingere partajat cu APK-ul și sanity Chrome sau Edge.

```powershell
.\atf-ui-browser\Package-CardTableBrowser.ps1 -WorkRoot $WorkRoot -Version 0.2.0
```

Generarea cere JDK 21 și scrie ambele arhive cu amprentele lor în directorul extern afișat. La o schimbare a contractelor sau a resurselor, incrementați versiunea și actualizați aceiași octeți și aceleași amprente în consumatori. Reutilizați JAR-ul existent când se schimbă numai motorul sau adaptorul unui joc.

## Verificare

- Rangurile 2–10, J, Q, K și A folosesc aceeași țintă de mărire în APK și Web. Fiecare adaptor încadrează rangurile în fețele micșorate cu aproximativ 25%.
- Lățimea indicelui vizibil stabilește suprapunerea în evantai, inclusiv pentru `10` și pentru mâinile de 17 cărți.
- Butonul este singurul îndemn vizual al jucătorului la licitarea manuală.

Folosiți `-BuildOnly` la cele trei lansatoare și treceți valorile de classpath returnate către suita de componente. Procesele JVM servesc adaptoarele pe loopback, iar suita trimite cereri HTTP și verifică starea publică. `$EvidenceRoot` indică un director extern, existent și dedicat rulării.

```powershell
node .\atf-ui-browser\tests\run-components.mjs $EvidenceRoot $WhistClasses $RentzClasses $HoldemClasses
java -cp $WhistClasses .\atf-ui-browser\tests\WhistParity.java > (Join-Path $EvidenceRoot 'whist-parity.json')
```

Pentru UI, pregătiți ediția Pages în `$Site` și generați `hand-fixture.json` cu proba `verify-card-adapter.mjs`. Rularea folosește Playwright Java cu Chromium headless, locatori după rol și câte un context izolat pentru fiecare scenariu. [Locatori și așteptări Playwright](https://playwright.dev/docs/locators)

`-BrowserExecutable` permite alegerea unei instalări Chrome sau Edge. Chromium compatibil trebuie să fie disponibil atunci când parametrul este omis.

```powershell
node .\atf-ui-browser\pages\verify-card-adapter.mjs $Site $AdapterEvidence
.\atf-ui-browser\tests\Run-BrowserUi.ps1 -Site $Site -HandFixture (Join-Path $AdapterEvidence 'hand-fixture.json') -EvidenceRoot $EvidenceRoot
.\atf-ui-browser\tests\Publish-BrowserExtent.ps1 -EvidenceRoot $EvidenceRoot -MavenClasspathFile $MavenClasspath
```

`$MavenClasspath` este lista de dependențe produsă de poarta Maven de [componente](../docs/testing/README.md). Raportul leagă comparațiile și imaginile inline de rularea browserului. [Criteriile de paritate încă deschise](../docs/feedback/ATF_UiBrowser.md)

| Nivel | Responsabilitate |
|---|---|
| Componentă | Verifică reviziile, refuzurile, geometria, profilul și decontarea prin contractul HTTP sau motorul Pages, cu stocare și temporizare controlate. |
| UI Web | Verifică în Chromium indicele `10` și accesul la ultima carte într-o mână de 17 cărți, inclusiv cu text la 200%. Verifică și licitarea de la tastatură, atingerea respinsă, AutoGame și profilul după reîncărcare. Scrie `browser-ui-checks.json` și capturile în directorul rulării. |
| Comparație pe Android | Folosește APK și Chrome pentru aspecte legate de sistem, inclusiv fonturile, gesturile native și zonele rezervate. Înregistrează viewportul, densitatea și SHA-ul APK-ului în `paired-android.json`. |
| Raportare | Extent citește rezultatele fiecărui nivel și atașează capturile PNG inline. |
