# Calificarea publicării și a predării rezultatelor

## Responsabilitate

Consumatorul deține înregistrările jocului, selecția, oracolul și indexarea externă. Defectele generice de API sau randare urmează [rutarea către responsabilul bibliotecilor](README.md).

## Lacune de calificare

| Prioritate | Limită | Criteriu de închidere |
|---|---|---|
| P1 | Un tip de republicare descrie date capturate anterior. | Execuția Android nouă are identitate și criterii proprii; datele republicate păstrează explicit proveniența și intervalele originale. |
| P1 | Indexul Java al ultimei publicări este un instrument de navigare. | Acceptarea unui scop se stabilește prin descriptorul și verificările sale, cu referințe separate pentru încercare și acceptanță. |
| P1 | Publicatorii de date istorice au suprafețe diferite. | Calificați fiecare producător folosit prin inventarul comparațiilor originale, ultimul rând, proprietarul capturii și refuzul datelor corupte. |

[WhistExtentFeedback](../../atf-test-android/src/main/java/atf/android/whist/evidence/WhistExtentFeedback.java) scrie indexul extern după audit. [Mecanismul de predare](../reporting/ATF_Handoff.md) definește acceptarea pe scop; un index nu este registrul acelei acceptări.

## Criterii pentru înregistrare și retenție

- Identificați aplicația, scenariul, sursele și bibliotecile execuției originale.
- Păstrați încercarea neacceptată și cauza ei fără modificarea ultimei acceptanțe.
- Verificați caracterul idempotent al reimportului, refuzul modificării aceluiași identificator și actualizarea atomică sub un singur blocaj.
- Protejați țintele manifestelor și referințelor înaintea arhivării.
- Transmiteți rădăcina externă explicit, independent de copia temporară și de descoperirea Git.

## Criterii pentru conținut

- Confruntați comanda trimisă cu tranziția semantică observată; păstrați rezultatele lor distincte.
- Verificați PNG-ul integral și intervalul original la proprietarul corect, inclusiv după întrerupere.
- Refuzați scalarii lipsă, intervalele imposibile, referințele alterate și verdictul nativ inconsecvent.
- Separați auditul datelor, revizia interactivă și acceptarea vizuală; consemnați numai nivelul executat.
- Publicarea și indexarea nu rescriu documentația editorială.

[Contractul Android](../reporting/ATF_Android.md) · [Auditul publicării](../reporting/ATF_Audit.md) · [Retenție](../learning/ATF_Retention.md)
