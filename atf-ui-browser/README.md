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

Rulați comanda din proiect cu PowerShell 7 și JDK 21 disponibil prin `PATH`. `$WorkRoot` indică un director extern unde se pot păstra clasele compilate. Deschideți adresa completă afișată în terminal în Edge sau Chrome.

```powershell
.\atf-ui-browser\Start-WhistBrowser.ps1 -WorkRoot $WorkRoot
```

Pentru un telefon conectat la aceeași rețea de test, `-ListenAddress $LocalLanAddress` selectează adresa locală accesibilă de pe telefon. Linkul conține cheia temporară în fragment, iar aplicația o transmite numai propriului server. O filă își poate relua partida cât timp rulează serverul.

## Verificare

Folosiți `-BuildOnly` la cele trei lansatoare și treceți directoarele de clase returnate către scriptul de componente. Rularea folosește procese Java și browsere headless cu profiluri separate. `$EvidenceRoot` indică un director extern al rulării.

```powershell
node .\atf-ui-browser\tests\run-components.mjs $EvidenceRoot $WhistClasses $RentzClasses $HoldemClasses $Edge $Chrome
java -cp $WhistClasses .\atf-ui-browser\tests\WhistParity.java > (Join-Path $EvidenceRoot 'whist-parity.json')
.\atf-ui-browser\tests\Publish-BrowserExtent.ps1 -EvidenceRoot $EvidenceRoot -MavenClasspathFile $MavenClasspath
```

`$MavenClasspath` este lista de dependențe produsă de poarta Maven de [componente](../docs/testing/README.md). Raportul leagă comparațiile și imaginile inline de rularea browserului. [Criteriile de paritate încă deschise](../docs/feedback/ATF_UiBrowser.md)
