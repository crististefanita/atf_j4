# Testele infrastructurii comune

## Pe scurt

Aceste teste califică primitivele Kitului, separat de regulile Whist și de randarea bibliotecii de raportare. [Strategia comună](../../../docs/testing/README.md) deține nivelul verificării; [contractul Kitului](../../README.md) deține API-ul și responsabilitățile.

## Familii și oracole

| Familie | Proprietate verificată |
|---|---|
| Configurare și selecție | Descoperire, conversie și refuzul configurațiilor ambigue ori incomplete. |
| Scope și resurse | Proprietate pe clasă, suită sau invocare, inițializare și închidere controlate. |
| Aserțiuni și proiecție | Verdictul JUnit original, valorile capturate, redactarea și preview-ul limitat. |
| Ciclul JUnit | Izolarea invocărilor, fazele, eșecurile și întreruperile prin lansări Jupiter locale. |
| Căi și timp | Directorul de output, recuperarea și intervalele observate fără identități inventate. |

## Execuție și limite

[Catalogul primar](../../../config/atf-test-suites.json) separă `KitUnitSuite` de `KitComponentSuite`. [Agregarea](../../../docs/reporting/ATF_Audit.md#agregarea-componentelor-pe-sursa-live) execută numai componentele și confruntă identitățile native cu cele din HTML; fixture-urile Jupiter lansate în interiorul unui test rămân probe controlate.

Dintr-o copie temporară cu POM-ul părinte și dependențele identificate, rulați [testele modulului](../../README.md#niveluri-și-verificare). Clasele-fixture cu eșecuri intenționate sunt lansate de componentele responsabile; nu se selectează ca teste de produs.

Testele unitare califică politica locală. Componentele ciclului folosesc JUnit real; probele concurente califică numai combinația de proprietar și execuție efectiv verificată. [Calificarea concurenței](../../../docs/feedback/README.md#suportul-comun-al-testelor) are responsabil separat.

[Kitul testelor](../../README.md) · [Strategia comună](../../../docs/testing/README.md)
