# Lacune active în calificarea RAG a livrărilor

## Pe scurt

Contractul stabil al RAG aparține [ghidului de calificare](../testing/ATF_ReleaseQualification.md), [politicii pe platforme](../../config/release-platform-rag-policy.json) și [evaluatorului](../../atf-tests/tools/evaluate-release-rag.mjs). Guvernanța README-ului principal este implementată separat în [testele de structură a livrării](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactLayoutGovernanceTest.java) și în evaluatorul RAG; acest registru păstrează doar limitele rămase.

## Lacune deschise

| Prioritate | Lacună | Criteriu de închidere |
|---|---|---|
| P1 | Legătura independentă risc–teste | Planul declară riscul și selecțiile. Evaluatorul nu deduce suprafața schimbată din diferența surselor și nu demonstrează independent că selecția minimă acoperă contractele afectate. Un selector verificabil trebuie să lege schimbarea de unități, componente, E2E și motivarea omisiunilor, cu controale negative. |
| P1 | Inventarul complet al snapshot-ului; Producător și handoff unic al candidatului | CLI-ul RAG doar citește și evaluează receipt-uri; pregătirea snapshot-ului, selecțiile, APK-ul, manifestul și predarea rămân coordonate manual. Un flux reproductibil trebuie să producă toate intrările în afara checkout-ului, să lege aceleași identități și să blocheze verdictul când snapshot-ul candidatului diferă de sursa calificată. |
| P2 | Prospețimea receipt-urilor | Receipt-urile sunt legate prin hash, dar nu au un interval de valabilitate sau un criteriu temporal de expirare. Declarați originea temporală și termenul, apoi blocați automatizarea publicării pentru dovezi expirate ori fără timp verificabil. |
| P2 | Costul proceselor de evaluare | Măsurați pornirea Node și citirea repetată a fixture-urilor pentru testele evaluatorului. Calificați evaluarea în lot cu procese persistente și păstrați izolarea fiecărui caz, codul de ieșire și timeout-ul. |

## Reproducere și criterii de acceptare

- **P1 · Risc și selecție.** Comparați tagul anterior cu sursa fixată și afișați regula care leagă fiecare cale relevantă de o suită. Un diff care mută controlul de licitare trebuie să ceară verificarea comportamentului chiar dacă planul declară flux neschimbat. Un diff exclusiv editorial păstrează selecția minimă cu motiv. Contradicția se vede înaintea verdictului, cu citarea căii și a selectorului.
- **P1 · Handoff.** Produceți planul, inventarul și receipt-urile pornind de la sursa fixată. Înaintea suitei de guvernanță, materializați APK-urile istorice cerute de catalog și confruntați-le cu hash-urile publicate. Calificați separat documentele de predare care apar după fixarea APK-ului, cu o comparație explicită a intrărilor de build. Probele negative amestecă două build-uri valide, elimină un APK istoric și schimbă o intrare de build după fixare.
- **P2 · Perimetre mai precise.** [Contractul reutilizării](../testing/ATF_ReleaseQualification.md#reutilizarea-dovezilor-de-componentă) acoperă perimetrele cunoscute și păstrează originea fiecărei execuții. Pentru a reduce și selecțiile Gradle, demonstrați dependențele de cod, resurse și configurație ale fiecărei colecții. Verificați o schimbare în codul partajat, o intrare citită dinamic și o dependență adăugată înainte de a restrânge perimetrul.

## Proprietari și limite

- [Calificarea livrării](../testing/ATF_ReleaseQualification.md) deține procesul, stările, comenzile și limita dintre verificarea RAG și decizia operatorului.
- [Feedback-ul raportării](ATF_Reporting.md) deține acceptanța HTML-ului Extent, a capturilor inline și a egalității dintre raport și JUnit.
- [Guvernanța locală](ATF_Governance.md) verifică integritatea artefactelor în dosarele de release.
