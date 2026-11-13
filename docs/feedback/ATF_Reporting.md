# Calificarea publicării și a predării rezultatelor

## Responsabilitate

Consumatorul deține înregistrările jocului, selecția, oracolul și indexarea externă. Defectele generice de API sau randare urmează [rutarea către responsabilul bibliotecilor](README.md).

## RPT-OBSERVATION-CONTRACT — validare după capabilitățile declarate

**P2 · integrarea adaptorului.** Profilurile explicite acoperă deja rapoarte neutre fără HTTP. Asocierea unei capturi OBSERVE cu acțiunea și ownerul declarați urmează contractul comun; feedback-ul local păstrează calificarea adaptorului și a datelor consumate.

- [ ] După livrarea capabilității comune, calificați adaptorul real și raportul produs cu biblioteca efectiv consumată. Verificați proprietarul capturii, inventarul expected/actual și refuzul la eliminarea capturii declarate.

## RPT-ANDROID-HTML-ONLY — livrabilul de audit fără copii auxiliare

**P1 · responsabil adaptor Android.** Pentru fiecare producător modificat, inventariați corpurile și observațiile obligatorii. Dosarele de componente au deja HTML per selecție, imagini inline și proveniență. Criteriul rămas privește datele fiecărui producător, inclusiv observațiile native și datele unice istorice. [Contractul predării](../reporting/ATF_Handoff.md#livrabil-compact-și-excepții)

- Confruntați inventarul, timpii și corpurile fiecărui checkpoint cu capturile originale. Injectați conținut lipsă, modificat, extern sau atribuit greșit. Verificați text lung, conținut asemănător HTML și Base64.
- Identificați fiecare excepție externă prin fișier, scop și consumator. Păstrați datele unice până la calificarea migrării. Un crash log sau verificarea semnăturii poate avea scop separat de auditul testului.
- Deschideți separat HTML-ul final identificat prin SHA-256, cu sursele auxiliare și rețeaua indisponibile. Verificați navigarea, ultimul rând, tabelele și revenirea de la imaginea integrală. Injectați un link XML și o imagine externă. [Contractul auditului detașat](../reporting/ATF_Audit.md#calificarea-unui-producător)

Publicarea incompletă păstrează diagnosticul și materialul unic. Replay-ul istoric păstrează identitatea, timpii și verdictul original. [Guvernanța retenției](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java)

## RPT-ANDROID-COMPONENT-HTML — comparații capturate, nu doar JUnit XML

**P1 · responsabil adaptor Android: calificarea fiecărui producător și a predării.** [Familiile Maven](../testing/ATF_DisplayComponents.md#familii-care-folosesc-același-audit) și [puntea Gradle](../testing/ATF_GradleComponents.md) folosesc auditul Kit și publică inputuri și comparații evaluate. Gradle păstrează intrări distincte pentru aplicație, motor, bibliotecă grafică și raster; catalogul impune clasificarea și participarea claselor la audit. Migrarea mecanismului nu recalifică execuțiile istorice și nici selecțiile neexecutate.

[Suita display](../../atf-test-android/src/test/java/atf/android/suites/AndroidDisplayComponentSuite.java), inclusiv [FrameFillAuditComponentTest](../../atf-test-android/src/test/java/atf/android/e2e/display/component/FrameFillAuditComponentTest.java), capturează comparațiile la evaluare. Familiile fără adaptor păstrează populația și verdictul JUnit; valorile necapturate ale unei execuții trecute nu se reconstruiesc retrospectiv.

- [ ] Identificați prin inventar producătorii care mai pot pierde o invocare sau o comparație la publicare. Proba injectează o componentă fără adaptor, o invocare parametrizată pierdută și câte un rezultat FAIL, ERROR și SKIP. Diagnosticul indică invocarea exactă și diferența dintre JUnit și HTML. [Verificatorul Gradle](../testing/ATF_GradleComponents.md)
- [ ] Revizuiți sensul rândurilor migrate: comparațiile booleene sau de excepție au mesaje declarative când valorile nu explică invariantul. Numele metodei ori linia Java nu substituie sensul fiecărei comparații în HTML; [contractul citirii](../reporting/ATF_Audit.md#citirea-unui-raport) deține regula.

**Închidere:** o comparație trecută și una negativă controlată sunt auditabile exclusiv din HTML, cu ultima valoare și ownerul corect; un caz fără comparații capturate este explicit necalificat.

[Publicarea ownerului](../../atf-test-android/src/main/java/atf/android/evidence/component/AndroidComponentRun.java) este amânată până la închiderea scope-ului și păstrează separat duratele originale. Costul memoriei la selecții mari și integrarea Jupiter paralelă cer calificare proprie înaintea extinderii; pilotul de istoric nu închide automat aceste frontiere.

## RPT-RELEASE-SUITE-COVERAGE — agregarea corectă a livrării

**P1 · responsabil adaptor și pregătirea release-ului.** Lansatoarele Maven și Gradle confruntă selecțiile rulate cu propriul HTML. Calificați predarea pentru selecțiile Gradle și E2E care încă nu au o execuție demonstrată în dosarul candidatului. [Agregarea componentelor](../reporting/ATF_Audit.md#agregarea-componentelor-pe-sursa-live)

- Confruntați invocările, identitatea sursei, receipt-ul și capturile inline. La reutilizare, verificați hash-ul și proveniența execuției originale. [Identitatea dovezii RAG](ATF_Release_RAG.md#identitatea-surselor-și-a-dosarului)
- Pentru datele istorice nemigrate, inventariați conținutul unic. O execuție nouă primește propria identitate, iar [criteriul comparațiilor](#rpt-android-component-html--comparații-capturate-nu-doar-junit-xml) rămâne deschis pentru valorile care nu au fost capturate.

## Validarea timpurie a checkpoint-ului

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | [AndroidUiFlowTrace.checkpoint](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java) păstrează aserțiuni pe care [publicatorul](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java) le confruntă ulterior cu politica bibliotecii; sumarul prea lung poate fi refuzat abia la publicare. | Componentă: validați prin aceeași politică efectivă înaintea următoarei capturi, cu refuz localizat și fără trunchierea valorilor integrale din observații și XML. |

Consumatorul deține momentul validării și datele transmise. Dacă API-ul public nu expune politica necesară, contractul reutilizabil se solicită responsabilului bibliotecii; limita nu se recopiază ca o regulă locală independentă.

## Costul intrărilor consumatorului

| Prioritate | Responsabil și calificare rămasă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | Consumatorul raportării: costul selecției și compoziției la populații mari ori predominant cu eșec. | Componente cu populații controlate și mediu identificat: cost, număr de comparații și corp opțional, cu toate eșecurile, valorile obligatorii și inventarul omisiunilor păstrate. |

## RPT-BOUNDED-REPORT-HEAP — finalizare sigură pentru suite mari

**P1 · responsabil biblioteci: publicarea și verificarea HTML; consumatorul: dimensiunea intrărilor.** O suită poate avea teste JUnit verzi și totuși să nu producă raportul canonic dacă injecția navigării ori verificarea DOM epuizează heap-ul. Eroarea de publicare nu trebuie să ascundă rezultatul testelor sau să lase un raport parțial prezentat ca acceptat.

- [ ] Măsurați separat memoria de vârf pentru randarea inițială, injectarea navigării și verificarea raportului, în funcție de numărul comparațiilor, corpurilor și capturilor inline.
- [ ] Evitați copiile integrale repetate ale documentului în etapele de publicare și verificare; fiecare etapă trebuie să poată procesa un raport mare valid sub limita de heap declarată.
- [ ] Calificați limitele cu rapoarte sintetice mici, medii și mari, inclusiv payload-uri repetate și imagini inline. Verificați identitățile JUnit–HTML, toate capturile și octeții obligatorii după publicare.
- [ ] La depășirea unei limite explicite, emiteți un diagnostic localizat care identifică etapa și păstrează rezultatul JUnit și dovezile disponibile; nu lăsați o excepție de heap să se transforme într-o lipsă ambiguă de fișier.

Calificarea este comună producătorului și verificatorului: mărirea heap-ului unui singur proces nu închide criteriul și nu demonstrează că raportul rămâne publicabil în mediul standard.

## RPT-EXTENT-REPEATED-PAYLOAD — compactare fără pierderea auditului

**P2 · compactarea HTML-ului de componentă fără reducerea dovezilor.** Payload-urile identice pot crește dimensiunea și timpul de navigare al raportului. Profilarea exactă aparține dosarului extern al execuției; criteriul urmărește mecanismul de compactare și păstrarea auditabilității. Capabilitățile reutilizabile urmează [rutarea comună](README.md#responsabilitatea-schimbării).

- [ ] Profilul compact trebuie să păstreze fiecare owner, comparație, verdict și corp integral accesibil offline în același HTML. Nu eliminați valori și nu mutați conținutul în ZIP, JSON ori fișiere externe.
- [ ] Evaluați referințe interne content-addressed pentru corpuri byte-identice repetate. Fiecare invocare trebuie să afișeze conținutul integral corect; conținutul poate fi stocat o singură dată în HTML autonom.
- [ ] Păstrați capturile PNG inline Base64 și navigarea către checkpoint-ul original; optimizarea textului nu trebuie să altereze identitatea capturii sau asocierea ownerului.
- [ ] Expuneți un sumar de dimensiune pe categorie și o opțiune de prezentare configurabilă. Preview-ul se poate scurta, dar corpul integral și criteriul PASS/FAIL rămân verificabile.
- [ ] Calificați HTML-ul singur într-un director gol, cu payload-uri repetate, lungi, diferite și Base64. Confruntați byte-for-byte fiecare valoare vizibilă și verificați hash-ul raportului final.

Măsurarea justifică investigarea reducerii redundanței; nu dovedește că toate corpuri similare au același sens. Referințele se pot unifica numai pentru payload-uri identice, iar verdictul nu poate depinde de modul compact.

## Contractele responsabile

- [Predarea rezultatelor](../reporting/ATF_Handoff.md) deține proveniența republicării, idempotența înregistrării și pointerele distincte pentru încercare și acceptanță.
- [Auditul publicării](../reporting/ATF_Audit.md#calificarea-unui-producător) deține calificarea fiecărui producător, comparațiile originale, ultima valoare, capturile și controalele negative.
- [Contractul Android](../reporting/ATF_Android.md) deține asocierea acțiune–observație–comparație și distincția dintre rezultatul funcțional și publicare.

[Contractul Android](../reporting/ATF_Android.md) · [Auditul publicării](../reporting/ATF_Audit.md) · [Retenție](../learning/ATF_Retention.md)
