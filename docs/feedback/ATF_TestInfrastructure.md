# Calificarea infrastructurii locale de testare

## Responsabilitate

Responsabilii Kitului și ai validării API dețin aceste criterii ale adaptoarelor locale. Contractele implementate rămân în README-urile modulelor; biblioteca binară și acceptanța Android au verificări separate.

## Concurența Kitului

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | [ScopedComponentAuditExtensionComponentTest](../../atf-test-kit/src/test/java/atf/testkit/assertions/ScopedComponentAuditExtensionComponentTest.java) verifică ciclul Jupiter cu paralelism dezactivat. | Clase și metode Jupiter concurente: o singură creare și închidere pe proprietar, audit separat pe invocare; eșecul pregătirii ori publicării păstrează cauza inițială. |
| P2 | Monitorul [ScopedAcceptanceRun](../../atf-test-kit/src/main/java/atf/testkit/lifecycle/ScopedAcceptanceRun.java) serializează utilizatorii propriei metode, fără izolarea cititorilor externi ai proprietății globale. | Componentă cu cititor concurent și eroare injectată: restaurarea proprietății și limita izolării sunt explicite înaintea extinderii suportului paralel. |

## Costul validării API

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | [JsonSchemaCapabilities](../../atf-validation/src/main/java/atf/validation/contract/schema/JsonSchemaCapabilities.java) și [JsonSchemaValidator](../../atf-validation/src/main/java/atf/validation/contract/JsonSchemaValidator.java) nu declară un buget al adâncimii și volumului evaluării. | Probe controlate pentru adâncime, cardinalitate și repetarea regulilor; mediu și intrare identificate, refuz explicit și limite documentate în contract. |

[Kitul testelor](../../atf-test-kit/README.md) · [Validarea API](../../atf-validation/README.md) · [Guvernanță](ATF_Governance.md)
