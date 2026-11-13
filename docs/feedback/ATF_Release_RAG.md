# Lacune active în calificarea RAG a livrărilor

## Pe scurt

Contractul stabil al RAG aparține [ghidului de calificare](../testing/ATF_ReleaseQualification.md), [politicii pe platforme](../../config/release-platform-rag-policy.json) și [evaluatorului](../../atf-tests/tools/evaluate-release-rag.mjs). Guvernanța README-ului principal este implementată separat în [testele de structură a livrării](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactLayoutGovernanceTest.java) și în evaluatorul RAG; acest registru păstrează doar limitele rămase.

## Lacune deschise

| Prioritate | Lacună | Criteriu de închidere |
|---|---|---|
| P1 | Legătura independentă risc–teste | Planul declară riscul și selecțiile. Adăugați un selector verificabil care leagă diferența de sursă de unități, componente și E2E. Testați un diff de licitare cu `userFlowChanged` fals și un diff editorial care poate păstra selecția minimă. Diagnosticul indică regula și căile afectate. |
| P1 | Snapshot unitar și preflight al artefactelor | [Criteriile de mai jos](#identitatea-surselor-și-a-dosarului) acoperă verificarea timpurie a artefactelor și intrările citite de guvernanță. |
| P2 | Prospețimea receipt-urilor | Receipt-urile sunt legate prin hash, dar nu au un interval de valabilitate sau un criteriu temporal de expirare. Declarați originea temporală și termenul, apoi blocați automatizarea publicării pentru dovezi expirate ori fără timp verificabil. |
| P1 | Verificarea timpurie a dispozitivului Android | [Ghidul](../testing/ATF_ReleaseQualification.md) descrie verificarea dispozitivului. Integrați verificarea în lansatorul selecției native și testați separat transportul refuzat, pornirea incompletă și profilul greșit. |
| P2 | Costul pregătirii componentelor Maven | Măsurați separat copierea cache-ului, compilarea, testele și publicarea Extent. Pregătirea poate fi omisă când toate selecțiile Maven au dovezi valide. |
| P2 | Cronologia incompletă a calificării | Înregistrați începutul, sfârșitul și durata fiecărei etape, inclusiv cea de pornire la rece. Marcați suprapunerile. Cronometrați separat evaluatorul read-only, probele, publicarea și descărcarea. |

## Identitatea surselor și a dosarului

Verificatoarele controlează deja integritatea dosarului și proveniența selecțiilor. Mai rămâne un flux reproductibil care produce intrările din același snapshot și le confruntă la fiecare trecere de la pregătire la publicare. [Evaluatorul](../../atf-tests/tools/evaluate-release-rag.mjs) · [Integritatea dosarului](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java)

- Înainte de Maven, confruntați numele, dimensiunea și SHA-256 ale APK-ului și checksum-ului cu manifestul. Cazurile negative trebuie să oprească fluxul înaintea compilării la fișier lipsă, hash divergent sau intrare de build modificată după fixare.
- Inventariați și dosarul `release/` citit de suita de guvernanță. Adăugarea, modificarea sau ștergerea unei intrări între începutul și sfârșitul suitei invalidează atribuirea rezultatului. Dosarul rămâne separat de sursele compilate. [Inventarul surselor](../../scripts/Get-AtfSourceInputs.ps1)

## Perimetrul reutilizării

Pentru perimetre Gradle mai înguste, demonstrați dependențele de cod, resurse și configurație ale fiecărei colecții. Controalele trebuie să detecteze o modificare în codul partajat, o intrare citită dinamic și o dependență adăugată. [Contractul reutilizării](../testing/ATF_ReleaseQualification.md#reutilizarea-dovezilor-de-componentă) · [Verificatorul perimetrelor actuale](../../atf-tests/tools/component-evidence-scope.mjs)

## Proprietari și limite

- [Calificarea livrării](../testing/ATF_ReleaseQualification.md) deține procesul, stările, comenzile și limita dintre verificarea RAG și decizia operatorului.
- [Feedback-ul raportării](ATF_Reporting.md) deține acceptanța HTML-ului Extent, a capturilor inline și a egalității dintre raport și JUnit.
- [Guvernanța locală](ATF_Governance.md) verifică integritatea artefactelor în dosarele de release.
