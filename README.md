# Whist Lab și testarea Android

## Pe scurt

Whist Lab este un joc Android cu motor Java independent de interfață. Include
jucători automați, verificarea regulilor și a scorului prin teste de componentă,
evaluări offline și scenarii UI proprii. Experimentele compară politici pe distribuții controlate și observații publice.

[Bibliotecile comune](lib/README.md) au un responsabil comun al codului și feedback-ului; aici păstrăm adaptoarele și acceptanța domeniului.

## Jocul

[Descarcă APK-ul din ultima versiune publicată](https://github.com/crististefanita/atf_j4/releases/latest). Alege fișierul `.apk` din **Assets**; mărimea și SHA-256 sunt publicate împreună cu el și în [registrul verificării](artifacts/reports/README.md).

[Livrare și instalare](docs/platform/ATF_AndroidDelivery.md) · [Verificarea jocului pe mai multe ecrane](docs/testing/ATF_Display.md)

Profilul principal este Whist românesc, patru jucători, cu programul 1–8–1.
Jucătorul urmează culoarea cerută; când nu o are, joacă atu dacă are atu.
Motorul aplică regulile variantei selectate, inclusiv pachetul, ordinea
licitațiilor, restricția ultimei licitații și calculul scorului.

Un contract românesc exact primește `5 + levate`; abaterea se penalizează cu
numărul levatelor în plus sau în minus. Bonusul pentru o serie de cinci mâini eligibile
reușite se alege înaintea partidei: **5 sau 10**, implicit **10**; seria de
cinci ratări se penalizează cu valoarea în oglindă: **−5 sau −10**.

- Interfața și instrucțiunile sunt disponibile în română și engleză.
- Numele omului se configurează din meniul inițial; adversarii primesc nume scurte aleatorii la fiecare partidă.
- [Istoricul comun](docs/game/ATF_ScoreHistory.md) și [clasamentele locale](docs/game/ATF_PlayerRecords.md) păstrează separat evoluția partidei și cele mai bune rezultate.

Jocul are două niveluri. **Normal** urmărește levatele licitate și evită să treacă peste contract. **Hard** evaluează șansa contractului din informația publică, apoi poate căuta ratări la adversari când limitele jocului îi arată că propriul contract a devenit imposibil.

[Regulile și limitele variantelor](docs/game/README.md) explică diferențele de profil.

[Motorul jocului](whist-lab-app/game-core/README.md) leagă comportamentele de implementare și teste.

Doar mâinile de 2–8 cărți avansează seria. La o carte, un rezultat de același tip păstrează contorul, iar rezultatul opus îl întrerupe.

## Arhitectura

```mermaid
flowchart LR
APP[Android] -->|consumă| CORE[Motor Java]
OFF[Simulare offline] -->|verifică| CORE
OFF -->|compară| IND[Politică independentă]
GOV[Guvernanță] -->|controlează| OFF
```

<details>
<summary>Legendă și limite</summary>

Motorul deține regulile și tranzițiile. Aplicația deține interacțiunea, iar mediul offline controlează comparațiile fără a transmite mâinile adverse politicilor. Guvernanța condiționează promovarea, fără actualizare automată în timpul jocului.

</details>

## Organizarea surselor

| Director | Responsabilitate |
|---|---|
| `whist-lab-app/app` | Aplicația Android: configurare, masă, interacțiuni, salvare și localizare. |
| `whist-lab-app/game-core` | Reguli, distribuire, licitare, levate, scor și politica de joc. |
| `whist-lab-app/algorithm-build` | Construirea JVM a motorului pentru lucru offline, fără SDK Android. |
| `whist-lab-app/ai-lab` | Modul JVM: jucător independent, unelte și experimente offline; dependență doar pentru teste. |
| `atf-test-android` | Adaptor ADB, captură și temporizare Android, audituri reutilizabile, componente și E2E Whist Lab. |
| `atf-test-kit`, `atf-validation` | Suportul comun al testelor, configurării și validării. |
| `atf-tests` | Contracte ale adaptoarelor, integrarea bibliotecilor și guvernanța calității. |
| `config`, `scripts` | Configurație declarativă și lansatoare comune; uneltele de domeniu sunt în modulele responsabile. |
| `lib` | Biblioteci binare; versiunile efective sunt în POM, iar proveniența în `library-provenance.json`. |

Aplicația se construiește cu Gradle; infrastructura de testare folosește Maven.
Sunt două procese de construire distincte, cu responsabilități și dependențe diferite.

## Început rapid offline

Sunt necesare Java 21, PowerShell 7 (`pwsh`) și Node.js pentru verificatoarele JSON.
SDK-ul Android este necesar pentru aplicație și testele Android; verificările
algoritmului au și o intrare JVM separată.

Din rădăcina proiectului, o verificare mică a regulilor și a contextelor publice:

```powershell
.\scripts\run-whist-collection.ps1 -Collection context
.\scripts\run-whist-collection.ps1 -Collection quick
```

Pentru regresia completă a componentelor:

```powershell
.\scripts\run-whist-collection.ps1 -Collection regression
```

Lansatorul pregătește sursele într-un director temporar și păstrează rezultatele
în depozitul extern de dovezi. Aceste colecții nu comandă emulatorul.
Parametrii și delimitarea colecțiilor sunt în
[catalogul colecțiilor](docs/testing/ATF_Collections.md).

[Arhitectura testării](docs/testing/ATF_Architecture.md) explică nivelurile unitar, componentă, integrare și E2E, cu o matrice verificată față de cod. [Guvernanța calității](docs/governance/README.md) descrie verificările surselor și documentației.

## Cum dezvoltăm jucătorii

Pornim de la partide salvate și extragem situații mici cu intrări publice.
O mutare devine regresie numai după justificarea așteptării printr-o regulă,
un invariant sau un oracol finit; o înfrângere singură nu dovedește o greșeală.

Comparăm un candidat cu o politică fixată pe aceleași distribuții și locuri,
apoi îl verificăm pe partide nefolosite la ajustare. Păstrăm și rezultatele
negative; un candidat fără progres demonstrat nu înlocuiește politica acceptată.

Datele complete ale adversarilor sunt disponibile numai pentru auditul de după partidă.

Jucătorul primește propria mână și informația publică; cărțile ascunse nu intră
în API-ul de decizie. În experimentele pereche, fiecare profil joacă din nou
aceleași distribuiri la fiecare loc; după prima licitație sau carte diferită,
starea publică evoluează firesc pe linia acelui profil.

[Arhitectura învățării](docs/ai/README.md), [modelul deciziei](docs/ai/ATF_DecisionModel.md), [regulile și ponderile](docs/ai/ATF_RulesConfiguration.md), [corpusul de partide](docs/learning/ATF_Corpus.md), [selecția pentru învățare](docs/learning/ATF_Selection.md) și [catalogul regulilor AI](docs/ai/ATF_HandPatternsRo.md) descriu fluxul.

Aceste evaluări măsoară comportamente pe domenii declarate, fără garanția că Hard va câștiga orice partidă cu informație ascunsă.

## Lucrul pe Android

Testele pe dispozitiv urmează verificărilor offline și cer un dispozitiv
identificat explicit, conectivitate ADB și acces exclusiv pe durata scenariului.
[Ghidul Android](docs/testing/ATF_Android.md) descrie pregătirea și comenzile;
[aplicația](whist-lab-app/README.md) descrie procesul de construire și salvările.

Sursele rămân în proiect, compilările intermediare în directoare temporare,
iar partidele și celelalte dovezi în depozitul extern configurat. Nu se
suprascrie o execuție închisă pentru a o transforma în rezultat curent.

## Documentație

[Indexul documentației](docs/README.md) pornește de la arhitectură, reguli,
testare și contractele de date. Modificările de comportament trebuie însoțite
de actualizarea ghidului relevant și de o verificare potrivită acelui comportament.

## Biblioteci comune

Reactorul păstrează modulele locale de testare și adaptoarele Android. [Contractul bibliotecilor comune](docs/platform/README.md) explică perechile binare din `lib`, identitatea lor și limitele acceptanței locale.
