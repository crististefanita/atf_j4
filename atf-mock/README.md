# Transport API mock

## Pe scurt

Modulul implementează transport API determinist pentru verificări offline.
Catalogul de rute aparține mock-ului; el nu schimbă setările transportului real
și nu implementează regulile jocului.

## Configurare și selecție

[AtfMockTransport](src/main/java/atf/mock/AtfMockTransport.java) returnează răspunsul
rutei potrivite sau fallback-ul declarat.
[AtfMockConfigurationLoader](src/main/java/atf/mock/AtfMockConfigurationLoader.java)
acceptă o cale explicită și definește descoperirea fișierelor YAML. Prioritatea
configurației și valorile implicite se citesc din acest loader.

[MockRoute](src/main/java/atf/mock/MockRoute.java) definește potrivirea metodei,
căii, parametrilor, headerelor și corpului. Fallback-ul este un rezultat
configurat, nu dovada că s-a apelat un serviciu extern.

## JSON strict și potrivire numerică exactă

Catalogul și request-ul sunt documente complete. Trailing content, rădăcini
multiple și duplicate nested sunt refuzate; scalarul null este JSON valid.
Un expected absent în YAML nu echivalează cu textul JSON null, iar obiectele
sau array-urile YAML nu sunt convertite tacit în absența constrângerii.

| Potrivire | Contract verificat |
|---|---|
| precizie | JSON și scalarul numeric YAML folosesc BigDecimal, fără narrowing prin Double |
| valori distincte | vecinii peste 2^53, fracțiile lungi, exponenții extremi și underflow-ul ajung la fallback |
| nominale | formele decimal egale și zerourile rămân eligibile; integer/decimal și număr/string păstrează distincțiile |
| wire | body-ul și răspunsul configurat nu sunt rescrise |

[Contractele de componentă](src/test/java/atf/mock/json/MockNumericContractComponentTest.java)
folosesc API-ul public, expected independent și transportul mock. Ele verifică
fallback-ul, răspunsul original și încărcarea YAML, fără serviciu extern.

## Verificare

Rulați dintr-o copie proprie din `%TEMP%`, cu POM-ul părinte și dependențele
locale ale reactorului:

```powershell
mvn -B -pl atf-mock -am test
```

[POM-ul modulului](pom.xml) și [sursele testelor](src/test/java/) definesc
dependențele și contractele efective. Aceste comenzi nu sunt dovada unei execuții
deja trecute.
