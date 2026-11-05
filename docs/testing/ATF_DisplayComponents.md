# Display verificat prin componente

## Responsabilitate

Colecția offline verifică politicile de dimensionare, decodarea observației Android și oracolele geometrice sau raster. Intrările simulate sunt explicite; acceptanța versiunii instalate păstrează o verificare nativă restrânsă.

## Arhitectură

```mermaid
flowchart LR
    MOCK[XML controlat] -->|furnizează| DECODE[Decodor de observații]
    DEVICE[XML capturat nativ] -->|furnizează| DECODE
    DECODE -->|proiectează| SNAPSHOT[Snapshot geometric]
    SNAPSHOT -->|confruntă| ORACLE[Oracol independent]
    ORACLE -->|explică| RESULT[Comparații evaluate]
```

<details>
<summary>Legendă: aceeași cale de verificare pentru date simulate și capturate</summary>

Fixture-ul descrie regiuni măsurate, fără să apeleze algoritmul de layout al aplicației. Decodorul consumă un singur XML; E2E deține capturarea, iar componenta deține interpretarea acelei observații.

</details>

## Ce se mută în componente

| Nivel | Intrare controlată | Proprietate verificată | Proprietar |
|---|---|---|---|
| Politici ale aplicației | Lățime, înălțime, densitate, font, număr de jucători și cărți | Distribuirea regiunilor, dimensiuni minime, spațiu disponibil, rotunjire și insets calculate | `app.table`, `app.hand`, `app.window` |
| Nume și acțiune | Înălțimi de text măsurate sau simulate, conținutul chenarului și locul egalizat | Acțiune completă deasupra numelui, spațiu rezervat între ture, chenar mărit fără scalare repetată | `app.seat` |
| Istoric | Număr de locuri și densitate | Aceleași frontiere pentru antete și subcoloane, separatoare între jucători, lățime completă inclusiv padding | `app.score` |
| Decodare | XML și profil de afișaj | Asociere semantică, fețe de carte, cadre, titlu, atu și fereastră de conținut | [TableViewportObservation](../../atf-test-android/src/test/java/atf/android/e2e/display/support/TableViewportObservation.java) |
| Oracol geometric | Snapshot cu defecte injectate | Număr, completitudine, centrare, încadrare și absența suprapunerilor | [TableViewportAudit](../../atf-test-android/src/main/java/atf/android/whist/evidence/TableViewportAudit.java) |
| Oracol raster | Imagine sintetică și coordonate logice | Schimbare de scară, culoare dominantă, contrast și toleranțe | [Componente display](../../atf-test-android/src/test/java/atf/android/e2e/display/component/) |

## Contractul mock-ului

- [Ierarhia sintetică](../../atf-test-android/src/test/java/atf/android/e2e/display/component/SyntheticTableHierarchy.java) păstrează o bază validă și modifică explicit regiunea care trebuie respinsă.
- [Matricea parametrizată](../../atf-test-android/src/test/java/atf/android/e2e/display/component/TableViewportObservationComponentTest.java) combină profile și înălțimi măsurate pentru scale de text diferite; nu randează fonturi Android.
- Transportul simulat aruncă eroare la orice comandă sau captură. Un apel accidental la dispozitiv eșuează în componentă.
- Prezența unui nod nu dovedește vizibilitatea lui; regiunile fără suprafață și elementele din afara ferestrei sunt respinse.
- Fereastra folosește `android:id/content` când este disponibil; capturile fără această regiune au fallback explicit la profilul declarat.
- Decodarea completă a fixture-urilor folosește semnalele EN, iar verificarea acțiunii are contracte explicite RO/EN. Schimbarea limbii nu permite prescurtarea etichetei sau pierderea asocierii cu jucătorul activ.
- [PlayerSeatMetrics](../../whist-lab-app/app/src/main/java/ro/whistlab/app/seat/PlayerSeatMetrics.java) primește înălțimea completă a acțiunii; Android o măsoară cu textul real, iar componenta injectează extinderi pentru text mărit și mai multe rânduri. Modelul nu limitează numărul rândurilor și nu deduce lizibilitatea din prezența textului în XML.
- Verificarea acțiunii din [decodor](../../atf-test-android/src/test/java/atf/android/e2e/display/support/TableViewportObservation.java) este comună observațiilor simulate și checkpoint-urilor E2E de joc și licitare. Controalele negative elimină acțiunea, îi schimbă textul sau o mută peste nume; pauza are explicit zero acțiuni active.
- Distanța până la jucător se măsoară față de reuniunea chenarului și a acțiunii lui. Un spațiu liber până la chenar nu permite unui control să acopere eticheta aflată deasupra.
- Dimensiunile declarate ale atuului și fețelor jucate au comparații în pixeli și toleranță de rotunjire. O față mai înaltă decât cea cerută nu trece doar pentru că depășește un prag minim.

## Colecții și comenzi

| Colecție | Precondiții | Rezultat |
|---|---|---|
| `AndroidDisplayComponentSuite` | Java 21, Maven și dependențe deja disponibile | JUnit XML și sumar `JVM_COMPONENTS`, fără ADB sau SDK Android. |
| `whist.displayComponents=true` | Java, Gradle și SDK Android pentru compilare | Componentele JVM ale aplicației; fără instalare sau emulator. |

Din rădăcina proiectului:

```powershell
pwsh -NoProfile -File .\scripts\run-whist-responsive-sanity.ps1 -ComponentsOnly
```

Lansatorul pregătește o copie temporară și un repository Maven privat. `-MavenRepository` indică sursa cache-ului; JAR-urile ATF sunt instalate și confruntate în copia execuției, fără înlocuirea celor citite de alt proces.

Regresia concurentă păstrează un JAR deschis pentru citire în cache-ul sursă și cere bootstrap reușit în copia privată, cu sursa intactă. Hash-urile bibliotecilor rezolvate trebuie să coincidă cu cele din copia surselor; un eșec de bootstrap rămâne distinct de verdictul interfeței.

Dintr-o copie temporară a aplicației, cu dependențele pregătite:

```powershell
.\gradlew.bat :app:testDebugUnitTest -Pwhist.displayComponents=true --offline --no-daemon
```

## Ce păstrează verificarea nativă

| Proprietate | Verificare rămasă pe Android |
|---|---|
| Font și glyph | Măsurare reală, fallback, text mărit, trunchiere și lizibilitatea capturii. |
| Fereastră | Consumul efectiv al insets, tastatură, decupaj și navigarea sistemului pe API-ul țintă. |
| Interacțiune | Tap acceptat, focus, hit testing, animație și tranziția observată. |
| Compoziție | Aceeași stare conține toate regiunile necesare în rasterul real. |

O modificare de formulă rulează întâi componentele; o schimbare de randare sau de integrare cu sistemul adaugă [sanity-ul nativ selectat](ATF_Display.md). Rezultatele sunt externe și identifică sursele verificate.
