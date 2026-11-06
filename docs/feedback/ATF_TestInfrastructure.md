# Calificarea infrastructurii locale de testare

## Responsabilitate

Consumatorul deține validarea API, procesele și adopția locală; [registrul responsabilităților](README.md#suportul-comun-al-testelor) rutează primitivele Kit. Contractele implementate rămân în README-urile modulelor; biblioteca binară și acceptanța Android au verificări separate.

## Concurența Kitului

[Suportul comun](README.md#suportul-comun-al-testelor) deține primitivele și probele generice. Consumatorul păstrează acceptanța adaptorului Android; transferul nu califică paralelismul și nu închide criteriul.

## Costul validării API

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | [JsonSchemaCapabilities](../../atf-validation/src/main/java/atf/validation/contract/schema/JsonSchemaCapabilities.java) și [JsonSchemaValidator](../../atf-validation/src/main/java/atf/validation/contract/JsonSchemaValidator.java) nu declară un buget al adâncimii și volumului evaluării. | Probe controlate pentru adâncime, cardinalitate și repetarea regulilor; mediu și intrare identificate, refuz explicit și limite documentate în contract. |

## Stabilitatea verificării proceselor

| Prioritate | Limită de calificare | Criteriu de închidere |
|---|---|---|
| P2 | [CommandProcessComponentTest](../../atf-test-android/src/test/java/atf/android/process/CommandProcessComponentTest.java) combină sincronizarea explicită cu praguri strânse ale timpului real. | Separați pregătirea subprocesului de intervalul transportului și proba de terminare/curățare de calificarea latenței pe mediul identificat. Păstrați termenul produsului și controalele negative; nu relaxați pragurile doar pentru a obține PASS. |

[Kitul testelor](../../lib/README.TestKit.md#scope-și-integrare) · [Validarea API](../../atf-validation/README.md) · [Guvernanță](ATF_Governance.md)

## Compoziția acțiunilor driverului

| Prioritate | Lacună și responsabil | Criteriu de închidere |
|---|---|---|
| P2 | Adaptorul Android: [AndroidUiDriver](../../atf-test-android/src/test/java/atf/android/e2e/driver/AndroidUiDriver.java) păstrează împreună selecția controalelor, repetarea comenzilor și înregistrarea succesului ori eșecului acțiunii. Blocurile similare cresc costul schimbării atribuirii și al temporizării. | Separați execuția și înregistrarea comună a acțiunii într-un colaborator cu porturi explicite, păstrând fațada driverului și oracolul de domeniu. Componente cu proces și ceas controlate demonstrează aceeași ordine a comenzilor, cauză originală, intervale și atribuire setup/body, inclusiv timeout și anulare. |
