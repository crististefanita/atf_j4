# Feedback Whist Lab

## Pe scurt

Acest registru grupează lacunele, limitele de cercetare și calificările încă necesare.
Contractele stabile aparțin ghidurilor tematice; fiecare criteriu rămâne deschis până la verificarea nivelului cerut.

## Registre active

[Dosarul activ](../../release/README.md) reunește APK-ul și dovezile versiunii calificate. Candidații intermediari rămân în staging extern; [calificarea livrării](../testing/ATF_ReleaseQualification.md) condiționează înlocuirea dosarului activ.

| Arie | Responsabilitate și criterii |
|---|---|
| [Jucători AI](ATF_Ai.md) | Inferență, estimări, cost și calificarea politicilor pe date noi. |
| [Android](ATF_Android.md) | Istoric complet, persistență, geometrie, localizare și accesibilitate. |
| [Interfață comună](ATF_CardUi_Extraction.md) | Limitele dintre prezentarea reutilizabilă și adaptorul Whist: meniu, profil, masă, istoric și animații. |
| [Browser](ATF_UiBrowser.md) | Geometria de referință și paritatea cu APK-urile pentru adaptorul comun Edge și Chrome. |
| [Raportare](ATF_Reporting.md) | Calificarea autonomiei, comparațiilor și consumului de memorie în Extent. |
| [Guvernanță](ATF_Governance.md) | API reutilizabil, perimetru explicit și coeziunea codului. |
| [Infrastructură locală](ATF_TestInfrastructure.md) | Concurența Kitului, izolarea configurației și costul validării API. |
| [Realizări Whist](ATF_Achievements.md) | Restaurarea stării ecranului Android și calibrarea cu jucători umani. |

## ToDo-uri prioritizate

Scorul 1–100 exprimă ordinea recomandată de lucru, nu calitatea sau procentul de finalizare. Evaluarea combină impactul asupra utilizatorului, mărimea golului de verificare, riscul pentru release și claritatea criteriului de închidere. Fiecare rând indică registrul normativ; scorul se revizuiește când se schimbă riscul ori dovada, iar criteriile rămân deschise până la închiderea lor acolo.

