# Lacune active în calificarea RAG a livrărilor

## Pe scurt

[Ghidul de calificare](../testing/ATF_ReleaseQualification.md) deține procesul, iar [evaluatorul](../../atf-tests/tools/evaluate-release-rag.mjs) verifică dovezile. Aici rămân lucrările deschise care afectează durata sau reluarea unei calificări.

## Dovezile cu valabilitate limitată și publicarea

| ID | Lacună | Criteriu de închidere |
|---|---|---|
| RAG-03 | Prospețimea receipt-urilor. Chitanțele sunt legate prin hash. Semnificația vechimii și expirarea dovezilor rămân nedefinite pentru preflight, artefacte și verificarea remote. | Definește ce expiră și în ce condiții. Separă dovezile imuabile de starea schimbătoare a dispozitivului sau a GitHub. Verifică starea expirată, timpul necunoscut și datele lipsă. |

## Întreținerea regresiilor

[Testul evaluatorului](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagEvaluatorComponentTest.java) concentrează contracte distincte într-o clasă. Separați cazurile pentru proveniența componentelor, izolarea Android și distribuție. Păstrați ID-urile și controalele negative din rapoarte prin comparația inventarului înainte și după extragere.

## Unde se verifică

- [Cronologia calificării](../testing/ATF_ReleaseQualification.md#cronologia-calificării) și [verificarea Git înaintea curățării](../platform/ATF_ReleaseRetention.md#curățarea-locală-după-livrare) dețin contractele operaționale.

- [Calificarea livrării](../testing/ATF_ReleaseQualification.md#reutilizarea-dovezilor-de-componentă) deține regulile de reutilizare.
- [Predarea rapoartelor](../reporting/ATF_Handoff.md) deține autonomia HTML și republicarea.
- [Livrarea Android](../platform/ATF_AndroidDelivery.md#publicarea-reluabilă-pe-github) deține publicarea și condițiile de reluare.
