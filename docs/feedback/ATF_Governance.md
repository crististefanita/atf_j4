# Reutilizarea guvernanței și coeziunea codului

## Pe scurt

Proiectul deține perimetrul Android și adaptarea verificatoarelor. Contractul comun reutilizabil se solicită prin [responsabilitățile bibliotecilor](README.md), fără introducerea în consumator a implementărilor acestora.

## Costul inspecției sintactice

| Prioritate | Observație și sursă | Criteriu de închidere |
|---|---|---|
| P2 | [Parserul sintactic](../../atf-tests/src/test/java/atf/tests/governance/source/JavaTestDeclarations.java) dezactivează deja procesarea adnotărilor și folosește căi goale pentru clase și procesoare. Costul acestei configurări nu a fost măsurat pe același inventar. | Măsurați costul parsării pe un mediu și inventar identificate. Păstrați controale independente pentru adnotări compuse, importuri ambigue, erori de sintaxă și ciclul de viață al parserului; configurarea singură nu dovedește accelerarea. |


## Coeziunea și evoluția pachetelor

[Politica executabilă](../../config/atf-governance-policy.json) aplică limita fără derogări pentru structura curentă. [Responsabilitățile pachetelor](../governance/ATF_Packages.md) delimitează API-urile, suportul comun și selecțiile testelor.

[Guvernanța calității](../governance/README.md) · [Bune practici documentare](../governance/ATF_Documentation.md)

## Calificarea afirmațiilor și lansatoarelor

Legarea dovezilor de sursa calificată este verificată de [regresiile evaluatorului RAG](../testing/ATF_ReleaseQualification.md#regresiile-evaluatorului). Integritatea APK-ului și a payload-ului din dosarele înregistrate rămâne verificată de [guvernanța integrității release-ului](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java).

| Prioritate | Responsabil și lacună | Nivel și criteriu de închidere |
|---|---|---|
| P2 | Coordonarea: lansatoarele verifică precondițiile în moduri diferite. | Componente controlate pentru Java, Maven, Gradle, Node și Python: capabilitățile cerute intrării sunt verificate înaintea operației costisitoare, iar lipsa lor produce diagnostic și identitate de sursă explicite. Pentru Python, detectați aliasul de instalare separat de un interpretor executabil; pentru [modulul PowerShell ADB](../../atf-test-android/tools/AndroidDeviceSetup.psm1), verificați disponibilitatea, refuzul, timeout-ul și starea transportului cu proces controlat, separat de parsare și de clientul Java. |
| P2 | Costul calificării: pregătirea snapshot-ului copiază și recalculează repetat hash-urile rapoartelor păstrate, iar suita de infrastructură rămâne o unitate largă de reutilizare. | Măsurați separat hash-urile surselor și release-urilor, copierea, pregătirea JVM și execuția. Calificați orice reutilizare mai fină prin intrări imuabile și controale negative pentru octeți modificați sau lipsă; nu reatribuiți execuții istorice și nu omiteți cazuri afectate. |

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

## Limitele calificării release-ului

Separarea sursei native de validator aparține [contractului de calificare](../testing/ATF_ReleaseQualification.md#proveniența-execuției-native-și-a-validatorului). Reutilizarea cere în continuare motor, scenariu, APK și dovezi originale neschimbate.

## Costul calificării și reluarea invocărilor

Validarea snapshot-ului și a cache-ului consumă timp chiar când execuția testelor se reutilizează. Măsurați aceste costuri separat de execuție și de așteptarea rezervării.

- Salvați fiecare invocare acceptată împreună cu sursa, dependențele compilate, identitatea, comparațiile originale și hash-urile rapoartelor.
- Reluați invocările nerezolvate numai după verificarea acestor intrări imuabile. Păstrați încercările respinse ca diagnostice.
- Dovediți paritatea întregului set de identități JUnit față de o rulare completă; respingeți sursele schimbate, cazurile lipsă, identitățile duplicate și dovezile modificate.
- Păstrați timeout-urile explicite până trece o reluare reală. Un caz vecin trecut sau un receipt rescris nu reprezintă dovezi înlocuitoare.

## Costul selecției pe platformă

Selecția conservatoare include resursele exclusiv Web, pachetele istorice ale navigatorului și instrumentele de împachetare în fluxurile Android. Păstrați această limită de siguranță până când componentele bazate pe dependențe demonstrează o selecție mai restrânsă.

- Declarați platforma responsabilă pentru resurse și împachetare; schimbările necunoscute ale execuției comune trebuie să selecteze în continuare fiecare flux afectat.
- Simulați schimbări exclusiv Web, eliminarea versiunilor nefolosite, schimbări ale meniului Android și ale dependențelor de execuție. Cereți un caz vecin valid și un control negativ pentru fiecare excludere.
- Măsurați separat inventarul, copierea, compilarea, execuția și așteptarea dispozitivului. Păstrați separat snapshot-ul RAG exclusiv de surse și workspace-urile componentelor care conțin catalogul de release și fișiere generate.

## Admiterea testChanges înainte de pregătirea colecției

Respingeți probele de control al sursei lipsă sau expirate înainte de copierea cache-urilor, compilare ori lansarea unei colecții lungi. Eșecul târziu de guvernanță protejează corectitudinea, dar consumă inutil pregătirea validată și execuțiile fără legătură.

- Descoperiți ID-urile controalelor necesare din diff-ul Git revizuit și validați explicit rădăcina externă a probelor.
- Folosiți același validator pentru admitere și guvernanța finală: ID-ul, baza, commitul, diff-ul, selectorii pozitivi și negativi și hash-ul raportului original trebuie să corespundă.
- Generați controalele lipsă folosind compilarea existentă înaintea colecției primare; transmiteți explicit rădăcina rezultată procesului suitei.
- Acoperiți configurația absentă, alt commit, controlul negativ lipsă, raportul modificat și schimbarea sursei între admitere și execuție. Probele lipsă trebuie să oprească fluxul înainte de Maven; sursele schimbate trebuie să invalideze în continuare admiterea.
- Măsurați separat pregătirea respinsă și execuția acceptată. Nu înlocuiți raportul original eșuat cu un caz vecin trecut și nu editați un receipt de execuție.

- Faceți baza de revizie un parametru explicit în lansările locale și izolate. Verificați planul controalelor înaintea dependențelor; o bază istorică incompatibilă trebuie respinsă cu același diagnostic înaintea compilării.

Fluxul izolat primește acum explicit baza reviziei. Admiterea probelor de control al sursei lipsă înaintea pregătirii dependențelor rămâne deschisă; alegerea bazei corecte nu închide singură această limită.

## Admiterea commitului auditului publicat

Fluxul de publicare poate afișa un commit pregătit înainte ca manifestul să conțină commitul sursei calificate. Guvernanța finală respinge acest link, dar ediția publică necesită apoi corectarea metadatelor.

- Validați commitul auditului înainte de publicarea adresei: cereți sursa calificată, identitatea identică a APK-ului și hash-ul raportului canonic, cu link direct în indexul versionat.
- Acoperiți prin componente manifestul pregătit fără sursă, alt commit al sursei, raportul lipsă și hash-ul modificat al raportului.
- Păstrați receipt-urile originale ale execuțiilor și fișierele publicate când corectați metadatele prezentării. Verificați separat notele GitHub și dosarul versionat.
- Refolosiți selecția de guvernanță deja compilată pentru corectarea metadatelor; nu repetați componentele sau execuțiile Android neschimbate.
