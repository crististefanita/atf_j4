# Calificarea publicării și a predării rezultatelor

## Responsabilitate

Consumatorul deține înregistrările jocului, selecția, oracolul și indexarea externă. Defectele generice de API sau randare urmează [rutarea către responsabilul bibliotecilor](README.md).

## RPT-ANDROID-HTML-ONLY — livrabilul de audit fără copii auxiliare

**P1 · responsabil adaptor Android: colectare, publicare și predare.** [Livrarea curentă](../../artifacts/README.md) păstrează PNG-uri canonice inline, cu viewer offline și revenire la checkpoint. Acestea califică accesul la capturi, fără să demonstreze că întregul audit poate fi înțeles numai din HTML.

- **Producător:** [publicatorul](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java) încorporează PNG-ul și corpurile text furnizate ori declarate de checkpoint-uri, inclusiv manifestul. Fiecare corp și fingerprint provin din aceeași citire; inputul lipsă, binar nesuportat ori UTF-8 invalid este refuzat înaintea sesiunii.
- **Identitate:** receipt-ul separă pachetul de identitatea APK-ului; scenariile Records cer hash-ul observat egal cu cel cerut, înaintea comenzilor Android.

Această schimbare a producătorului nu îmbogățește retrospectiv rapoartele native istorice, care păstrează încă anexe unice. Versiunea, configurația și proveniența completă cer confruntarea inventarului fiecărui producător; contractele cu fixture-uri nu devin acceptanță Android nouă.

- [ ] Produceți un HTML autonom per execuție/selecție/suită, cu identități distincte pentru invocări; toate datele necesare auditului rămân în raport. Eliminați copiile finale ale datelor încorporate; mutarea lor într-un ZIP sau într-un director brut extern nu închide criteriul. Temporarele servesc construirii și verificării livrabilului.
- [ ] Prezentați întâi scopul, precondițiile, versiunea/build-ul și hash-ul APK, dispozitivul/API-ul, profilul, sursele și bibliotecile efectiv folosite; separați explicit identitatea capturată la execuție de cea stabilită ulterior. Pachetul aplicației nu este identitatea APK-ului.
- [ ] Delimitați în prezentare intervalul publicării de intervalele execuției capturate. Datele Started/Ended ale sesiunii HTML nu trebuie interpretate ca timpul Android atunci când durata nativă provine din alt interval; data lipsă rămâne lipsă, fără reconstrucție retrospectivă.
- [ ] Calificați asocierea corpurilor integrale cu fiecare checkpoint și migrarea istorică fără pierderea timpilor. Corpurile XML/JSON/trace sunt publicate ca text sigur și pliabil; receipt-ul central și manifestul nu substituie acceptanța tuturor producătorilor ori identitatea completă a configurației.
- [ ] Refolosiți observațiile, corpurile tipizate și PNG-urile canonice Reporting; valorile obligatorii și cadrele complete supraviețuiesc prezentării AUTO/SUMMARY/FULL, cu preview 240. Hash-ul confirmă identitatea datelor deja accesibile în HTML.
- [ ] Aliniați [catalogul versiunilor](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactCatalog.java), [predarea offline](../../atf-test-android/tools/whist_ui_handoff.py) și contractele livrării/retenției: catalogul cere astăzi JSON separat, iar predarea copiază HTML și documente auxiliare. Păstrați un singur exemplar canonic; indicii indică același exemplar.
- [ ] Unificați inventarul producătorului și al predării: fiecare informație obligatorie are proprietar și corp accesibil în HTML. Pentru o excepție externă, declarați fișierul, scopul unic, consumatorul ori diagnosticul și identitatea; extensia `.log` nu justifică păstrarea. Un log de crash, profilarea sau verificarea semnăturii pot aduce valoare separată, fără să ascundă o dependență necesară înțelegerii rezultatului.
- [ ] Calificați consolidarea cu octeți lipsă/modificați, valori lungi, text care conține HTML, prefix base64 și asocieri greșite între checkpoint-uri. Întâi confruntați datele și navigarea în HTML-ul singur; apoi eliminați numai copiile demonstrate, fără ștergerea dovezii unice încă nepublicate.

Revizia inventarului curent distinge PNG-urile identice deja încorporate de ierarhiile și starea Android încă externe, rezultatele JUnit fără comparații HTML și diagnosticele cu scop separat. [Guvernanța retenției](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) refuză reapariția copiilor PNG; această consolidare parțială nu închide autonomia întregului audit.

**Închidere:** comparați inventarul și octeții obligatorii înainte/după publicare, apoi verificați numai HTML-ul într-un director gol și offline. Publicarea incompletă nu autorizează eliminarea singurei dovezi și nu emite PASS; istoricul migrat își păstrează originea și limitele, fără o execuție Android inventată.

## RPT-ANDROID-COMPONENT-HTML — comparații capturate, nu doar JUnit XML

**P1 · responsabil adaptor Android: extinderea capturii către componentele rămase.** [Suita istoricului](../../atf-test-android/src/test/java/atf/android/suites/AndroidHistoryComponentSuite.java) folosește auditul Kit și publică inputuri și comparații în Extent/fallback. Ramura largă [ComponentsOnly](../../scripts/run-whist-responsive-sanity.ps1) păstrează și componente nemigrate; un HTML al subsetului auditat nu certifică toate comparațiile selecției.

[Suita](../../atf-test-android/src/test/java/atf/android/suites/AndroidDisplayComponentSuite.java) selectează componente, iar [FrameFillAuditComponentTest](../../atf-test-android/src/test/java/atf/android/e2e/display/component/FrameFillAuditComponentTest.java) evaluează comparații JUnit directe. Rezultatele JUnit păstrează populația și verdictul, fără să reconstruiască valorile necapturate ale comparațiilor trecute.

