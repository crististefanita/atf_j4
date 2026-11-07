# Cercetarea și calificarea jucătorilor

## Responsabilitate

Responsabilul politicilor Whist deține aceste criterii de cercetare și calificare. Regulile deja implementate sunt descrise în [modelul deciziei](../ai/ATF_DecisionModel.md), iar promovarea urmează [protocolul offline](../learning/README.md).

## Lacune și limite de calificare

| Prioritate | Subiect | Stare și criteriu de închidere |
|---|---|---|
| P1 | Inferența cărților la 2–8 cărți | Ponderarea generală după comportamentul fiecărui adversar rămâne experimentală. Activarea cere model identificat, suport legal, regularizare și progres separat pe loturi noi. Modelul activ de o carte are domeniu separat. |
| P1 | Interpretarea probabilităților | Eșantionarea fezabilă și o medie de simulare nu certifică uniformitate sau calibrare. Declarați distribuția, bugetul și incertitudinea înainte de a folosi estimarea ca probabilitate. |
| P1 | Cost pe mediul țintă și progres strategic | [Calificarea JVM](../ai/ATF_CostQualification.md) deține componentele, bugetele și măsurarea pe poziții generate la 2–8 cărți. Latența și consumul pe dispozitiv cer profil Android identificat; progresul strategic cere evaluare separată pe rol, adversar și loc, cu surse înghețate. Niciunul nu se deduce din costul JVM. |
| P2 | Reproductibilitatea independentului între contexte | Hard folosește diferențe de scor în inițializarea estimatorului, conform [contractului deciziei](../ai/ATF_DecisionModel.md#reproductibilitate-și-limite). Jucătorul independent păstrează scorurile absolute în propriul seed; extinderea aceleiași invariante cere componente proprii și verificarea politicii independente. |

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

## Reutilizarea mecanismelor

[Contractul transferului](../learning/ATF_GameTransfer.md) deține inventarul reutilizabil, lecțiile și admiterea unui domeniu nou. Extragerea unor mecanisme suplimentare rămâne condiționată de un consumator și de contracte independente; pregătirea metodei nu califică automat un al doilea joc.

| Prioritate | Responsabil și frontieră | Criteriu de închidere |
|---|---|---|
| P2 | Laborator: identitatea sursei și admiterea evaluării combină contracte reutilizabile cu namespace-ul jocului. | Un al doilea adaptor păstrează proveniența, refuzul suprapunerii și recuperarea; migrarea registrului nu resetează expunerea și nu schimbă identitățile vechi implicit. |
| P2 | Motor și laborator: comparația politicilor depinde de scorul și observația Whist. | Un contract comun separă observația, acțiunea, metrica și oracolul; două adaptoare reale demonstrează paritatea și păstrează acțiunile independente de datele ascunse. |
| P2 | Testare produs nou: mock-ul poate conserva o ipoteză greșită despre cerință. | Registru cu stări confirmat/observat/presupus, controale negative și confruntare minimă cu sistemul real; o observație neconfirmată nu devine etichetă de succes. |

## Joc la 0

| Prioritate | Risc | Criteriu de închidere |
|---|---|---|
| P1 | Un AI care schimbă ținta când vede locul trei abandonează partide încă recuperabile. O limită care omite seriile sau mâna curentă poate produce aceeași eroare. | Păstrați pragul strict al celor doi adversari garantați înainte. Verificați egalitatea pentru locul doi, zero aritmetic imposibil, ultima licitație interzisă și penalizarea plătită în ultima mână. [Contractul deciziei](../ai/ATF_DecisionModel.md#când-ai-ul-joacă-la-0) |
| P2 | Ponderile abstracte pot favoriza o cale aritmetică greu de jucat. Un exemplu de componentă nu stabilește frecvența conversiei la 500 ori costul căutării. | Comparați partide noi pereche, cu ambele programe, 3–6 locuri și serii pornite/oprite. Măsurați conversiile la 500, schimbările de țintă, locurile în clasament și latența înainte de a declara progres strategic. |
| P2 | Ratele dintr-un prefix advers controlat supraestimează cât de frecvent apare ocazia în jocul liber. Eșantionarea poate lăsa fără sprijin o cale aritmetic posibilă. | Raportați lotul natural separat de lotul controlat, cu numitor, faza primei alegeri și conversii pentru fiecare nivel. Păstrați loturi mai mari cu rotații de dificultate și măsurați costul pentru mâinile în care se evaluează mai multe ținte. |

## Evoluția motorului

| Prioritate | Responsabil și lucrare | Criteriu de închidere |
|---|---|---|
| P2 | Motorul: calificarea frontierelor prin mutanți semantici. | Inversarea obligației de atu, a câștigătorului sau a întreruperii seriei este detectată independent; acoperirea JaCoCo se interpretează separat de corectitudine. |
| P2 | Motorul: [MonteCarloAi](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/engine/MonteCarloAi.java) coordonează licitarea, selecția cărții și stocarea mostrelor de licitație. | Extrageți gestionarea și reutilizarea mostrelor într-un colaborator intern, păstrând fațada publică și observația actorului. Componentele fixează ordinea consumului aleator, repetarea cu aceeași inițializare, anularea și bugetul; o refactorizare nu se declară progres strategic. |

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
