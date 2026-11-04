# Navigarea și costul rapoartelor mari

Navigarea păstrează conținutul obligatoriu și limitează lucrul de pregătire la suprafețele vizibile. Bugetul inputului și costul prezentării sunt responsabilități distincte.

## Mecanisme

| Suprafață | Contract |
|---|---|
| Selecție | Consumerul limitează contextul opțional și păstrează invocări întregi, cu omisiuni declarate. |
| Tabele ascunse | Măsurarea geometriei și alocarea coloanelor sunt amânate până la vizibilitate. |
| Actualizări | Modificările reale sunt grupate; operațiile de prezentare identice nu provoacă recalcul inutil. |
| Navigare | Schimbarea proprietarului pregătește secțiunile afectate fără rescanarea întregului document. |
| Capturi | Vizualizatorul păstrează PNG-ul original și contextul revenirii. |
| Timing | Execuția originală, pregătirea prezentării și interacțiunea sunt măsurători diferite. |
| Readiness | Starea pregătită apare după normalizarea reușită și este independentă de sfârșitul încărcării paginii. |

## Guvernanța performanței

- Un buget de caractere nu garantează o mărime totală fixă a HTML-ului sau a heap-ului browserului.
- Optimizarea navigării păstrează identitatea, ordinea și conținutul comparațiilor.
- Datele obligatorii și imaginile originale nu dispar ca efect secundar al unei optimizări.
- Costul se măsoară pe operații cu scop declarat și input identificat.
- Controalele de corupere verifică dacă modificarea conținutului este detectată.
- Implementarea generică se verifică la proprietarul bibliotecii; consumerul păstrează contractul pe care îl utilizează.

[Verificarea publicării](testing/reporting/README.md) · [Politica inputului](ATF_Game_Report_Presentation.md) · [Responsabilități](feedback/README.md)