- [ ] Extindeți către componentele rămase captura în timpul evaluării: input/fixture, expected independent, actual și verdict, cu grupul standard pliabil în TEST BODY. Codul testului sau JUnit PASS nu reconstruiesc ulterior valori necapturate.
- [ ] Extindeți acceptanța pilotului către fiecare familie migrată: individual și prin suită, un HTML per scope, inventar exact, invocări parametrizate distincte, FAIL/ERROR/SKIP originale și timing separat de publicare. Controlul adaugă o clasă componentă fără adaptorul de audit și cere detectarea diferenței JUnit–HTML; verificarea unui singur fișier HTML în lansator nu demonstrează această egalitate. Suportul reutilizabil rămâne în main; pilotul verifică deja eșecul, abort-ul și lipsa aserțiunilor într-o invocare înregistrată.
- [ ] Folosiți profilul neutru de componentă, cu stare/raster sintetic etichetat corect. Unit-urile rămân categorie separată; componentele fără dispozitiv nu primesc acțiuni ADB, HTTP sau capturi native fabricate.

**Închidere:** o comparație trecută și una negativă controlată sunt auditabile exclusiv din HTML, cu ultima valoare și ownerul corect; un caz fără comparații capturate este explicit necalificat.

[Publicarea ownerului](../../atf-test-android/src/main/java/atf/android/evidence/component/AndroidComponentRun.java) este amânată până la închiderea scope-ului și păstrează separat duratele originale. Costul memoriei la selecții mari și integrarea Jupiter paralelă cer calificare proprie înaintea extinderii; pilotul de istoric nu închide automat aceste frontiere.

## RPT-ANDROID-DETACHED-ACCEPTANCE — calificarea finalului publicat

**P1 · responsabil adaptor Android: acceptarea și guvernanța livrabilului.** Rapoartele istorice și cele curente au aceeași obligație de autonomie a datelor; mutarea sau arhivarea anexelor nu închide criteriul.

Proba pozitivă a HTML-ului din livrarea curentă confirmă încărcarea detașată fără rețea, tabelul de comparații, ultimul rând și deschiderea PNG-ului la dimensiunea originală, cu revenirea la același proprietar. Receipt-ul identifică APK-ul instalat, dar fingerprint-urile nu includ corpurile XML; criteriul de autonomie a datelor rămâne deschis. [Contractul reviziei](../reporting/ATF_Audit.md#calificarea-unui-producător) cere identificarea externă a exemplarului și limitelor probei; revizia offline nu reprezintă o execuție Android nouă sau acceptanță prin browserul MCP.

Confruntarea inventarului și a hash-urilor dovedește păstrarea identității la mutare, fără să execute accesul la probele din HTML. Guard-ul actual al publicatorului refuză numai forma literală href cu ghilimele duble, iar receipt-ul său nu substituie o calificare portabilă.

- [ ] Acceptați numai fișierul final identificat prin hash, copiat singur pentru probă, cu directoarele sursă/auxiliare indisponibile și resursele externe blocate. Verificați tabelele, ultimul rând, deschiderea/închiderea și revenirea de la PNG-ul integral pe câte un owner declarat; fără Expand all pe rapoarte mari.
- [ ] Consumați invariantul neutru [REPORT-ARTIFACT-AUTONOMY-150](README.md#contractul-comun-de-autonomie), cu verificare DOM și controale pentru link XML și imagine externă; numele unui control ori lipsa resurselor numărate nu este PASS.
- [ ] Păstrați distinct execuțiile pe sisteme Android diferite, diagnosticul FAIL și replay-ul istoric. Raportul explică separat rezultatul funcțional, ERROR-ul JUnit original și republicarea; reunirea într-un HTML nu schimbă identitatea, timpii sau verdicturile.

**Închidere:** fiecare publicator și handoff refuză mutanții, iar raportul detașat permite înțelegerea scopului fără README, checkout, XML/JSON sau alte fișiere. Problemele generice ale inspectorului urmează responsabilul bibliotecilor; datele Android și guvernanța versiunii rămân aici.

## Validarea timpurie a checkpoint-ului

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | [AndroidUiFlowTrace.checkpoint](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java) păstrează aserțiuni pe care [publicatorul](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java) le confruntă ulterior cu politica bibliotecii; sumarul prea lung poate fi refuzat abia la publicare. | Componentă: validați prin aceeași politică efectivă înaintea următoarei capturi, cu refuz localizat și fără trunchierea valorilor integrale din observații și XML. |

Consumatorul deține momentul validării și datele transmise. Dacă API-ul public nu expune politica necesară, contractul reutilizabil se solicită responsabilului bibliotecii; limita nu se recopiază ca o regulă locală independentă.

## Costul intrărilor consumatorului

| Prioritate | Responsabil și calificare rămasă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | Consumatorul raportării: costul selecției și compoziției la populații mari ori predominant cu eșec. | Componente cu populații controlate și mediu identificat: cost, număr de comparații și corp opțional, cu toate eșecurile, valorile obligatorii și inventarul omisiunilor păstrate. |

## Contractele responsabile

- [Predarea rezultatelor](../reporting/ATF_Handoff.md) deține proveniența republicării, idempotența înregistrării și pointerele distincte pentru încercare și acceptanță.
- [Auditul publicării](../reporting/ATF_Audit.md#calificarea-unui-producător) deține calificarea fiecărui producător, comparațiile originale, ultima valoare, capturile și controalele negative.
- [Contractul Android](../reporting/ATF_Android.md) deține asocierea acțiune–observație–comparație și distincția dintre rezultatul funcțional și publicare.

[Contractul Android](../reporting/ATF_Android.md) · [Auditul publicării](../reporting/ATF_Audit.md) · [Retenție](../learning/ATF_Retention.md)
