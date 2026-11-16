# Adaptor browser Card Table

## Pe scurt

Modulul servește o masă locală în browser, cu motorul JVM al jocului. Whist este adaptorul inclus. Rentz și Hold’em furnizează adaptoarele lor și folosesc același server, același afișaj și aceleași controale web.

| Strat | Responsabilitate |
|---|---|
| [Contractul jocului](src/main/java/ro/cardtable/browser/BrowserGame.java) | Publică locurile, cărțile permise, comenzile legale și punctajele. Validarea rămâne în motorul fiecărui joc. |
| [Serverul](src/main/java/ro/cardtable/browser/BrowserServer.java) | Servește resursele din aceeași origine. Izolează partidele pe filă, validează reviziile și respinge comenzile repetate. |
| [Geometria](src/main/java/ro/cardtable/browser/BrowserGeometry.java) | Aplică planul Java folosit în Android pentru locuri și evantai. Pe telefon măsoară viewportul util. Pe desktop fixează cadrul logic la 414 × 896, calculează scara după înălțimea utilă și centrează. |
| [Adaptorul Whist](whist/src/main/java/ro/whistlab/browser/WhistBrowser.java) | Folosește distribuirea, politicile AI și decontarea din motor. Recomandarea vine din aceeași selecție umană ca pe Android. |

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

`lib/atf-ui-browser-0.1.0.jar` conține serverul, contractele Java, geometria partajată și resursele DOM. Rentz și Hold’em compilează numai sursele adaptoarelor proprii față de JAR și verifică suma SHA-256 la pornire. Arhiva `atf-ui-browser-0.1.0-tests.jar` conține probele comune de HTTP, control al reviziilor și sanity Chrome sau Edge.

```powershell
.\atf-ui-browser\Package-CardTableBrowser.ps1 -WorkRoot $WorkRoot -Version 0.1.0
```

Generarea cere JDK 21 și scrie ambele arhive cu amprentele lor în directorul extern afișat. La o schimbare a contractelor sau a resurselor, incrementați versiunea și actualizați aceiași octeți și aceleași amprente în consumatori. Reutilizați JAR-ul existent când se schimbă numai motorul sau adaptorul unui joc.

## Verificare

Folosiți `-BuildOnly` la cele trei lansatoare și treceți valorile de classpath returnate către scriptul de componente. Rularea folosește procese Java și browsere headless cu profiluri separate. `$EvidenceRoot` indică un director extern al rulării.

```powershell
node .\atf-ui-browser\tests\run-components.mjs $EvidenceRoot $WhistClasses $RentzClasses $HoldemClasses $Edge $Chrome
java -cp $WhistClasses .\atf-ui-browser\tests\WhistParity.java > (Join-Path $EvidenceRoot 'whist-parity.json')
.\atf-ui-browser\tests\Publish-BrowserExtent.ps1 -EvidenceRoot $EvidenceRoot -MavenClasspathFile $MavenClasspath
```

`$MavenClasspath` este lista de dependențe produsă de poarta Maven de [componente](../docs/testing/README.md). Raportul leagă comparațiile și imaginile inline de rularea browserului. [Criteriile de paritate încă deschise](../docs/feedback/ATF_UiBrowser.md)

| Nivel | Responsabilitate |
|---|---|
| Componentă | Acoperă validarea Unicode, persistența profilului, catalogul, progresul idempotent, geometria și destinația animației. Starea așteptată vine din motor și din catalogul Android. |
| Adaptor UI | Deschide controalele prin etichete, confirmă rezultatul observabil și atașează capturi. Pe Android, compară APK și Chrome pe același dispozitiv și păstrează profilul, densitatea și SHA-ul APK-ului. |
| Raportare | Primește rezultatele și capturile după execuție. O observație pe telefon intră ca `paired-android.json`, o listă de cazuri cu rezultat, așteptare, observație și fișiere PNG din același dosar. |
