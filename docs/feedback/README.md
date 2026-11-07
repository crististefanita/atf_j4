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
| [Interfață comună](ATF_CardUi_Extraction.md) | Refactorizarea meniului, profilului și prezentării mesei pentru consumul Rentz, cu interfața și contractele Whist păstrate. |
| [Publicare](ATF_Reporting.md) | HTML autonom, componente auditabile și calificarea publicării. |
| [Guvernanță](ATF_Governance.md) | API reutilizabil, perimetru explicit și coeziunea codului. |
| [Infrastructură locală](ATF_TestInfrastructure.md) | Concurența Kitului, izolarea configurației și costul validării API. |

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

Core, Reporting, Console și Mock au [responsabilul comun al bibliotecilor](../../../atf_j0/docs/feedback/README.md).
Principiile editoriale se aliniază cu [bunele practici documentare comune](../../../atf_j0/docs/README_Atf_DocumentationBestPractices.md), adaptate la domeniul Whist și formalizate în [ghidul local](../governance/ATF_Documentation.md).

## Închiderea și promovarea

- Păstrați distincte defectul demonstrat, calificarea lipsă și ipoteza de cercetare.
- Verificați cazul nominal, refuzul relevant și identitatea surselor înainte de închidere.
- Promovați regula stabilă în ghidul său și eliminați numai criteriul demonstrat.
- Fiecare lucrare deschisă are un singur registru responsabil; README-urile explică mecanismul și trimit la acel criteriu.
- Păstrați controalele tehnice active până la reconcilierea lor explicită.
- Actualizați toate referințele la consolidare; o schimbare de director nu dovedește rezolvarea comportamentului.

## Contractul comun de autonomie

[Criteriul comun de autonomie a raportului](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-artifact-autonomy-150--calificare-fără-dependențe-externe) aparține responsabilului bibliotecilor. [Feedback-ul consumatorului Android](ATF_Reporting.md) păstrează colectarea, publicarea și calificarea livrabilului propriu.

## Suportul comun al testelor

[Primitivele și concurența Kitului](../../../atf_j0/docs/feedback/ATF_Common_Test_Support_Feedback.md#kit-concurrency-152--calificare-jupiter-paralelă) au responsabil comun. Consumatorul folosește perechea binară declarată în POM, iar acceptanța adaptorului Android rămâne locală; transferul responsabilității nu constituie calificarea paralelismului.
