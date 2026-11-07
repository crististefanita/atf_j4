# Fixture-uri comune pentru auditul Gradle

## Pe scurt

Acest set de surse furnizează infrastructură de test pentru modulele Gradle. Testele componente aleg intrările și oracolele, iar adaptorul înregistrează comparațiile și confruntă raportul cu invocările JUnit. [Strategia comună](../../../../docs/testing/README.md)

| Responsabilitate | Contract |
|---|---|
| Captură | Păstrează valoarea efectivă și momentul capturii pentru intrările declarate. |
| Comparații | Asociază fiecare verificare cu invocarea care a evaluat-o. |
| Verificare | Confruntă identitățile și rezultatele între HTML și JUnit. |

Fixture-urile intră în variantele de test ale consumatorilor. Verificarea lor proprie este în [catalogul de componente](../test/README.md).
