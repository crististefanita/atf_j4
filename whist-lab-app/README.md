# Whist Lab — Android și iOS

## Pe scurt

Aplicația Android Whist Lab rulează local cu motorul Java comun și adversari AI.
Proiectul nativ iOS este în calificare; interfața SwiftUI și integrarea aceluiași
motor se află în directorul `../ios`.

## Folosire

Configurarea permite alegerea variantei, a unei mese de 3–6 jucători și a nivelului Normal sau Hard.

În configurarea în română, Hard apare ca „Dificil”. Limba EN/RO se schimbă din selectorul interfeței și se păstrează între porniri. Regulile și programul mâinilor sunt disponibile din `Rules & schedule` / `Reguli și program`.

Variantele disponibile sunt definite în catalogul de reguli.

O salvare cu o variantă retrasă rămâne pe dispozitiv până când utilizatorul pornește o partidă nouă. Tipurile de reguli sunt definite în [GameMode](game-core/src/main/java/ro/whistlab/core/rules/GameMode.java), iar textele și etichetele efective în [WhistUiText](app/src/main/java/ro/whistlab/app/localization/WhistUiText.java). Regulile afișate se construiesc în [HowToPlay](game-core/src/main/java/ro/whistlab/core/rules/HowToPlay.java) pentru varianta, limba și numărul de jucători selectate.

- [Regulile jocului](../docs/game/README.md) dețin programul mâinilor, atuul, scorul și seriile pentru fiecare profil.
- Istoricul se deschide din `Meniu joc → Scorul partidei → Istoricul scorului`. [Contractul istoricului](../docs/game/ATF_ScoreHistory.md) explică licitația, totalul fiecărui jucător, marcajele și adaptarea la fereastră.

## Licitarea asistată

La tura jucătorului, motorul Hard estimează și preselectează o licitație legală din informația vizibilă. Alegerea se poate schimba și trebuie confirmată prin `Licitează` / `Place bid`; recomandarea nu depune contractul. Nivelul Normal sau Dificil selectat pentru partidă continuă să controleze adversarii.

- Calculul rulează în fundal și folosește mâna proprie, atuul și cartea întoarsă cunoscută, licitațiile publice și contextul de scor al motorului.
- Alegerea manuală are prioritate; un rezultat întârziat nu înlocuiește selecția și nu se aplică altei mâini ori partide.
- Dacă estimarea nu este disponibilă, se poate alege manual orice valoare legală. [Selecția contextuală](app/src/main/java/ro/whistlab/app/bidding/HumanBidSelection.java) păstrează această stare separat de contractele depuse.

## Joacă automat mâna curentă

