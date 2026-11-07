# Componentele bibliotecii pentru masa de joc

## Pe scurt

Testele JVM verifică geometria mâinii, modelele de prezentare și frontiera bibliotecii reutilizabile. Oracolele compară valori calculate și semnături de dependență. [Strategia comună](../../../../docs/testing/README.md)

| Familie | Proprietate verificată |
|---|---|
| Masă și cărți | Geometria, ordinea și prezentarea pentru intrări controlate. |
| Locuri și meniu | Conținutul modelului de prezentare, inclusiv editarea numelui. |
| Frontieră | Biblioteca de interfață are contracte independente de motorul Whist. |

Sarcina `:card-table-ui:testDebugUnitTest` rulează pe JVM. Validarea vizuală pe dispozitiv aparține fluxurilor aplicației. [Colecțiile proiectului](../../../../docs/testing/ATF_Collections.md)
