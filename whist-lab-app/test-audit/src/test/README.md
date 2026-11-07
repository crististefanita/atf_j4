# Componentele adaptorului de audit Gradle

## Pe scurt

Testele verifică legarea fiecărei invocări de intrările și comparațiile ei, contractul matricilor exhaustive și refuzul dovezilor inconsistente. [Strategia comună](../../../../docs/testing/README.md)

| Familie | Oracol |
|---|---|
| Punte Gradle | Identitățile invocărilor, intrările și rezultatele capturate pot fi confruntate cu așteptări independente. |
| Matrice | Agregarea păstrează aria evaluată și face vizibilă prima nepotrivire. |

Sarcina `:test-audit:test` verifică adaptorul pe JVM. [Fixture-urile consumate de module](../testFixtures/README.md)
