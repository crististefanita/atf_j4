# Reguli, YAML-uri și ponderi dependente de context

O regulă definește condiția, acțiunea și limitele ei. O pondere are sens numai împreună cu caracteristica măsurată, unitatea, consumatorul din cod și criteriul după care este calificată.

## Tipuri de configurare

| Tip | Contract | Efect |
|---|---|---|
| Reguli obligatorii | Pachet, licitații permise, culoare, atu și scor | Filtrează acțiuni și calculează rezultate în motor. |
| Manifest legat de cod | Condiții, priorități, surse și teste | Documentează și verifică implementarea declarată. |
| Parametru de estimator | Buget, mix de continuare sau prag de departajare | Influențează calculul numai prin consumatorul implementat. |
| Catalog de tipare | Clase de combinații, indicii și statut | Descriere pentru cercetare, cu activare runtime explicită. |
| Model învățat | Caracteristici, coeficienți, dispersie și identitate | Produce estimări prin cod generat sau încărcare definită. |
| Date de experiment | Partide, selecții concrete și rezultate | Intrări externe pentru dezvoltare și evaluare. |

## Cine consumă manifestele

```mermaid
flowchart LR
    RULES[Manifest de reguli] -->|leagă condiții de| CODE[Cod și teste]
    PATTERN[Catalog tipare] -->|descrie| RESEARCH[Cercetare]
    MODEL[Model declarat] -->|generează controlat| GENERATED[Implementare experimentală]
    GENERATED -->|necesită calificare| PROMOTION[Promovare]
```

<details>
<summary>Legendă: declarație și execuție</summary>

Manifestele AI curente nu formează un interpretor general de reguli YAML. Fiecare efect executabil are o cale de consum explicită și un statut de activare; editarea unui text declarativ nu modifică de la sine politica aplicației.

</details>

| Manifest | Rol |
|---|---|
| [Obiective Normal/Hard](../config/whist-play-objectives.yaml) | Priorități active, condiții de schimbare a obiectivului și profiluri experimentale. |
| [Reguli contextuale](../config/whist-contextual-discard.yaml) | Necesare, oportunități, discard, informație publică și legături către componente. |
| [Catalog de tipare](../config/whist-hand-patterns.yaml) | Clase de combinații și indicii neaditive, fără influență runtime directă. |
| [Salvare certificată](../config/whist-contract-salvage.yaml) | Domeniu, limite universale și fallback-ul gărzii active. |
| [Presiune contextuală](../config/whist-contract-pressure.yaml) | Model de continuare experimental, separat de politica acceptată. |
| [Inferență din licitații](../config/whist-public-beliefs.yaml) | Model experimental de comportament observat și contractul generării sale. |
| [Licitații viitoare](../config/whist-future-bid-forecast.yaml) | Experiment de propagare a prefixului ipotetic al licitației. |
| [Legalitatea situațiilor](../config/whist-legal-evidence.yaml) | Contractul martorului legal și al observației disponibile testului. |

## Cum se schimbă influența unei reguli

| Mecanism | Ce se schimbă în joc | Ce rămâne fix în politica înghețată |
|---|---|---|
| Activare condițională | Condiția poate deveni adevărată sau falsă după o carte. | Regula și ordinea priorităților. |
| Caracteristică contextuală | Necesar, oportunități, ranguri public eliminate, lipsuri și prefix. | Definiția și normalizarea caracteristicii. |
| Poartă de obiectiv | Contract recuperabil sau imposibil public. | Criteriul care dovedește schimbarea stării. |
| Ponderare de model | Plauzibilitatea lumilor se recalculează din observație. | Coeficienții și mecanismul de regularizare. |
| Învățare offline | Un candidat poate schimba parametri sau implementare. | Politica acceptată până la o promovare explicită. |

## Modelul formal al influenței

```text
scor_contextual(acțiune, stare) =
  suma_j θ_j × φ_j(acțiune, stare)

θ_j = parametru al unei politici identificate
φ_j = caracteristică definită și normalizată din informația permisă
```

