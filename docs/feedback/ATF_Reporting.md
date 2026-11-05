# Calificarea publicării și a predării rezultatelor

## Responsabilitate

Consumatorul deține înregistrările jocului, selecția, oracolul și indexarea externă. Defectele generice de API sau randare urmează [rutarea către responsabilul bibliotecilor](README.md).

## RPT-ANDROID-HTML-ONLY — livrabilul de audit fără copii auxiliare

**P1 · responsabil adaptor Android: colectare, publicare și predare.** [Livrarea curentă](../../artifacts/README.md) păstrează PNG-uri canonice inline, cu viewer offline și revenire la checkpoint. Acestea califică accesul la capturi, fără să demonstreze că întregul audit poate fi înțeles numai din HTML.

[Publicatorul](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java) încorporează PNG-ul, dar publică numai fingerprint-uri pentru ierarhii și manifest. Receipt-ul livrat numește pachetul drept „Installed APK”; versiunea și hash-ul binarului, configurația și proveniența completă rămân în fișiere externe.

- [ ] Produceți un HTML autonom per execuție/selecție/suită, cu identități distincte pentru invocări; toate datele necesare auditului rămân în raport, fără copii finale PNG/XML/JSON/properties/log ori directoare brute mutate într-o altă arhivă. Temporarele servesc doar construirii și verificării livrabilului.
- [ ] Prezentați întâi scopul, precondițiile, versiunea/build-ul și hash-ul APK, dispozitivul/API-ul, profilul, sursele și bibliotecile efectiv folosite; separați explicit identitatea capturată la execuție de cea stabilită ulterior. Pachetul aplicației nu este identitatea APK-ului.
- [ ] Delimitați în prezentare intervalul publicării de intervalele execuției capturate. Datele Started/Ended ale sesiunii HTML nu trebuie interpretate ca timpul Android atunci când durata nativă provine din alt interval; data lipsă rămâne lipsă, fără reconstrucție retrospectivă.
- [ ] Asociați starea necesară deciziei, inputul, acțiunile, observațiile, comparațiile și timpii aceluiași checkpoint. Includeți conținutul XML/JSON/trace relevant integral, afișat ca text sigur și pliabil, cu o explicație semantică; numele fișierului și SHA-256 singure nu permit auditul.
- [ ] Refolosiți observațiile, corpurile tipizate și PNG-urile canonice Reporting; valorile obligatorii și cadrele complete supraviețuiesc prezentării AUTO/SUMMARY/FULL, cu preview 240. Hash-ul confirmă identitatea datelor deja accesibile în HTML.
- [ ] Aliniați [catalogul versiunilor](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactCatalog.java), [predarea offline](../../atf-test-android/tools/whist_ui_handoff.py) și contractele livrării/retenției: catalogul cere astăzi JSON separat, iar predarea copiază HTML și documente auxiliare. Păstrați un singur exemplar canonic; indicii indică același exemplar.

**Închidere:** comparați inventarul și octeții obligatorii înainte/după publicare, apoi verificați numai HTML-ul într-un director gol și offline. Publicarea incompletă nu autorizează eliminarea singurei dovezi și nu emite PASS; istoricul migrat își păstrează originea și limitele, fără o execuție Android inventată.

## RPT-ANDROID-COMPONENT-HTML — comparații capturate, nu doar JUnit XML

**P1 · responsabil adaptor Android: adaptorul componentelor și selecția.** Ramura [ComponentsOnly](../../scripts/run-whist-responsive-sanity.ps1) copiază surefire-reports, setează PASS și revine fără publicare HTML.

[Suita](../../atf-test-android/src/test/java/atf/android/suites/AndroidDisplayComponentSuite.java) selectează componente, iar [FrameFillAuditComponentTest](../../atf-test-android/src/test/java/atf/android/e2e/display/component/FrameFillAuditComponentTest.java) evaluează comparații JUnit directe. Rezultatele JUnit păstrează populația și verdictul, fără să reconstruiască valorile necapturate ale comparațiilor trecute.

- [ ] Pentru componente, capturați în timpul evaluării inputul/fixture-ul, expected independent, actual și verdict; publicați comparațiile standard, pliabile, în TEST BODY. Codul testului sau JUnit PASS nu reconstruiesc ulterior valori necapturate.
- [ ] Verificați câte un caz individual și selecția prin suită: un HTML per rulare, inventar exact, cazuri parametrizate distincte, FAIL/ERROR/SKIP originale și timing separat de publicare. Suportul reutilizabil rămâne în main la adaptorul Android.
- [ ] Folosiți profilul neutru de componentă, cu stare/raster sintetic etichetat corect. Unit-urile rămân categorie separată; componentele fără dispozitiv nu primesc acțiuni ADB, HTTP sau capturi native fabricate.

**Închidere:** o comparație trecută și una negativă controlată sunt auditabile exclusiv din HTML, cu ultima valoare și ownerul corect; un caz fără comparații capturate este explicit necalificat.

## RPT-ANDROID-DETACHED-ACCEPTANCE — calificarea finalului publicat

**P1 · responsabil adaptor Android: acceptarea și guvernanța livrabilului.** Rapoartele istorice și cele curente au aceeași obligație de autonomie a datelor; mutarea sau arhivarea anexelor nu închide criteriul.

Proba pozitivă a HTML-ului din livrarea curentă confirmă încărcarea detașată fără rețea, tabelul de comparații, ultimul rând și deschiderea PNG-ului la dimensiunea originală, cu revenirea la același proprietar. Receipt-ul păstrează încă pachetul în câmpul APK, iar fingerprint-urile nu includ corpurile XML; criteriul de autonomie a datelor rămâne deschis. [Contractul reviziei](../reporting/ATF_Audit.md#calificarea-unui-producător) cere identificarea externă a exemplarului și limitelor probei; revizia offline nu reprezintă o execuție Android nouă sau acceptanță prin browserul MCP.

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
