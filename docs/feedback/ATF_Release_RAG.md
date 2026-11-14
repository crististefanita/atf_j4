# Lacune active în calificarea RAG a livrărilor

## Pe scurt

[Ghidul de calificare](../testing/ATF_ReleaseQualification.md) deține procesul, iar [evaluatorul](../../atf-tests/tools/evaluate-release-rag.mjs) verifică dovezile. Aici rămân lucrările deschise care afectează durata sau reluarea unei calificări.

## Identitatea snapshot-ului și timpul

| ID | Lacună | Criteriu de închidere |
|---|---|---|
| RAG-03 | Prospețimea receipt-urilor. Chitanțele sunt legate prin hash. Semnificația vechimii și expirarea dovezilor rămân nedefinite pentru preflight, artefacte și verificarea remote. | Definește ce expiră și în ce condiții. Separă dovezile imuabile de starea schimbătoare a dispozitivului sau a GitHub. Verifică starea expirată, timpul necunoscut și datele lipsă. |
| RAG-06 | Costul pregătirii componentelor Maven și durata calificării se citesc din loguri separate. Modificarea sursei live a invalidat rulări după terminarea testelor. | Salvează etapele în ordinea reală cu intervale și motivul deciziei RUN/REUSE. Compară explicit snapshot-ul promovat cu cel calificat. Verifică reconstruirea integrală din commit, inclusiv fișierele cu EOL mixt. Când Git nu poate reproduce octeții, păstrează numai intrările unice și verifică hash-urile înaintea curățării. Măsoară separat pornirea la rece, staging-ul, testele, Extent, evaluatorul și distribuția. Raportează suprapunerile fără adunarea lor ca timp pe traseul critic. |
| RAG-07 | Publicarea depinde de sincronizarea dintre commit, draft, tag, asset-uri și catalog. Un commit local încă inaccesibil GitHub poate primi 422. | Folosește un jurnal atomic cu commit, ID de draft și hash-uri. Probe controlate acoperă 422, draft creat dar nevizibil după tag, upload acceptat cu răspuns pierdut și reluare după promovare. Verifică README-ul și fișierul real înaintea rotației. |

## Întreținerea regresiilor

[Testul evaluatorului](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagEvaluatorComponentTest.java) concentrează contracte distincte într-o clasă. Separați cazurile pentru proveniența componentelor, izolarea Android și distribuție. Păstrați ID-urile și controalele negative din rapoarte prin comparația inventarului înainte și după extragere.

## Unde se verifică

- [Calificarea livrării](../testing/ATF_ReleaseQualification.md#reutilizarea-dovezilor-de-componentă) deține regulile de reutilizare.
- [Predarea rapoartelor](../reporting/ATF_Handoff.md) deține autonomia HTML și republicarea.
- [Livrarea Android](../platform/ATF_AndroidDelivery.md#commit-ul-surselor-și-commit-ul-dovezilor) deține ordinea publicării.
