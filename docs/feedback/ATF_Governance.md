# Reutilizarea guvernanței și coeziunea codului

## Responsabilitate

Proiectul deține perimetrul Android și adaptarea verificatoarelor. Contractul comun reutilizabil se solicită prin [responsabilitățile bibliotecilor](README.md), fără introducerea în consumator a implementărilor acestora.

## API reutilizabil pentru verificatoare

| Prioritate | Lacună | Criteriu de închidere |
|---|---|---|
| P1 | Validatoarele comune nu au încă un contract împachetat independent de reactorul producătorului. | API sau CLI cu rădăcină explicită, inventar de documente și surse, module, rădăcini JVM suplimentare și excepții motivate. |

Integrarea locală păstrează verificatoarele adaptate și controalele lor negative până la demonstrarea echivalenței API-ului împachetat. Configurația Surefire nu substituie identificarea rădăcinii efectiv inspectate.

## Costul inspecției sintactice

| Prioritate | Observație și sursă | Criteriu de închidere |
|---|---|---|
| P2 | Profilarea inspecției straturilor surprinde inițializarea încărcătorului de procesoare și interogarea classpath-ului în [parserul sintactic](../../atf-tests/src/test/java/atf/tests/governance/source/JavaTestDeclarations.java), deși verificarea consumă arborele de sintaxă. | Calificați o configurare parse-only cu dependențe delimitate și măsurați costul pe același inventar. Păstrați adnotările compuse, importurile ambigue, erorile de sintaxă și ciclul de viață al parserului prin controale independente. |

Profilarea și timpii concreți rămân în arhiva externă. Optimizarea parserului se califică separat de verificările funcționale ale aplicației.

## Criterii pentru contractul comun