- Pe Android, bifa `Auto play` / `Automat` preia temporar licitarea și jocul cărților. [Documentația activității Android](app/src/main/java/ro/whistlab/app/README.md#autogame-pentru-mâna-activă) descrie coordonarea fluxului.
- Bifa stă în dreapta titlului când încape. La text mărit, poate ocupa propriul rând în antet, deasupra atuului. Numărul mâinii și rezumatul licitațiilor apar dedesubt.
- Cât timp automatizarea este activă, `Game menu` / `Meniu joc` oferă `Stop auto-play` / `Oprește jocul automat`.
- Aplicația folosește nivelul Normal sau Hard ales pentru adversari și controlează temporar și locul uman. La ecranul scorului automatizarea se oprește, iar următoarea mână revine la controlul manual.

Automatizarea afectează numai tura de licitare și turele de joc ale mâinii active. Nu schimbă nivelul AI, regulile, scorul sau salvarea partidei. Deschiderea unui dialog, trecerea aplicației în fundal și revenirea suspendă și reiau deciziile prin coordonatorul AI existent.

## Flux și responsabilități

```mermaid
flowchart LR
UI[Android sau SwiftUI] -->|trimite comenzi| SESSION[GameSession]
SESSION -->|produce| VIEW[Stare capturată]
VIEW -->|alimentează| UI
SESSION -->|solicită decizii| AI[AiPolicy]
STORE[Salvare locală] -->|restaurează| SESSION
IOS[J2ObjC] -->|traduce motorul comun| SESSION
```

<details>
<summary>Legendă și limite</summary>

GameSession validează comenzile înaintea schimbării stării; UI-ul redă starea capturată. Persistența restaurează numai informația disponibilă în schema salvării.

</details>

## Organizare

| Componentă | Responsabilitate |
|---|---|
| [game-core](game-core/README.md) | Reguli, sesiune, scor, salvare și politica AI a aplicației; teste JVM. |
| [atf-card-table-ui](atf-card-table-ui/README.md) | Nucleu Android reutilizabil: meniu/profil, widgeturi, cărți, temă și geometria mâinii; fără motorul jocului sau biblioteci ATF. |
| [card-table-names](card-table-names/README.md) | Politica Java comună pentru repartizarea numelor pe locuri și validarea numelui uman. |
| [app](app/src/main/java/ro/whistlab/app/) | Activitatea Android, configurarea mesei, prezentarea jocului și persistența locală. |
| [ai-lab](ai-lab/README.md) | Modul JVM al jucătorului independent, uneltelor și experimentelor offline; consumat numai la testare. |
| [test-audit](test-audit/README.md) | Fixture-uri JVM pentru captura și verificarea componentelor; exclus din runtime-ul aplicației. |
| [algorithm-build](algorithm-build/README.md) | Intrare Gradle exclusiv JVM, fără configurarea aplicației Android. |
| [iOS](../ios/README.md) | Proiect SwiftUI, traducerea motorului comun și calificarea pe simulator Apple. |
| [atf-test-android](../atf-test-android/README.md) | Componente de verificare și scenarii Android activate explicit. |

Versiunea aplicației, SDK-urile și identificatorul pachetului au ca sursă
[configurația Gradle a aplicației](app/build.gradle). Un APK construit din sursele curente și un
APK instalat anterior au proveniențe distincte; versiunea sursei nu dovedește
ce binar rulează pe dispozitiv. [Contractul acceptanței Android](../docs/testing/ATF_Android.md) definește verificarea binarului identificat.

## Construire și verificare

| Scop | Intrare | Resurse |
|---|---|---|
| Regresie JVM rapidă | `:game-core:quickRegressionTest` prin `algorithm-build` | JDK și cache Gradle/JVM; fără dispozitiv Android. |
| APK de dezvoltare | `:app:assembleDebug` | JDK, SDK Android și dependențele construirii. |
| Colecție cu identitate și rezultat extern | [run-whist-collection.ps1](../scripts/run-whist-collection.ps1) | Configurația și precondițiile [colecției](../docs/testing/ATF_Collections.md). |

Executați compilările într-o copie de lucru proprie din `%TEMP%`. Din acea copie
a directorului `whist-lab-app`:

```powershell
.\gradlew.bat -p algorithm-build :game-core:quickRegressionTest --offline --no-daemon
.\gradlew.bat :app:assembleDebug --offline --no-daemon
```

Prima comandă necesită JDK și dependențele JVM disponibile local. A doua cere și
SDK-ul Android declarat în configurație; APK-ul este produs în `app/build/outputs/apk`
din copia de lucru. `--offline` refuză dependențele care nu sunt deja în cache.

Din rădăcina proiectului, [lansatorul colecțiilor](../scripts/run-whist-collection.ps1)
creează propria copie temporară și acceptă selecții explicite precum `quick`,
`regression`, `context` și `documentation`:

```powershell
.\scripts\run-whist-collection.ps1 -Collection quick
```

Testele componente, turneele offline și scenariile Android sunt selecții
distincte. Candidații experimentali externi nu schimbă aplicația activă prin
simpla lor existență. Accesul la un dispozitiv și instalarea unui APK sunt pași
separați, activați explicit.

Contractele și mecanismele curente sunt în [documentație](../docs/).

## Contracte de integrare

[Coordonarea Android](app/src/main/java/ro/whistlab/app/README.md#coordonarea-turelor-automate) separă calculul AI de firul principal și condiționează aplicarea de identitatea partidei și a stării capturate. Componentele verifică anularea și aplicarea unică; persistența, reactivitatea și interacțiunea se califică separat pe binarul identificat.

| Suprafață | Invariantă |
|---|---|
| Profil nou și salvări compatibile | Configurarea, scorul, restaurarea, proiecțiile AI și ajutorul RO/EN folosesc aceeași identitate a regulilor; profilul istoric își păstrează explicit valorile. |
| Ghid și interfață | Etichetele și accesul la istoricul tuturor locurilor urmează textele și comenzile curente; procedurile complete aparțin ghidurilor responsabile. |

[Regulile și scorul](../docs/game/README.md) dețin contractul produsului, iar [calificările Android](../docs/feedback/ATF_Android.md) păstrează criteriile încă deschise.
