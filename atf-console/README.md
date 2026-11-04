# Proiecția consolei

## Pe scurt

Modulul proiectează evenimentele neutre de execuție în consola configurată.
Mesajele sunt diagnostice; o eroare de afișare nu schimbă verdictul testului.
Nu deține logica jocului sau un renderer HTML.

## Componente

| Sursă | Responsabilitate |
|---|---|
| [AtfConsoleReporter](src/main/java/atf/console/AtfConsoleReporter.java) | Primește evenimente și aplică setările de afișare. |
| [AtfConsoleFormatter](src/main/java/atf/console/AtfConsoleFormatter.java) | Formatarea textului pentru consolă. |
| [AtfConsoleConfigurationLoader](src/main/java/atf/console/AtfConsoleConfigurationLoader.java) | Încărcarea configurației și valorile implicite. |
| [AtfConsoleTestExtension](src/main/java/atf/console/junit/AtfConsoleTestExtension.java) | Lifecycle JUnit izolat pe invocare și restaurarea contextului anterior. |
| [AtfConsoleTestContext](src/main/java/atf/console/junit/AtfConsoleTestContext.java) | Contextul explicit, inclusiv pentru copiii testelor dinamice. |

Politica mesajelor de lifecycle și notificarea căii finale sunt opțiuni
independente. Extensia rezolvă separat aceste politici; dezactivarea mesajelor
nu trebuie interpretată drept o publicare reușită sau un rezultat business.

O metodă TestFactory este un container. Copiii dinamici își deschid propriul
lifecycle prin API-ul contextului; containerul nu este numărat ca rezultat al
fiecărui copil. Configurarea comună se consumă prin
[POM-ul părinte](../pom.xml), fără versiuni copiate în acest overview.

## Verificare

Rulați dintr-o copie proprie din `%TEMP%`, cu POM-ul părinte și dependențele
locale ale reactorului:

```powershell
mvn -B -pl atf-console -am test
```

[POM-ul modulului](pom.xml) și [sursele testelor](src/test/java/) definesc
dependențele și contractele efective. Aceste comenzi nu sunt dovada unei execuții
deja trecute.