- Același inventar alimentează structura, linkurile, fragmentele, proza, stilul, diagramele și căile configurabile.
- Diagnosticul conține regula, fișierul, linia și remedierea; rezultatul identifică validatorul și populația verificată.
- Rădăcina greșită, inventarul gol, documentele lipsă și căile din afara perimetrului sunt refuzate.
- Probele negative detectează defectul injectat și acceptă documentul sănătos vecin, inclusiv pe căi cu spații și diacritice.
- Descoperirea surselor și adnotările compuse păstrează aceeași semantică; comentariile și exemplele nu devin clase executabile.
- Rezolvarea adnotărilor trebuie să acopere pozițiile legale față de modificatorii declarației; [adaptorul local](../governance/README.md#declarațiile-testelor) folosește structura sintactică JDK, iar un contract comun se califică prin aceleași controale independente.
- Validarea nu rescrie documentele și nu relaxează regulile pentru a închide o revizie.

## Coeziunea și evoluția pachetelor

[Politica executabilă](../../config/atf-governance-policy.json) aplică limita fără derogări pentru structura curentă. [Responsabilitățile pachetelor](../governance/ATF_Packages.md) delimitează API-urile, suportul comun și selecțiile testelor.

**P2 · Restaurarea sesiunii Whist.** [GameSession](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/GameSession.java) deține mutările, scorul și rehidratarea checkpoint-ului.

- Probele restaurează aceeași salvare în licitare, joc și scor și confruntă prima comandă legală după reluare.
- O extragere a asamblării checkpoint-ului păstrează refuzul unei istorii incompatibile și compatibilitatea salvărilor existente.
- Decizia privind o frontieră separată se bazează pe aceste probe.

[Guvernanța calității](../governance/README.md) · [Bune practici documentare](../governance/ATF_Documentation.md)

## Calificarea afirmațiilor și lansatoarelor

Legarea dovezilor de sursa calificată ține de [registrul RAG](ATF_Release_RAG.md). Integritatea APK-ului și a payload-ului din dosarele înregistrate rămâne verificată de [guvernanța integrității release-ului](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java).

| Prioritate | Responsabil și lacună | Nivel și criteriu de închidere |
|---|---|---|
| P2 | Coordonarea: lansatoarele verifică precondițiile în moduri diferite. | Componente controlate pentru Java, Maven, Gradle, Node și Python: capabilitățile cerute intrării sunt verificate înaintea operației costisitoare, iar lipsa lor produce diagnostic și identitate de sursă explicite. Pentru Python, detectați aliasul de instalare separat de un interpretor executabil; pentru [modulul PowerShell ADB](../../atf-test-android/tools/AndroidDeviceSetup.psm1), verificați disponibilitatea, refuzul, timeout-ul și starea transportului cu proces controlat, separat de parsare și de clientul Java. |

[Retenția copiilor inline](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) verifică separat lipsa PNG-urilor identice lângă HTML. Această gardă nu dovedește autonomia datelor, lizibilitatea imaginilor sau integritatea APK-ului; [criteriul adaptorului](ATF_Reporting.md#rpt-android-html-only--livrabilul-de-audit-fără-copii-auxiliare) rămâne distinct.

## Îmbunătățirea ghidurilor de orientare

Grila editorială din [contractul documentației](../governance/ATF_Documentation.md#verificare-și-evaluare-editorială) evaluează documentul și limita lui, separat de maturitatea codului. Notele și inventarul evaluat rămân în auditul extern; aici păstrăm numai lucrările care pot îmbunătăți lectura sau reproducerea.

| Prioritate | Responsabil și suprafață | Criteriu de închidere |
|---|---|---|
| P2 | Ghidurile bibliotecilor distribuite: reutilizarea probei la producător | [Acceptanța locală a fragmentelor](../platform/ATF_Integration.md#verificarea-exemplelor-din-ghiduri) separă compilarea, execuția și verdictul. Promovați un contract generic al acestor probe în registrul responsabililor comuni și sincronizați clarificările ghidurilor la consumatori. Regresia producătorului și acceptanța fiecărui consumator rămân distincte. |

## Descărcarea prin browser

[Contractul distribuției](../platform/ATF_AndroidDelivery.md#verificări-automate-ale-distribuției) separă proba API, descărcarea efectivă din browser și accesul anonim. Verificarea browserului și hash-ul fișierului primit se consemnează în [dosarul versiunii active](../../release/README.md).

| Prioritate | Lacună delimitată | Criteriu de închidere |
|---|---|---|
| P2 | Proba din browserul autentificat depinde de operator și de sesiunea GitHub disponibilă. | După publicare, verificați URL-ul versiunii, fișierul primit și SHA-256, apoi deschideți auditul de pe commitul din note. Înregistrați separat răspunsurile anonime și eventualele refuzuri locale ale browserului. |

Proba de transfer prin Android DownloadManager și verificarea certificatului aparțin dosarului versiunii efectiv testate. [Contractul transferului Android](../platform/ATF_AndroidDelivery.md#separarea-accesului-de-transferul-android) descrie datele necesare, iar [accesul anonim pe ruta publică](../platform/ATF_AndroidDelivery.md#accesul-anonim-și-ruta-publică) are propriul criteriu de închidere.

## Acceptanța nativă a distribuției

| Prioritate | Lacună delimitată | Criteriu de închidere |
|---|---|---|
| P2 | Proba nativă este păstrată cu sursele în Extent, dar lansarea ei este încă o operație TMP a operatorului. | Adaptor opt-in versionat: citește identitatea candidatului, folosește permisiuni Android proprii și separă accesul de transfer. Componentele injectează 404, fișier scurt, hash/CRC greșit, certificat absent ori diferit și timeout; nu salvează redirectul semnat. Raportul păstrează comparațiile, eroarea și durata originale, iar cleanup-ul elimină doar resursele deținute. Un singur transfer Android califică livrarea; nu se multiplică pe rezoluțiile UI. |

## Reluarea publicării după răspunsuri parțiale

[Procesul livrării](../platform/ATF_AndroidDelivery.md#commit-ul-surselor-și-commit-ul-dovezilor) separă sursele, dovezile și ID-ul draftului. Verificatoarele existente controlează rezultatul; coordonarea operațiilor GitHub rămâne o sarcină a operatorului.

| Prioritate | Lacună | Criteriu de închidere |
|---|---|---|
| P2 | Publicarea nu are încă un lansator versionat care salvează imediat identitatea fiecărei operații remote și permite reluarea sigură. | Jurnal atomic cu ID-ul draftului înainte de upload, commit-ul surselor, asset-urile și hash-urile. API controlat demonstrează creare reușită urmată de 404 la căutarea după tag, upload acceptat cu răspuns pierdut și promovare deja efectuată. Reluarea recitește obiectele după ID și refuză diferențele; nu recreează, nu reîncarcă și nu mută tag-ul prin repetare oarbă. |

## Confruntarea afirmațiilor documentare

| Prioritate | Lacună și sursă | Criteriu de închidere |
|---|---|---|
| P2 | [Politica documentară](../../atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java) recunoaște forma controalelor testChanges; această recunoaștere nu confruntă singură baza Git, căile schimbate și expirarea. | Definiți explicit partea locală și contractul comun, cu refuz pentru bază lipsă, cale neacoperită, control expirat și modificare neautorizată a unui gate. Păstrați distinct validarea formatului și validarea schimbării efective. |

Inventarul fișierelor `release/` citite în timpul guvernanței aparține [contractului de snapshot RAG](../testing/ATF_ReleaseQualification.md#snapshot-și-preflight-înainte-de-maven).

## Lizibilitatea coordonatorilor de domeniu

**P2 · deschis · responsabil: motorul și coordonarea deciziei.**

[GameSession](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/GameSession.java) și [MonteCarloAi](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/engine/MonteCarloAi.java) păstrează coordonare și mai multe modele sau etape interne. Pragul pachetelor rămâne util, însă nu califică singur lizibilitatea metodelor și stabilitatea fațadelor.

Extrageți o singură responsabilitate demonstrată pe increment, cu același contract public, aceleași alegeri și estimări pe observații fixate, conservarea scorului și verificări nominale și de refuz. Păstrați modificările de prezentare și candidații strategici în lucrări separate; o formatare amplă nu dovedește refactorizare semantică.

- O regulă nouă de stratificare sau de proprietate a stării cere o intrare sănătoasă și una defectă.
- [Registrul adaptorului](ATF_Android.md#persistență-și-coordonare) deține probele pentru fixture-urile Android din Release.
- [Registrul motorului](ATF_Ai.md#evoluția-motorului) deține gestionarea mostrelor Monte Carlo.
