# Lacune active în calificarea RAG a livrărilor

## Pe scurt

Contractul stabil al RAG aparține [ghidului de calificare](../testing/ATF_ReleaseQualification.md), [politicii pe platforme](../../config/release-platform-rag-policy.json) și [evaluatorului](../../atf-tests/tools/evaluate-release-rag.mjs). Acest registru păstrează doar limitele care mai cer implementare sau calificare.

## Lacune deschise

| Prioritate | Lacună | Criteriu de închidere |
|---|---|---|
| P1 | Suprafața publică a README-ului principal | README-ul din rădăcină expune digestul SHA-256 al APK-ului. Nici guvernanța documentară, nici evaluatorul RAG nu au o regulă care să interzică această publicare sau să o lege de manifestul versiunii. Păstrați în README doar versiunea curentă și linkul de descărcare; digestul rămâne în manifestul și dovezile versiunii. Adăugați probă de guvernanță care respinge digestul în README-ul principal, dar acceptă checksum-ul valid din dosarul versiunii, plus un control RAG care evaluează README-ul fixat în același snapshot. Testul negativ trebuie să injecteze digestul și să blocheze calificarea. |
| P1 | Inventarul complet al snapshot-ului | Evaluatorul verifică toate intrările declarate, hash-urile și egalitatea seturilor APK–componente, dar nu poate detecta fișiere omise din manifest și nu inspectează indexul Git. Producătorul snapshot-ului definește explicit intrările și excluderile; testele negative blochează fișiere lipsă, suplimentare, schimbate sau adăugate după fixare. |
| P1 | Legătura independentă risc–teste | Planul declară riscul și selecțiile. Evaluatorul nu deduce suprafața schimbată din diferența surselor și nu demonstrează independent că selecția minimă acoperă contractele afectate. Un selector verificabil trebuie să lege schimbarea de unități, componente, E2E și motivarea omisiunilor, cu controale negative. |
| P1 | Producător și handoff unic al candidatului | CLI-ul RAG doar citește și evaluează receipt-uri; pregătirea snapshot-ului, selecțiile, APK-ul, manifestul și predarea rămân coordonate manual. Un flux reproductibil trebuie să producă toate intrările în afara checkout-ului, să lege aceleași identități și să blocheze verdictul când snapshot-ul candidatului diferă de sursa calificată. |
| P2 | Prospețimea receipt-urilor | Receipt-urile sunt legate prin hash, dar nu au un interval de valabilitate sau un criteriu temporal de expirare. Declarați originea temporală și termenul, apoi blocați automatizarea publicării pentru dovezi expirate ori fără timp verificabil. |

## Proprietari și limite

- [Calificarea livrării](../testing/ATF_ReleaseQualification.md) deține procesul, stările, comenzile și limita dintre verificarea RAG și decizia operatorului.
- [Feedback-ul raportării](ATF_Reporting.md) deține acceptanța HTML-ului Extent, a capturilor inline și a egalității dintre raport și JUnit.
- [Guvernanța locală](ATF_Governance.md) verifică integritatea artefactelor în dosarele de release; completitudinea snapshot-ului RAG rămâne în acest registru.
