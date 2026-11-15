# Lacune active în calificarea RAG a livrărilor

## Pe scurt

[Ghidul de calificare](../testing/ATF_ReleaseQualification.md) deține procesul, iar [evaluatorul](../../atf-tests/tools/evaluate-release-rag.mjs) verifică dovezile. Aici rămân lucrările deschise care afectează durata sau reluarea unei calificări.

## Întreținerea regresiilor

| ID | Lacună | Criteriu de închidere |
|---|---|---|
| RAG-08 | [Testul evaluatorului](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagEvaluatorComponentTest.java) concentrează fixture-uri de proveniență, izolare Android și distribuție. | Extrage fixture-urile pe contract, păstrează cazurile negative și compară inventarul cazurilor și al rezultatelor Extent înainte și după. |

Expirarea observațiilor este definită în [contractul de calificare](../testing/ATF_ReleaseQualification.md#valabilitatea-dovezilor).

## Unde se verifică

- [Cronologia calificării](../testing/ATF_ReleaseQualification.md#cronologia-calificării) și [verificarea Git înaintea curățării](../platform/ATF_ReleaseRetention.md#curățarea-locală-după-livrare) dețin contractele operaționale.
- [Calificarea livrării](../testing/ATF_ReleaseQualification.md#reutilizarea-dovezilor-de-componentă) deține regulile de reutilizare.
- [Predarea rapoartelor](../reporting/ATF_Handoff.md) deține autonomia HTML și republicarea.
- [Livrarea Android](../platform/ATF_AndroidDelivery.md#publicarea-reluabilă-pe-github) deține publicarea și condițiile de reluare.
