# Componentele bibliotecii pentru masa de joc

## Pe scurt

Testele JVM verifică geometria mâinii, modelele de prezentare și frontiera bibliotecii reutilizabile. Oracolele compară valori calculate și semnături de dependență. [Strategia comună](../../../../docs/testing/README.md)

| Familie | Proprietate verificată |
|---|---|
| Masă și cărți | 3–6 locuri, bandă centrală, compactare, însemne și marcaj la densitate fracționară. |
| Locuri și meniu | Conținutul modelului de prezentare, inclusiv editarea numelui și o înălțime comună a badge-ului pentru etichetele celor două faze. |
| Realizări | Ordinea catalogului, cheile de restaurare și calculul detaliului la deschidere. |
| Frontieră | Inventarele de producție, JVM și instrumentare exclud dependențele de joc. |

- Sarcina `:atf-card-table-ui:testDebugUnitTest` rulează pe JVM.
- Opțiunea `-Pcardui.only=component` configurează biblioteca și puntea auditului. Lansatorul RAG o folosește când biblioteca este singura selecție Gradle.
- Validarea vizuală pe dispozitiv aparține fluxurilor aplicației. [Colecțiile proiectului](../../../../docs/testing/ATF_Collections.md)
