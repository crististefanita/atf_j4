# Istoricul partidei și calificarea interfeței

## Responsabilitate

Motorul deține datele partidei, iar aplicația Android deține persistența și prezentarea lor. Modificările interfeței se califică pe binarul efectiv instalat, cu starea și scopul scenariului declarate.

## Contract prioritar de afișaj portret

[Extragerea interfeței comune](ATF_CardUi_Extraction.md) are registru și criterii proprii de refactorizare. Păstrează prezentarea Whist; calificările native încă deschise în acest document nu sunt închise prin mutarea claselor sau prin componentele modulului nou.

[Contractul profilului și al afișării](../testing/ATF_Display.md#profil-și-domeniu-de-calificare) deține geometria, densitatea, textul și invariantele stabile. Aici rămân calificările native lipsă; prezența unei cerințe în ghid nu o declară automat trecută.

## Acceptanța nativă a proporțiilor și ordinii

| Prioritate | Calificare rămasă | Criteriu de închidere |
|---|---|---|
| P1 | Extinderea calificării native dincolo de profilul compact; componentele nu substituie fonturile și interacțiunea Android. | Profil înalt și text mărit: 13 cărți, levată completă la 5 locuri și succesiunea actorilor la 3/5/6 locuri, cu nume și indici lizibili. Păstrați capturile, XML-ul și hash-ul APK-ului, fără repetarea unei partide complete pe fiecare profil. |

[Catalogul livrărilor](../../release/README.md) păstrează proba compactă cu 13 cărți, levată completă la patru locuri și licitare, inclusiv auditul pixelilor indicilor. Calificarea acestui profil nu închide restul matricei.

## Compatibilitatea istoricului

### P1 — calificarea nativă a istoricului cu două coloane

Sanity-ul folosește un APK care include schimbarea; nu se atribuie unui release anterior. Componentele califică separat proiecția, persistența și geometria.

| Suprafață | Criteriu de închidere |
|---|---|
| Două coloane | Exact licitat și punctajul total după fiecare mână per jucător; coloana `P` afișează soldul rândului, inclusiv zero și valori negative, fără o a treia coloană. RO/EN, text normal și mărit, prima și ultima mână și toate locurile, pe un APK care include acest contract. |
| Dreptunghi de serie | Contur normal numai după cinci realizări eligibile consecutive; contur roșu după cinci ratări eligibile consecutive, inclusiv când totalul rămâne pozitiv. Bonusurile și penalizările ±5/±10 încadrează totalul numai pe rândul plății, exclusiv în celula `P`; licitația, antetul, vecinii și rezultatele fără plată nu primesc dreptunghi. |
| Randare | Captura originală confirmă cele patru laturi, lizibilitatea numărului și semnului, spațiul până la contur și lipsa intersecțiilor, inclusiv la text mărit. Prezența nodului în XML nu demonstrează dreptunghiul, iar un raport al unui APK anterior nu califică noul marcaj. |
| Tăietură oblică | O licitație ratată are marcaj vizibil în rasterul Android, inclusiv la zero și text mărit; numai culoarea roșie sau descrierea accesibilă nu certifică tăietura. Comparați cu un contract reușit și păstrați lizibilitatea cifrei. |
| Integritate | Explorarea și reluarea păstrează totalurile și toate rândurile; marcajul rămâne asociat mâinii și locului corect. |
| Separatoare | Țintă de 2,1 dp, rotunjită în pixeli o singură dată; toate granițele jucătorilor rămân aliniate între antete și corp. PNG-ul de componentă califică geometria și culoarea în randarea JVM; fontul și pixelii Android cer APK identificat. |

[ScoreHistory](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/ScoreHistory.java) păstrează sufixele cunoscute ale licitațiilor și levatelor. Punctele se derivă din solduri, iar ratarea compară licitatul cu levatele; nu se deduce din culoare sau semnul punctajului.

| Prioritate | Cerință rămasă | Criteriu de închidere |
|---|---|---|
| P2 | Acceptanța nativă a importului unui registru vechi rămâne distinctă de componentele migrării. | Reluarea păstrează soldurile și prefixul necunoscut; numai rezultatul demonstrabil primește marcaj. Verificați asocierea loc–mână–licitat–levate–puncte–total. |

Un sold sau o diferență de puncte nu justifică reconstruirea unor levate lipsă. Contractul implementat și limita migrării rămân în [modelul istoricului](../game/ATF_ScoreHistory.md).

## Persistență și coordonare

[Comenzile și salvarea](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md#comenzi-salvare-și-erori) au porturi de componentă pentru refuz, checkpoint, clasament și continuare. Randarea nu înregistrează rezultate; recuperarea stocării native și costul pe dispozitiv păstrează calificare distinctă.

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P1 | Recuperarea stocării Android: checkpoint-ul `SharedPreferences` și clasamentul `AtomicFile` nu au acceptanță nativă după corupere sau întrerupere. | Android pe APK identificat: întrerupere controlată, recuperare sau eroare explicită, datele utilizatorului păstrate; componentele serializării și porturile de stocare rămân verificări separate. |
| P2 | Latența checkpoint-ului sincron nu este calificată pe dispozitiv; ordinea componentelor nu certifică reactivitatea. | Mediu și stare identificate, costul scrierii separat de calculul AI și randare; păstrați ordinea și cauza erorii înaintea unei eventuale mutări a I/O între executori. |
| P2 | [MainActivity](../../whist-lab-app/app/src/main/java/ro/whistlab/app/MainActivity.java) rămâne un coordonator amplu pentru navigare, randare, ciclul AI și fabricile fixture-urilor de test, chiar dacă porturile principale au fost extrase. | Izolați fabricarea fixture-urilor deterministe într-un seam exclus din varianta Release și micșorați activitatea numai pe responsabilități confirmate. Păstrați scenariile Debug existente și verificați că APK-ul Release nu poate activa fixture-uri prin intent extras. |

## Calificarea modificărilor vizuale

| Prioritate | Suprafață | Verificare necesară înainte de acceptare |
|---|---|---|
| P2 | Colectarea levatei: legarea nativă a porturilor calificate prin componente | APK identificat la 3–6 locuri: măsurarea și clipping-ul corespund scenei, transformările se restaurează la anulare, iar ultima levată precedă scorul. Confruntați `onPause`/reluarea, meniul și dialogurile suprapuse, redimensionări consecutive, notificări de configurație, detașarea și animațiile dezactivate. Numai ultima geometrie poate primi scena nouă; un callback întârziat nu reia ruta abandonată și nu consumă timpul suspendat. Politica portret rămâne cea din manifest; probele cu ceas și evenimente controlate nu califică pixelii sau fluiditatea. |
| P1 | Masă, carte și atu | Profiluri declarate, text mărit, intersecții reale, coordonate logice mapate la raster și revizie a capturii originale. |
| P1 | Istoric și reluare | Toate mâinile încheiate și toate locurile accesibile, fără acordarea repetată a punctelor ori avansarea partidei. |
| P1 | Accesibilitate și localizare | Ordine asistată, etichete ale valorilor necunoscute ori negative, primul și ultimul element derulabil în ambele limbi. La text mărit, confruntați separat titlul integral al dialogului și numele din prima/ultima coloană, înainte și după derulare; calificarea regiunii legendei nu demonstrează lizibilitatea acestor regiuni. |
| P2 | Nume configurat și insignă de tură | Verificați numele la lungimea maximă acceptată, în toate pozițiile și cu insigna activă; identitatea vizibilă trebuie să fie lizibilă, iar prescurtarea să aibă o regulă explicită și acces la numele complet. |

- **Regiune:** acțiunea turei are o regiune măsurată deasupra chenarului; componentele verifică dimensiunile și separarea pentru text mărit.
- **Observație:** E2E folosește același decodor pentru joc și licitare; acceptanța nativă rămâne necesară pentru glyph-uri, împărțirea reală pe rânduri și capturi în RO/EN.

- **Sanity:** observați acțiunea integrală deasupra numelui la toate locurile active, inclusiv după schimbarea turei și la revenirea din pauză.
- **Istoric:** verificați prima și ultima coloană după derulare, etichetele numerice ale cărților și continuitatea separatoarelor dintre jucători.

O schimbare de geometrie sau stil se leagă de sursele și APK-ul inspectate. Calificarea pe dispozitiv și auditul componentelor sunt înregistrări distincte, păstrate extern.

- **Atu comun:** sanity-ul compară dimensiunile ramei la aceeași densitate și scară de text, în toate modurile, cu o carte întoarsă, o culoare și lipsa atuului. Verifică cele două regiuni complete și identitatea accesibilă, fără un rând suplimentar despre absența cărții.
- **Depunere:** verificați butonul cu textul RO/EN complet, la font normal și mărit; conturul retras rămâne separat de ținta tactilă. [Contractul display](../testing/ATF_Display.md) deține dimensiunile și criteriile comune.

## Acceptanță nativă Android 17

Protecția ferestrei și calculul spațiului util sunt implementate; acceptanța pe API 37 rămâne distinctă de componente și de verificarea pe un alt sistem Android. Comparația între versiuni cere identitatea ambelor APK-uri și a dispozitivului.

| Prioritate | Cerință rămasă | Criteriu de închidere |
|---|---|---|
| P1 | API 37: fereastră, reluare și memorie | Pe un dispozitiv identificat: navigare prin gesturi și trei butoane, decupaj, tastatură, reluare și câteva mâini; înregistrați memoria, motivul ieșirii și hash-ul APK-ului, fără modificarea limitelor sistemului. |

[Contractul livrării](../platform/ATF_AndroidDelivery.md) distinge padding-ul cu IME de măsurarea ferestrei fără IME și definește restaurarea și diagnosticul de memorie. Componentele verifică modelul, iar criteriul nativ rămâne deschis până la execuția callback-urilor și a scenariului pe sistemul țintă.

## Memoria adaptorului

Limita de memorie a adaptorului rămâne distinctă de geometria interfeței.

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | [CommandProcess](../../atf-test-android/src/main/java/atf/android/process/CommandProcess.java) citește fluxurile integral, fără bugete separate pentru text și binar. | Componente cu ieșire controlată: depășire explicită a fiecărui buget, memorie delimitată și PNG incomplet refuzat înaintea publicării; termenul și cauza inițială se păstrează. |

## Distribuție GitHub: accesul anonim

| Prioritate | Blocaj observat | Criteriu de închidere |
|---|---|---|
| P1 extern | API-ul Search autentificat confirmă `User flagged as spammy.`; profilul, repository-ul public și asset-ul returnează 404 anonim. | Solicitați revizuirea marcajului contului prin formularul oficial GitHub; după ridicare, URL-ul public al asset-ului trebuie să răspundă 200 și să producă exact mărimea și SHA-256 ale binarului verificat. |

[Registrul distribuției](../../release/README.md) identifică dosarul activ și probele sale. Starea publicării, descărcarea în browserul autentificat și accesul anonim au verdicte separate. Semnalul furnizorului și codurile HTTP rămân în proba de distribuție.

Identitatea repository-ului, a release-ului și a asset-ului, mărimea și SHA-256 aparțin înregistrării distribuției și cererii de suport, fără duplicarea lor în ghid. Distribuția publică rămâne necalificată; modificarea APK-ului nu compensează un răspuns HTTP negativ.

## Automatizarea publicării

### P1 — sursa tag-ului trebuie să existe și pe un branch remote

Acceptanța nativă a versiunii publicate rămâne condiționată de proveniența completă: tag-ul identifică sursa declarată, iar același commit este accesibil din istoricul unui branch remote. Publicarea asset-ului și un commit ulterior cu documentele versiunii nu demonstrează această relație.

- **Defect urmărit:** un tag poate identifica sursa APK-ului fără ca acea sursă să fie accesibilă din istoricul unui branch remote. Descărcarea autentificată verificată și un commit ulterior cu documentele nu închid această diferență de proveniență.
- **Criteriu de închidere:** proprietarul publicării păstrează sursa release-ului pe un branch remote; după `git fetch`, enumerarea branch-urilor și verificarea strămoșului trebuie să confirme exact commit-ul tag-ului. Nu rescrieți istoricul comun sau tag-ul publicat pentru a masca discrepanța.
- **Dovadă externă:** [verificarea porții release-ului](../../../../whist_back/release-acceptance/1.9.2/release-gate.json), împreună cu receipt-ul publicării și APK-ul arhivat în același dosar. Instalarea și rezervarea emulatorului nu au început.

| Prioritate | Lacună rămasă | Criteriu de închidere |
|---|---|---|
| P2 | [Procesul versiunii](../platform/ATF_AndroidDelivery.md#procesul-unei-versiuni) este documentat, iar operațiile de publicare folosesc încă unelte temporare. | Un lansator versionat confruntă tag-ul, APK-ul, checksum-ul și notele înainte de publicare; reluarea unui draft verifică asset-urile existente, iar o versiune publicată nu este înlocuită implicit. Actualizează atomic dosarul versiunii, indexul și catalogul, cu linkul Release către auditul fixat pe commit. |

[Guvernanța prezentării](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java) verifică structura și identitatea notelor; sensul schimbărilor se confruntă cu sursele release-ului la revizie. Notele publice trimit către dosarul tehnic, care păstrează rezultatele și limitele verificării.

## Contractele responsabile

- [Scenariile de profil, istoric, clasamente și serii](../testing/ATF_Android.md#profil-istoric-clasamente-și-serii) dețin mecanismele automatizate și precondițiile; existența scenariului nu califică o versiune.
- [Componentele display](../testing/ATF_DisplayComponents.md) dețin mock-urile și izolarea cache-ului; fonturile și interacțiunea nativă au verificare distinctă.
- [Calificarea publicării](ATF_Reporting.md) păstrează lacuna validării timpurii a checkpoint-ului; [predarea rezultatelor](../reporting/ATF_Handoff.md) deține distincția dintre verdictul funcțional și publicare.

[Testarea Android](../testing/ATF_Android.md) · [Profiluri de ecran](../testing/ATF_Display.md) · [Atlasul interfeței](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md)
