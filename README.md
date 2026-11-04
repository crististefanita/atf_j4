# Whist Lab și testarea Android

Whist Lab este un joc Android cu motor Java independent de interfață. Proiectul
include aplicația, jucători automați și instrumentele pentru verificarea regulilor,
a licitațiilor și a scorului prin teste de componentă și partide complete.

Contract Whist (`com.qsoft.android.whist`) este aplicația de referință studiată.
Whist Lab (`ro.whistlab.app`) are propriul motor, propriile salvări și propriile
reguli configurabile; rezultatele unei aplicații nu certifică automat cealaltă.

## Jocul

[Instalare APK 0.1.5](dist/WhistLab-0.1.5-debug.apk) · [Verificarea jocului pe mai multe ecrane](docs/WHIST_RESPONSIVE_UI_SANITY.md)

Profilul principal este Whist românesc, patru jucători, cu programul 1–8–1.
Jucătorul urmează culoarea cerută; când nu o are, joacă atu dacă are atu.
Motorul aplică regulile variantei selectate, inclusiv pachetul, ordinea
licitațiilor, restricția ultimei licitații și calculul scorului.

Un contract românesc exact primește `5 + levate`; abaterea se penalizează cu
numărul levatelor în plus sau în minus. Bonusul pentru o serie de cinci mâini eligibile
reușite se alege înaintea partidei: **5 sau 10**, implicit **10**; seria de
cinci ratări se penalizează cu **−5** când această opțiune este activă.

Interfața și instrucțiunile sunt disponibile în română și engleză.

Jocul are două niveluri. **Normal** urmărește levatele licitate și evită să treacă peste contract. **Hard** evaluează șansa contractului din informația publică, apoi poate căuta ratări la adversari când limitele jocului îi arată că propriul contract a devenit imposibil.

[Regulile și limitele variantelor](docs/CONTRACT_WHIST_RULE_RECONSTRUCTION.md) explică diferențele de profil.

[Motorul jocului](whist-lab-app/game-core/README.md) leagă comportamentele de implementare și teste.

Doar mâinile de 2–8 cărți avansează seria. La o carte, un rezultat de același tip păstrează contorul, iar rezultatul opus îl întrerupe.

## Organizarea surselor

| Director | Responsabilitate |
|---|---|
| `whist-lab-app/app` | Aplicația Android: configurare, masă, interacțiuni, salvare și localizare. |
| `whist-lab-app/game-core` | Reguli, distribuire, licitare, levate, scor și politica de joc. |
| `whist-lab-app/algorithm-build` | Build JVM al motorului pentru lucru offline, fără SDK Android. |
| `whist-lab-app/ai-lab` | Experimente și evaluator independent; nu este inclus în aplicație. |
| `atf-android` | Adaptor ADB și captură de dovezi Android. |
| `atf-test-android` | Teste Android, oracole independente și scenarii complete. |
| `atf-test-kit`, `atf-mock`, `atf-validation`, `atf-console` | Suportul comun al testelor și al execuției. |
| `atf-tests` | Verificări ale configurației și ale integrării bibliotecilor. |
| `config`, `scripts` | Colecții, reguli declarative, selecție de partide și comenzi de lucru. |
| `lib` | Biblioteci binare; versiunile efective sunt în POM, iar proveniența în `library-provenance.json`. |

Aplicația se construiește cu Gradle; infrastructura de testare folosește Maven.
Sunt două build-uri distincte, cu responsabilități și dependențe diferite.

## Început rapid offline

Sunt necesare Java 21, PowerShell și Node.js pentru verificatoarele JSON.
SDK-ul Android este necesar pentru aplicație și testele Android; verificările
algoritmului au și un build JVM separat.

Din rădăcina proiectului, o verificare mică a regulilor și a contextelor publice:

```powershell
.\scripts\run-whist-collection.ps1 -Collection context
.\scripts\run-whist-collection.ps1 -Collection quick
```

Pentru regresia completă a componentelor:

```powershell
.\scripts\run-whist-collection.ps1 -Collection regression
```

Runnerul pregătește sursele într-un director temporar și păstrează rezultatele
în depozitul extern de dovezi. Aceste colecții nu comandă emulatorul.
Parametrii și delimitarea colecțiilor sunt în
[ghidul testelor](docs/TEST_COLLECTIONS.md).

## Cum dezvoltăm jucătorii

Pornim de la partide salvate și extragem situații mici cu intrări publice.
O mutare devine regresie numai după justificarea așteptării printr-o regulă,
un invariant sau un oracol finit; o înfrângere singură nu dovedește o greșeală.

Comparăm un candidat cu o politică fixată pe aceleași distribuții și locuri,
apoi îl verificăm pe partide nefolosite la ajustare. Păstrăm și rezultatele
negative; un candidat fără progres demonstrat nu înlocuiește politica acceptată.

Datele complete ale adversarilor sunt disponibile numai pentru auditul de după partidă.

Jucătorul primește propria mână și informația publică; cărțile ascunse nu intră în API-ul de decizie.

[Corpusul de partide](docs/AI_CORPUS.md), [selecția pentru învățare](docs/AI_LEARNING_FOCUS.md) și [catalogul regulilor AI](docs/rules/AI_HAND_PATTERNS_RO.md) descriu fluxul.

Aceste evaluări măsoară comportamente pe domenii declarate, fără garanția că Hard va câștiga orice partidă cu informație ascunsă.

## Lucrul pe Android

Testele pe dispozitiv urmează verificărilor offline și cer un dispozitiv
identificat explicit, conectivitate ADB și acces exclusiv pe durata scenariului.
[Ghidul Android](docs/ANDROID_TESTING.md) descrie pregătirea și comenzile;
[aplicația](whist-lab-app/README.md) descrie build-ul și salvările.

Sursele rămân în proiect, build-urile intermediare în directoare temporare,
iar partidele și celelalte dovezi în depozitul extern configurat. Nu se
suprascrie o execuție închisă pentru a o transforma în rezultat curent.

## Documentație

[Indexul documentației](docs/README.md) pornește de la arhitectură, reguli,
testare și contractele de date. Modificările de comportament trebuie însoțite
de actualizarea ghidului relevant și de o verificare potrivită acelui comportament.
