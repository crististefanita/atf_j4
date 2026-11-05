# Cercetarea și calificarea jucătorilor

## Responsabilitate

Responsabilul politicilor Whist deține aceste criterii de cercetare și calificare. Regulile deja implementate sunt descrise în [modelul deciziei](../ai/ATF_DecisionModel.md), iar promovarea urmează [protocolul offline](../learning/README.md).

## Lacune și limite de calificare

| Prioritate | Subiect | Stare și criteriu de închidere |
|---|---|---|
| P1 | Inferența cărților la 2–8 cărți | Ponderarea generală după comportamentul fiecărui adversar rămâne experimentală. Activarea cere model identificat, suport legal, regularizare și progres separat pe loturi noi. Modelul activ de o carte are domeniu separat. |
| P1 | Interpretarea probabilităților | Eșantionarea fezabilă și o medie de simulare nu certifică uniformitate sau calibrare. Declarați distribuția, bugetul și incertitudinea înainte de a folosi estimarea ca probabilitate. |
| P1 | Cost pe mediul țintă și progres strategic | [Calificarea JVM](../ai/ATF_CostQualification.md) deține componentele, bugetele și măsurarea pe poziții generate la 2–8 cărți. Latența și consumul pe dispozitiv cer profil Android identificat; progresul strategic cere evaluare separată pe rol, adversar și loc, cu surse înghețate. Niciunul nu se deduce din costul JVM. |
| P2 | Reproductibilitate între contexte | Scorurile absolute influențează inițializarea estimatorului. O eventuală invarianță la translația comună a scorurilor cere o cerință explicită și o calificare separată. |

## Coordonarea evaluării și delimitarea mecanismelor

| Prioritate | Lacună demonstrabilă în sursă | Criteriu de închidere |
|---|---|---|
| P2 | [Manifestul obiectivelor](../../config/whist-play-objectives.yaml) distinge modelul de o carte, dar coerența lui cu domeniul runtime nu are control automat propriu. | Componentă care detectează omiterea sau inversarea domeniului, cu sursa și testele modelului identificate; coeficienții și comportamentul calificat se păstrează. |
| P2 | Selecția generică de focus după categorii nu are un consumator calificat în sursele curente. | Selector cu manifest părinte, subset și hash-uri verificate, criterii de acoperire și refuzul loturilor rezervate; până atunci rămâne propunere. |

Contractele implementate sunt explicate în [modelul deciziei](../ai/ATF_DecisionModel.md), [colecții](../testing/ATF_Collections.md) și [selecția pentru învățare](../learning/ATF_Selection.md). Aceste criterii rămân deschise până la verificarea mecanismului cerut.

[Registrul comun de expunere](../learning/ATF_EvaluationRegistry.md) deține rezervarea și refuzul suprapunerilor între corpusuri. Implementarea registrului și criteriul de progres strategic sunt contracte distincte; disponibilitatea unui lot nu demonstrează superioritatea politicii.

## Verificarea autonomă a laboratorului

Responsabilul modulului AI deține intrările și verificarea proprie. Testele de integrare din motor rămân utile, fără să înlocuiască această responsabilitate.

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | Costul și recuperarea uneltelor de corpus, selecție și compoziție cer calificare proprie, separată de registrul expunerii. | Componente cu duplicate, intrări incomplete, scriere întreruptă ori concurentă și volume controlate; buget și mediu declarate, cu păstrarea provenienței. |

## Evoluția motorului

| Prioritate | Responsabil și lucrare | Criteriu de închidere |
|---|---|---|
| P2 | Politica Hard: separarea evaluării alternativelor de compoziția din [MonteCarloAi](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/engine/MonteCarloAi.java). | Extrageri justificate prin invariantă proprie; componentele păstrează ordinea eșantionării, inițializarea deterministă și așteptări independente. |
| P2 | Motorul: calificarea frontierelor prin mutanți semantici. | Inversarea obligației de atu, a câștigătorului sau a întreruperii seriei este detectată independent; acoperirea JaCoCo se interpretează separat de corectitudine. |

## Contractele calificării

Lucrările deschise de mai sus se verifică prin contractele responsabile; o schimbare de documentație nu califică politica.

| Criteriu obligatoriu | Contract și frontieră |
|---|---|
| Informație și model | [Inferența comportamentului](../ai/ATF_RulesConfiguration.md#inferența-comportamentului): reconstrucție fără dublare, fără viitor, cărți neîmpărțite posibile și suport fără observații, constant sau concentrat. Factorii individuali nu certifică distribuția comună a adversarilor. |
| Independența mecanismelor | [Modelul deciziei](../ai/ATF_DecisionModel.md): separați restricția legală, plauzibilitatea voluntară, continuarea și utilitatea; o probă a helperului rămâne limitată la acel API. |
| Garanție și rezervă | [Componentele AI](../testing/ATF_AiComponents.md): observație permisă, cuantificatori independenți și comportament explicit în afara domeniului ori la buget epuizat. |
| Promovare și date | [Protocolul offline](../learning/README.md#ciclul-de-îmbunătățire): înghețare, separarea ajustării de evaluare și toate criteriile declarate; rezultate și decizie păstrate extern. |

Catalogul de onoruri și calibrarea descriptivă au [unități și precondiții proprii](../ai/ATF_HandPatternsRo.md). Coerența lor documentară și medierea grupurilor nu închid criteriul probabilităților calibrate sau progresul strategic.

[Reguli și ponderi](../ai/ATF_RulesConfiguration.md) · [Testarea componentelor AI](../testing/ATF_AiComponents.md) · [Rutarea responsabilităților](README.md)
