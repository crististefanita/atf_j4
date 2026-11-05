# Cercetarea și calificarea jucătorilor

## Responsabilitate

Responsabilul politicilor Whist deține aceste criterii de cercetare și calificare. Regulile deja implementate sunt descrise în [modelul deciziei](../ai/ATF_DecisionModel.md), iar promovarea urmează [protocolul offline](../learning/README.md).

## Lacune și limite de calificare

| Prioritate | Subiect | Stare și criteriu de închidere |
|---|---|---|
| P1 | Inferența cărților la 2–8 cărți | Ponderarea generală după comportamentul fiecărui adversar rămâne experimentală. Activarea cere model identificat, suport legal, regularizare și progres separat pe loturi noi. Modelul activ de o carte are domeniu separat. |
| P1 | Interpretarea probabilităților | Eșantionarea fezabilă și o medie de simulare nu certifică uniformitate sau calibrare. Declarați distribuția, bugetul și incertitudinea înainte de a folosi estimarea ca probabilitate. |
| P1 | Cost și progres strategic | Calificați licitarea și jocul pe mărimi de mână, roluri, adversari și locuri, cu surse înghețate și buget de calcul declarat. |
| P2 | Reproductibilitate între contexte | Scorurile absolute influențează inițializarea estimatorului. O eventuală invarianță la translația comună a scorurilor cere o cerință explicită și o calificare separată. |

## Coordonarea evaluării și delimitarea mecanismelor

| Prioritate | Lacună demonstrabilă în sursă | Criteriu de închidere |
|---|---|---|
| P1 | [Runnerul](../../scripts/run-whist-collection.ps1) rezervă intervalul sub corpusul transmis și verifică identitatea exactă a fișierului de rezervare. | Registru canonic comun, refuzul suprapunerilor parțiale și al expunerii din alte corpusuri, cu probe negative și concurente. |
| P2 | Manifestele agregate pot descrie lipsa inferenței din licitații fără a distinge modelul specializat de o carte. | Domeniu explicit pentru fiecare afirmație și componente care leagă manifestul de calea runtime; păstrarea coeficienților și a comportamentului calificat. |
| P2 | Selecția generică de focus după categorii nu are un consumator calificat în sursele curente. | Selector cu manifest părinte, subset și hash-uri verificate, criterii de acoperire și refuzul loturilor rezervate; până atunci rămâne propunere. |

Contractele implementate sunt explicate în [modelul deciziei](../ai/ATF_DecisionModel.md), [colecții](../testing/ATF_Collections.md) și [selecția pentru învățare](../learning/ATF_Selection.md). Aceste criterii rămân deschise până la verificarea mecanismului cerut.

## Criterii pentru modelul de informație

- Reconstruiți mâna ipotetică inițială fără dublarea cărților de pe masă și fără informație viitoare.
- Distingeți restricțiile certe din legalitate de intenția estimată din alegeri voluntare.
- Păstrați cărțile neîmpărțite posibile și verificați distribuții constante, concentrate ori fără observații.
- Separați influența modelului de adversar de schimbarea continuării sau a utilității.
- Tratați dependențele dintre adversari ca o ipoteză proprie; ponderile individuale de plauzibilitate nu demonstrează o distribuție comună calibrată.

## Criterii pentru politica rezultată

- Păstrați observația permisă și comportamentul de rezervă în afara domeniului certificat.
- Verificați cuantificatorii garanției și cazurile de buget epuizat prin componente independente.
- Păstrați evaluarea rezervată separată de ajustare și aplicați toate criteriile declarate înaintea lotului.
- Înregistrați rezultatele, comparațiile și decizia de promovare în arhiva externă.

[Reguli și ponderi](../ai/ATF_RulesConfiguration.md) · [Testarea componentelor AI](../testing/ATF_AiComponents.md) · [Rutarea responsabilităților](README.md)
