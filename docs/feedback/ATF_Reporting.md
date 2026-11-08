# Calificarea publicării și a predării rezultatelor

## Responsabilitate

Consumatorul deține înregistrările jocului, selecția, oracolul și indexarea externă. Defectele generice de API sau randare urmează [rutarea către responsabilul bibliotecilor](README.md).

## RPT-OBSERVATION-CONTRACT — validare după capabilitățile declarate

**P2 · responsabil Reporting: contractul artefactului pentru observații.** O probă de componentă poate citi imagini controlate și publica comparații reale fără HTTP, Playwright sau acțiuni pe dispozitiv. Validatorul trebuie să cunoască acest domeniu, fără introducerea unor pași fictivi pentru completarea unei secțiuni.

- Declarați separat schimburi API, acțiuni UI, observații UI și imagini de componentă; inventarul expected/actual, proprietatea capturilor și autonomia rămân obligatorii pentru domeniul selectat.
- Absența unui canal nefolosit nu este eșec al probei; lipsa unei capturi ori comparații declarate trebuie să rămână eșec explicit.
- Calificați cu API-ul public și cu un JAR fizic un raport numai cu `OBSERVE`, apoi controlați eliminarea PNG-ului, schimbarea proprietarului și alterarea rezultatului unei comparații.

[Criteriile responsabilului comun](README.md) păstrează problema generică; adaptorul nu modifică JAR-ul sau HTML-ul publicat pentru a trece validatorul.

## RPT-ANDROID-HTML-ONLY — livrabilul de audit fără copii auxiliare

**P1 · responsabil adaptor Android: colectare, publicare și predare.** [Livrarea curentă](../../release/README.md) păstrează PNG-uri canonice inline, cu viewer offline și revenire la checkpoint. Acestea califică accesul la capturi, fără să demonstreze că întregul audit poate fi înțeles numai din HTML.

