# Lacune active în calificarea RAG a livrărilor

## Pe scurt

Contractul stabil al RAG aparține [ghidului de calificare](../testing/ATF_ReleaseQualification.md), [politicii pe platforme](../../config/release-platform-rag-policy.json) și [evaluatorului](../../atf-tests/tools/evaluate-release-rag.mjs). Guvernanța README-ului principal este implementată separat în [testele de structură a livrării](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactLayoutGovernanceTest.java) și în evaluatorul RAG; acest registru păstrează doar limitele rămase.

## Lacune deschise

| Prioritate | Lacună | Criteriu de închidere |
|---|---|---|
| P2 | Prospețimea receipt-urilor | Receipt-urile sunt legate prin hash, dar nu au un interval de valabilitate sau un criteriu temporal de expirare. Declarați originea temporală și termenul, apoi blocați automatizarea publicării pentru dovezi expirate ori fără timp verificabil. |
| P1 | Verificarea timpurie a dispozitivului Android | [Ghidul](../testing/ATF_ReleaseQualification.md) descrie verificarea dispozitivului. Integrați verificarea în lansatorul selecției native și testați separat transportul refuzat, pornirea incompletă și profilul greșit. |
| P2 | Costul pregătirii componentelor Maven | Măsurați separat copierea cache-ului, compilarea, testele și publicarea Extent. Pregătirea poate fi omisă când toate selecțiile Maven au dovezi valide. |
| P2 | Cronologia incompletă a calificării | Înregistrați începutul, sfârșitul și durata fiecărei etape, inclusiv cea de pornire la rece. Marcați suprapunerile. Cronometrați separat evaluatorul read-only, probele, publicarea și descărcarea. |

## Perimetrul reutilizării

Pentru perimetre Gradle mai înguste, demonstrați dependențele de cod, resurse și configurație ale fiecărei colecții. Controalele trebuie să detecteze o modificare în codul partajat, o intrare citită dinamic și o dependență adăugată. [Contractul reutilizării](../testing/ATF_ReleaseQualification.md#reutilizarea-dovezilor-de-componentă) · [Verificatorul perimetrelor actuale](../../atf-tests/tools/component-evidence-scope.mjs)

## Proprietari și limite

- [Calificarea livrării](../testing/ATF_ReleaseQualification.md) deține procesul, stările, comenzile și limita dintre verificarea RAG și decizia operatorului.
- [Feedback-ul raportării](ATF_Reporting.md) deține acceptanța HTML-ului Extent, a capturilor inline și a egalității dintre raport și JUnit.
- [Guvernanța locală](ATF_Governance.md) verifică integritatea artefactelor în dosarele de release.