Această formă descrie un model aditiv numai acolo unde codul îl implementează. Prioritățile lexicografice și filtrele de legalitate au alte semantici, iar indiciile care se suprapun nu se însumează automat.

Modelul experimental de [presiune](../whist-lab-app/game-core/src/test/java/ro/whistlab/core/ContractPressure.java) ajustează densitatea prin dezechilibru, progres și oportunități. El are ramuri distincte pentru necesar zero, rezervă suficientă și necesitatea tuturor levatelor, cu ponderi fixe asupra unor caracteristici care evoluează.

```text
ρ = clamp(necesar / oportunități, 0, 1)
progres = 1 − oportunități / mărimea_inițială
ρ_contextual = clamp(ρ + β × (1 + progres) × Δ / oportunități, 0, 1)
```

Corecția contextuală se aplică doar în ramura eligibilă a modelului experimental. Formula nu este activă implicit în rollout-ul Hard acceptat, iar dezechilibrul semnat Δ rămâne constant în timpul unei mâini.

## Inferența comportamentului

[Modelul experimental de licitații](../research/ai-bid-models/shared/game-core/src/main/java/ro/whistlab/core/LearnedBidEvidence.java) folosește coeficienți fixați și caracteristici ale mâinii ipotetice originale și prefixului disponibil atunci. Cărțile rămase și cele jucate de același loc se reunesc fără dublarea mesei curente.

```text
μ = clamp(scală × θ · φ(mână_inițială, prefix), 0, mărime)
p(licitare legală) =
  masă_uniformă / număr_licitații_legale
  + masă_model × distribuție_normalizată(licitare, μ, dispersie)

pondere_lume =
  limitare(produsul rapoartelor de plauzibilitate ale licitațiilor observate)

eșantion_efectiv = suma_ponderilor² / suma_pătratelor_ponderilor
```

- Regula ultimei licitații elimină numai valoarea interzisă legal.
- Masa uniformă păstrează suport pentru comportamente neobișnuite, inclusiv contracte mici cu cărți mari.
- Concentrarea excesivă sau ponderile constante păstrează estimarea de bază prin fallback.
- O licitație proprie ipotetică este țintă de simulare și nu este folosită retrospectiv drept dovadă despre un adversar.
- Modelul prezice comportament observat, fără etichetarea lui ca optim sau posterior comun calibrat.

## Licitațiile viitoare în simulare

Licitațiile deja depuse rămân fapte publice, iar contractul propriu evaluat este ținta ipotetică a acelei alternative. Estimarea următorilor actori folosește ordinea licitării și prefixul disponibil în continuarea simulată.

- Politica acceptată folosește estimatorul din `MonteCarloAi.biddingTargets` pentru contractele încă necunoscute.
- [Modelul condiționat experimental](../research/ai-bid-models/r16-bid-forecast/game-core/src/main/java/ro/whistlab/core/BidForecast.java) actualizează prefixul după fiecare predicție și respectă restricția ultimului ofertant.
- Alegerea modului unei distribuții produce o singură continuare a licitației; integrarea tuturor licitațiilor viitoare este un mecanism diferit.
- O predicție este context de simulare și nu devine observație reală sau dovadă despre cărțile adverse.

## Contractul unei reguli întreținute

| Câmp | Responsabilitate |
|---|---|
| Identitate și statut | Regulă stabilă, activă sau de cercetare. |
| Domeniu și precondiții | Variantă, observație permisă și condiție de aplicare. |
| Semnificație | Filtru, prioritate, scor, estimator sau certificat. |
| Parametri | Unități, intervale, normalizare și sursa valorii. |
| Limite și fallback | Comportament în afara domeniului, fără date sau la buget epuizat. |
| Cod și verificare | Consumator explicit, componentă independentă și criteriu de promovare. |

[Modelul deciziei](AI_DECISION_MODEL.md) · [Protocolul de îmbunătățire](AI_TRAINING_PROTOCOL.md)