| ID | Prioritate | Scor | ToDo rămas și criteriul scurt de închidere |
|---|---:|---:|---|
| RPT-01 | P1 | 95 | Verifică fiecare producător prin HTML detașat și controale negative pentru conținut lipsă, extern ori atribuit greșit. [Registru](ATF_Reporting.md#rpt-android-html-only--livrabilul-de-audit-fără-copii-auxiliare) |
| RPT-02 | P1 | 94 | Probează pe producătorii rămași respingerea invocărilor pierdute și utilitatea comparațiilor originale, inclusiv la FAIL, ERROR și SKIP. [Registru](ATF_Reporting.md#rpt-android-component-html--comparații-capturate-nu-doar-junit-xml) |
| AND-01 | P1 | 93 | Demonstrează recuperarea checkpoint-ului după corupere sau întrerupere, păstrând datele utilizatorului ori raportând eroarea explicit. [Registru](ATF_Android.md) |
| AND-02 | P1 | 92 | Califică pe APK actorul, geometria stabilă și mâna vizibilă în licitație/joc/AutoGame pentru 3–6 locuri, RO/EN și text mărit. [Registru](ATF_Android.md) |
| AI-01 | P1 | 91 | Calibrează inferența Hard pentru 2–8 cărți din licitații și cărți jucate pe loturi noi; separă exactitatea probabilităților de scorul simulat. [Registru](ATF_Ai.md) |
| ACH-01 | P1 | 90 | Califică pe APK restaurarea poziției și selecției din Realizări, apoi reluarea notificărilor după recrearea activității. [Registru](ATF_Achievements.md) |
| AND-03 | P1 | 89 | Califică API 37, navigarea sistemului, reluarea, fonturile mărite și memoria pe un dispozitiv identificat. [Registru](ATF_Android.md) |
| RPT-03 | P1 | 88 | Publică suite mari în limita de heap declarată, fără copii repetate ale HTML-ului și fără să piardă JUnit sau comparațiile la eșec. [Registru](ATF_Reporting.md#rpt-bounded-report-heap--finalizare-sigură-pentru-suite-mari) |
| UI-01 | P1 | 86 | Califică antetul, chenarele și notificările pe APK-urile consumatorilor și verifică nativ intrarea Unicode a numelui. [Registru](ATF_CardUi_Extraction.md#priorități-și-condiții-de-închidere) |
| RAG-16 | P2 | 85 | Reluați invocările de componentă nerezolvate din checkpoint-uri verificate, păstrând comparațiile originale și paritatea completă a identităților JUnit. [Registru](ATF_Governance.md#costul-calificării-și-reluarea-invocărilor) |
| UI-02 | P2 | 84 | Calificați acțiunile de rezultat mărite și derularea pe APK-urile consumatorilor în RO/EN cu text mărit. [Registru](ATF_CardUi_Extraction.md#acceptanța-acțiunilor-de-rezultat) |
| AI-02 | P2 | 82 | Măsoară ocaziile, conversiile la 500, efectul asupra clasamentului și latența pe loturi noi, cu joc natural separat de prefixul controlat. [Registru](ATF_Ai.md#joc-la-0) |
| WEB-16 | P2 | 80 | Pregătiți versiuni cu două segmente 0.x pentru următoarele pachete Browser și Android UI, păstrând coordonatele vechi imuabile. [Registru](ATF_CardUi_Extraction.md#formatul-următoarei-versiuni-comune) |
| WEB-07 | P2 | 79 | Califică setările, reluarea, Joacă din nou și istoricul în APK și Chrome pe același Android, în RO/EN și cu text mărit. [Registru](ATF_UiBrowser.md#calificarea-pe-același-dispozitiv) |
| WEB-11 | P2 | 78 | Califică rangurile și glifele într-o mână de 17 cărți, la text mărit pe Chrome Android. [Registru](ATF_UiBrowser.md#calificarea-pe-același-dispozitiv) |
| WEB-15 | P2 | 77 | Finalizează calificarea adoptării în consumatorii rămași, păstrând hash-urile JAR-urilor de execuție/teste și adaptoarele specifice jocului. [Registru](ATF_UiBrowser.md#adoptarea-pachetului-comun) |
| AI-03 | P2 | 76 | Extrageți gestionarea și reutilizarea mostrelor din MonteCarloAi fără să schimbați fațada; fixați ordinea aleatoare, replay-ul, anularea și bugetul prin componente. [Registru](ATF_Ai.md#evoluția-motorului) |

## Responsabilitatea schimbării

```mermaid
flowchart LR
    ISSUE[Lacună identificată] -->|atribuie| OWNER[Responsabil]
    OWNER -->|definește| CRITERION[Criteriu și nivel]
    CRITERION -->|verifică| CHECK[Rezultat reproductibil]
    CHECK -->|promovează regula în| CONTRACT[Documentul responsabil]
```

<details>
<summary>Legendă: închiderea unui criteriu</summary>

Un criteriu numește comportamentul, sursa și nivelul de verificare necesar. Regula stabilă se documentează la responsabil, iar rezultatele concrete și materialele de investigație rămân externe.

</details>

Core, Reporting, Console și Mock sunt consumate ca perechi binare versionate JAR/POM. Acest proiect deține verificarea contractelor pe care le folosește, proveniența perechii, integrarea locală și documentarea oricărei limitări observate; hash-ul și coordonatele sunt sursa de identitate.
[Ghidul editorial Whist](../governance/ATF_Documentation.md) deține regulile documentare și verificarea lor în checkout.

## Închiderea și promovarea

- Păstrați distincte defectul demonstrat, calificarea lipsă și ipoteza de cercetare.
- Verificați cazul nominal, refuzul relevant și identitatea surselor înainte de închidere.
- Promovați regula stabilă în ghidul său și eliminați numai criteriul demonstrat.
- Fiecare lucrare deschisă are un singur registru responsabil; README-urile explică mecanismul și trimit la acel criteriu.
- Păstrați controalele tehnice active până la reconcilierea lor explicită.
- Actualizați toate referințele la consolidare; o schimbare de director nu dovedește rezolvarea comportamentului.

## Contractul comun de autonomie

Rapoartele trebuie să se deschidă și să poată fi auditate fără runner, JAR adițional sau fișiere auxiliare. HTML-ul păstrează contextul, comparațiile și capturile inline; consumatorul verifică acest contract în [feedback-ul raportării](ATF_Reporting.md).

## Suportul comun al testelor

Kitul este consumat după perechea JAR/POM fixată. Testele locale verifică integrarea, lifecycle-ul, concurența folosită și propagarea cauzelor; rezultatele unei biblioteci nu înlocuiesc calificarea adaptorului Android sau a suitei de produs.