Captura corpurilor și proveniența sunt contracte implementate ale [predării](../reporting/ATF_Handoff.md#livrabil-compact-și-excepții). Lacuna rămasă este calificarea livrabilului fiecărui producător, inclusiv migrarea rapoartelor istorice care păstrează date unice externe.

- [ ] Produceți un HTML autonom per execuție/selecție/suită, cu identități distincte pentru invocări; toate datele necesare auditului rămân în raport. Eliminați copiile finale ale datelor încorporate; mutarea lor într-un ZIP sau într-un director brut extern nu închide criteriul. Temporarele servesc construirii și verificării livrabilului.
- [ ] Prezentați întâi scopul, precondițiile, versiunea/build-ul și hash-ul APK, dispozitivul/API-ul, profilul, sursele și bibliotecile efectiv folosite; separați explicit identitatea capturată la execuție de cea stabilită ulterior. Pachetul aplicației nu este identitatea APK-ului.
- [ ] Delimitați în prezentare intervalul publicării de intervalele execuției capturate. Datele Started/Ended ale sesiunii HTML nu trebuie interpretate ca timpul Android atunci când durata nativă provine din alt interval; data lipsă rămâne lipsă, fără reconstrucție retrospectivă.
- [ ] Calificați asocierea corpurilor integrale cu fiecare checkpoint și migrarea istorică fără pierderea timpilor. Corpurile XML/JSON/trace sunt publicate ca text sigur și pliabil; receipt-ul central și manifestul nu substituie acceptanța tuturor producătorilor ori identitatea completă a configurației.
- [ ] Aliniați inventarul [catalogului](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactCatalog.java) cu [predarea offline](../../atf-test-android/tools/whist_ui_handoff.py): datele obligatorii ale testului sunt accesibile în HTML, iar metadatele distribuției au scop distinct. Manifestul integrității nu înlocuiește corpurile observațiilor și nu justifică o copie suplimentară a raportării componentei.
- [ ] Unificați inventarul producătorului și al predării: fiecare informație obligatorie are proprietar și corp accesibil în HTML. Pentru o excepție externă, declarați fișierul, scopul unic, consumatorul ori diagnosticul și identitatea; extensia `.log` nu justifică păstrarea. Un log de crash, profilarea sau verificarea semnăturii pot aduce valoare separată, fără să ascundă o dependență necesară înțelegerii rezultatului.
- [ ] Calificați consolidarea cu octeți lipsă/modificați, valori lungi, text care conține HTML, prefix base64 și asocieri greșite între checkpoint-uri. Întâi confruntați datele și navigarea în HTML-ul singur; apoi eliminați numai copiile demonstrate, fără ștergerea dovezii unice încă nepublicate.

Selecțiile Android ale [livrării curente](../../release/README.md) includ PNG-urile, ierarhiile și starea capturată; corpurile mari sunt împărțite ordonat în plafonul efectiv. Rezultatele JUnit nemigrate și diagnosticele cu scop separat rămân explicit delimitate; [guvernanța retenției](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) nu substituie acceptanța tuturor producătorilor ori un guard permanent al completitudinii.

**Închidere:** comparați inventarul și octeții obligatorii înainte/după publicare, apoi verificați numai HTML-ul într-un director gol și offline. Publicarea incompletă nu autorizează eliminarea singurei dovezi și nu emite PASS; istoricul migrat își păstrează originea și limitele, fără o execuție Android inventată.

## RPT-ANDROID-COMPONENT-HTML — comparații capturate, nu doar JUnit XML

**P1 · responsabil adaptor Android: calificarea fiecărui producător și a predării.** [Familiile Maven](../testing/ATF_DisplayComponents.md#familii-care-folosesc-același-audit) și [puntea Gradle](../testing/ATF_GradleComponents.md) folosesc auditul Kit și publică inputuri și comparații evaluate. Gradle păstrează intrări distincte pentru aplicație, motor, bibliotecă grafică și raster; catalogul impune clasificarea și participarea claselor la audit. Migrarea mecanismului nu recalifică execuțiile istorice și nici selecțiile neexecutate.

[Suita display](../../atf-test-android/src/test/java/atf/android/suites/AndroidDisplayComponentSuite.java), inclusiv [FrameFillAuditComponentTest](../../atf-test-android/src/test/java/atf/android/e2e/display/component/FrameFillAuditComponentTest.java), capturează comparațiile la evaluare. Familiile fără adaptor păstrează populația și verdictul JUnit; valorile necapturate ale unei execuții trecute nu se reconstruiesc retrospectiv.

- [ ] Extindeți către componentele rămase captura în timpul evaluării: input/fixture, expected independent, actual și verdict, cu grupul standard pliabil în TEST BODY. Codul testului sau JUnit PASS nu reconstruiesc ulterior valori necapturate.
- [ ] Extindeți acceptanța pilotului către fiecare familie migrată: individual și prin suită, un HTML per scope, inventar exact, invocări parametrizate distincte, FAIL/ERROR/SKIP originale și timing separat de publicare. Controlul adaugă o clasă componentă fără adaptorul de audit și cere detectarea diferenței JUnit–HTML; verificarea unui singur fișier HTML în lansator nu demonstrează această egalitate. Suportul reutilizabil rămâne în main; pilotul verifică deja eșecul, abort-ul și lipsa aserțiunilor într-o invocare înregistrată.
- [ ] Folosiți profilul neutru de componentă, cu stare/raster sintetic etichetat corect. Unit-urile rămân categorie separată; componentele fără dispozitiv nu primesc acțiuni ADB, HTTP sau capturi native fabricate.
- [ ] Revizuiți sensul rândurilor migrate: comparațiile booleene sau de excepție au mesaje declarative când valorile nu explică invariantul. Numele metodei ori linia Java nu substituie sensul fiecărei comparații în HTML; [contractul citirii](../reporting/ATF_Audit.md#citirea-unui-raport) deține regula.

**Închidere:** o comparație trecută și una negativă controlată sunt auditabile exclusiv din HTML, cu ultima valoare și ownerul corect; un caz fără comparații capturate este explicit necalificat.

[Publicarea ownerului](../../atf-test-android/src/main/java/atf/android/evidence/component/AndroidComponentRun.java) este amânată până la închiderea scope-ului și păstrează separat duratele originale. Costul memoriei la selecții mari și integrarea Jupiter paralelă cer calificare proprie înaintea extinderii; pilotul de istoric nu închide automat aceste frontiere.

## RPT-ANDROID-DETACHED-ACCEPTANCE — calificarea finalului publicat

**P1 · responsabil adaptor Android: acceptarea și guvernanța livrabilului.** Rapoartele istorice și cele curente au aceeași obligație de autonomie a datelor; mutarea sau arhivarea anexelor nu închide criteriul.

Probele livrării curente confirmă deschiderea detașată, comparațiile și corpurile integrale, inclusiv ultima parte a unei intrări mari și PNG-ul original. Rapoartele Android finale sunt republicări offline identificate, cu aceleași trace-uri, verdicturi și intervale originale; nu reprezintă execuții Android noi. [Contractul reviziei](../reporting/ATF_Audit.md#calificarea-unui-producător) delimitează ownerii efectiv inspectați; extinderea calificării și guard-urile permanente rămân deschise.

Confruntarea inventarului și a hash-urilor dovedește păstrarea identității la mutare, fără să execute accesul la probele din HTML. Calificarea portabilă trebuie să confrunte DOM-ul și corpurile efective ale exemplarului final, cu sursele auxiliare indisponibile.

- [ ] Acceptați numai fișierul final identificat prin hash, copiat singur pentru probă, cu directoarele sursă/auxiliare indisponibile și resursele externe blocate. Verificați tabelele, ultimul rând, deschiderea/închiderea și revenirea de la PNG-ul integral pe câte un owner declarat; fără Expand all pe rapoarte mari.
- [ ] Consumați invariantul neutru [REPORT-ARTIFACT-AUTONOMY-150](README.md#contractul-comun-de-autonomie), cu verificare DOM și controale pentru link XML și imagine externă; numele unui control ori lipsa resurselor numărate nu este PASS.
- [ ] Păstrați distinct execuțiile pe sisteme Android diferite, diagnosticul FAIL și replay-ul istoric. Raportul explică separat rezultatul funcțional, ERROR-ul JUnit original și republicarea; reunirea într-un HTML nu schimbă identitatea, timpii sau verdicturile.

**Închidere:** fiecare publicator și handoff refuză mutanții, iar raportul detașat permite înțelegerea scopului fără README, checkout, XML/JSON sau alte fișiere. Problemele generice ale inspectorului urmează responsabilul bibliotecilor; datele Android și guvernanța versiunii rămân aici.

## RPT-RELEASE-SUITE-COVERAGE — agregarea corectă a livrării

**P1 · responsabil adaptor și pregătirea release-ului: calificarea colecțiilor și migrarea istorică.** [Agregarea pe sursa live](../reporting/ATF_Audit.md#agregarea-componentelor-pe-sursa-live) execută intrările primare Maven, iar [task-urile Gradle](../testing/ATF_GradleComponents.md) confruntă fiecare selecție cu propriul HTML. Identitățile se numără o singură dată; unitățile, componentele și cercetarea își păstrează categoria. O nouă execuție nu primește identitatea unui APK anterior.

**Constatare curentă pentru dosarul activ:** release-ul `0.14.0` include Extent pentru cele trei suite ATF, însă cele 518 rezultate Gradle sunt păstrate doar ca JUnit XML, iar manifestul nu declară HTML-urile Extent Gradle și nici o suită E2E Extent. Puntea Gradle produce Extent în workspace, dar această capabilitate nu este legată de agregarea livrării. Dosarul rămâne o evidență istorică a ceea ce a fost publicat; nu îl „reparăm” prin copierea unui raport vechi sau prin atribuirea unei rulări noi APK-ului deja calificat.

- [ ] Extindeți captura în timpul evaluării la familiile Gradle: input, expected independent, actual și verdict. Păstrați clasificarea semantică și separarea de unități, cercetare și corpus.
- [ ] Calificați o intrare canonică pentru fiecare selecție încă nemigrată, cu invocări parametrizate distincte și refuz la omisiune, duplicare ori rezultat divergent.
- [ ] Încorporați datele originale obligatorii înaintea eliminării XML/PNG/JSON istorice. Când comparațiile nu au fost capturate, o nouă execuție primește identitate proprie; tabelul PASS din XML nu reconstruiește expected/actual și nu recalifică APK-ul vechi.
- [ ] Conectați selecțiile primare componentă și E2E la dosarul candidatului: un HTML Extent canonic prin `atf-reporting` pentru fiecare selecție rulată, egalitate exactă JUnit–HTML, sursă/versiune identificate și capturi efectiv produse inline Base64. XML-ul rămâne temporar pentru confruntare și nu se păstrează ca raport duplicat în dosarul final.
- [ ] Adăugați la manifest câte un grup auditat pentru fiecare HTML Gradle și E2E executat; eliminați numai XML-ul aceleiași populații după trecerea verificatorului și revizia autonomă. Păstrați separate suita unit, smoke-ul nativ și diagnosticele cu valoare unică.
- [ ] Guvernați legătura de la amprenta TMP la APK și commit: același inventar sursă trebuie să fie în candidatul verificat și arborele promovat în `main`; orice diferență invalidează calificarea dependentă.

**Închidere:** HTML-urile canonice permit auditul selecțiilor declarate, cu metadate justificate și proveniență distinctă. Dovezile unice nemigrate rămân identificate; eliminarea lor ori mascarea unei dependențe nu reprezintă agregare.

## Validarea timpurie a checkpoint-ului

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | [AndroidUiFlowTrace.checkpoint](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java) păstrează aserțiuni pe care [publicatorul](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java) le confruntă ulterior cu politica bibliotecii; sumarul prea lung poate fi refuzat abia la publicare. | Componentă: validați prin aceeași politică efectivă înaintea următoarei capturi, cu refuz localizat și fără trunchierea valorilor integrale din observații și XML. |

Consumatorul deține momentul validării și datele transmise. Dacă API-ul public nu expune politica necesară, contractul reutilizabil se solicită responsabilului bibliotecii; limita nu se recopiază ca o regulă locală independentă.

## Costul intrărilor consumatorului

| Prioritate | Responsabil și calificare rămasă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | Consumatorul raportării: costul selecției și compoziției la populații mari ori predominant cu eșec. | Componente cu populații controlate și mediu identificat: cost, număr de comparații și corp opțional, cu toate eșecurile, valorile obligatorii și inventarul omisiunilor păstrate. |

## RPT-EXTENT-REPEATED-PAYLOAD — compactare fără pierderea auditului

**P2 · responsabilul bibliotecii de raportare: costul HTML-ului component fără reducerea dovezilor.** Măsurarea Extent dintr-o execuție primară de componentă:

| Măsurătoare | Valoare |
|---|---:|
| Teste promovate | 257 |
| Dimensiune HTML | 25.887.708 octeți |
| Blocuri `atf-observation-body` | 2.008 |
| Text escapate în blocuri | 19.425.351 caractere |
| Duplicări byte-identice | 700 blocuri / 11.105.130 caractere repetate |
| PNG Base64 | 75 apariții / 9 payload-uri distincte |

Rezultatul componentelor a fost PASS; constatarea privește costul raportului și navigarea lui.

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
