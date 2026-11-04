# Arhitectura jucătorilor AI

## Pe scurt

Politica transformă mâna proprie și informația publică într-o decizie legală, potrivită contractului rămas. Algoritmul și parametrii unei politici sunt identificați; îmbunătățirea lor urmează separat [ciclul de calificare offline](../learning/README.md).

## Ghiduri complementare

| Mecanism | Ghid |
|---|---|
| Observație, simulare, utilitate și certificate | [Modelul deciziei](ATF_DecisionModel.md) |
| Rolul manifestelor și influența contextului | [Reguli și configurare](ATF_RulesConfiguration.md) |
| Necesar, oportunități și ieșiri legale | [Joc contextual](ATF_ContextualPlay.md) |
| Onoruri și acoperiri, cu statut de cercetare | [Română](ATF_HandPatternsRo.md) și [engleză](ATF_HandPatternsEn.md) |

## Decizia din timpul jocului

```mermaid
flowchart LR
    STATE[Stare de joc] -->|proiectează| OBS[Observație proprie/publică]
    OBS -->|alimentează| POLICY[Politică acceptată]
    POLICY -->|propune| ACTION[Acțiune]
    ACTION -->|validează și aplică| STATE
```

<details>
<summary>Legendă: decizia din timpul jocului</summary>

Observația conține mâna actorului, contractele cunoscute, cărțile publice, necesarul și scorurile. Motorul verifică legalitatea acțiunii înainte de schimbarea stării; mâinile reale ale adversarilor nu intră în politica de decizie.

</details>

## Straturi și surse de adevăr

| Strat | Responsabilitate | Implementare |
|---|---|---|
| Domeniu | Legalitate, distribuire, tranziții și scor | [game-core](../../whist-lab-app/game-core/README.md) |
| Politică runtime | Licitare și alegerea cărții din observația permisă | `AiPolicy`, politicile Normal și `MonteCarloAi` |
| Politică independentă | Model separat pentru verificare și comparații | [ai-lab](../../whist-lab-app/ai-lab/README.md) |
| Corpus | Validarea jocurilor și selecția explicită a datelor de dezvoltare | [whist-corpus](../../scripts/whist-corpus.mjs), [whist-active-corpus](../../scripts/whist-active-corpus.mjs) |
| Tipare | Descrierea combinațiilor și calibrare descriptivă | [catalog și limite](ATF_HandPatternsRo.md) |
| Experiment | Copie izolată, candidat identificat, comparații pereche | [runnerul colecțiilor](../../scripts/run-whist-collection.ps1), [arena](../learning/ATF_Arena.md) |
| Promovare | Criterii fixate, lot neatins și păstrarea gărzilor de domeniu | [protocolul evaluării](../learning/README.md) |

## Rolul YAML-urilor

| Manifest | Semnificație | Efect asupra execuției |
|---|---|---|
| [Obiective](../../config/whist-play-objectives.yaml) | Priorități Normal/Hard, condiții și teste asociate | Documentează comportamentul implementat; nu interpretează reguli arbitrare în timpul jocului. |
| [Reguli contextuale](../../config/whist-contextual-discard.yaml) | Deducții certe, limite și ipoteze de cercetare | Leagă fiecare regulă de implementare și componentă. |
| [Tipare de cărți](../../config/whist-hand-patterns.yaml) | Indicii contextuale cu statut explicit | Catalogul declară influență runtime zero până la un consumator implementat și promovat. |

O pondere are nevoie de definiția caracteristicii, domeniul ei, contextul de aplicare și un consumator de cod. Un fișier editat nu modifică implicit algoritmul; proprietatea `research_only` păstrează ipoteza în afara politicii acceptate.

## Datele susțin mecanismul

- Schema, validatoarele, selecția și criteriile aparțin surselor proiectului.
- Partidele, calibrările, selecțiile concrete, registrele loturilor și rezultatele sunt date externe.
- [Configurația corpusului](../../config/learning-corpus.json) indică arhiva și pointerul activ; analiza obișnuită citește numai manifestul selectat și verificat.
- Selecția elimină urmele duplicate, păstrează originile protejate și urmărește categorii noi în limita bugetului.
- O categorie observată măsoară varietatea setului, fără să demonstreze acoperirea codului sau optimalitatea unei acțiuni.
- Regresia implicită păstrează cazuri mici generate sau construite pentru o proprietate generală, cu așteptare independentă.

## Interpretarea adversarilor

```mermaid
flowchart LR
    PUBLIC[Istoric public] -->|deduce legal| VOID[Restricții certe]
    PUBLIC -->|sugerează| MODEL[Ipoteze de comportament]
    VOID -->|elimină lumi| WORLDS[Lumi compatibile]
    MODEL -->|poate pondera| WORLDS
    WORLDS -->|alimentează| PLAN[Estimarea continuărilor]
```

<details>
<summary>Legendă: certitudine și estimare</summary>

Obligația de culoare sau atu permite deducții despre lipsa unei culori. Licitația și o alegere voluntară sugerează intenții numai printr-un model declarat; o carte jucată forțat nu dovedește intenția de a câștiga sau de a evita levata.

</details>

Hard folosește contractele cunoscute drept ținte ale simulărilor și constrânge mâinile ipotetice prin cărțile publice și lipsurile deduse. Ponderarea generală a mâinilor ascunse prin probabilitatea licitației observate rămâne o ipoteză de cercetare, fără activare implicită în politica acceptată.

## Condiții pentru progres

1. Testul local dovedește proprietatea și cazurile în care ea încetează să fie valabilă.
2. Comparația pereche controlează distribuția, locul, adversarii și bugetul de calcul.
3. Rezultatul se separă după rol, loc și mărimea mâinii; un agregat favorabil nu compensează un criteriu obligatoriu eșuat.
4. Candidatul și pragurile sunt înghețate înaintea lotului rezervat.
5. Schimbările concurente invalidează calificarea copiei afectate și cer o nouă identitate de sursă.
6. Promovarea actualizează codul, manifestele, regresiile și documentația generală în aceeași revizie.

## Limite de transfer către alte sisteme

| Abstracție reutilizabilă | Adaptor specific domeniului |
|---|---|
| Proiecția observației | Ce informație este permisă fiecărui actor. |
| Filtrul de acțiuni | Legalitatea definită de varianta jocului. |
| Reducerea istoricului | Faptele și restricțiile deduse din acțiunile publice. |
| Generatorul de lumi | Distribuțiile compatibile cu observația și ipotezele samplerului. |
| Utilitatea și prioritățile | Contractul, scorul, seria și obiectivul politicii. |
| Verificarea și promovarea | Proprietăți independente, comparații controlate și criterii fixate. |

[Modelul deciziei](ATF_DecisionModel.md), [regulile și ponderile](ATF_RulesConfiguration.md) și [proiectarea testelor](../testing/ATF_AiComponents.md) detaliază aceste limite fără a depinde de o partidă concretă.
