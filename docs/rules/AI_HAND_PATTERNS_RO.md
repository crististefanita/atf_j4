# Onoruri, acoperiri și siguranța contractului

Acest ghid explică ipoteze de strategie pentru Whist românesc. Regulile obligatorii sunt în [Cum se joacă](WHIST_RO.md).

## Catalogul și codul care îl produce

| Sursă | Responsabilitate |
|---|---|
| [Catalogul YAML](../../config/whist-hand-patterns.yaml) | 19 familii de combinații, contextul cerut și statutul fiecărui indiciu |
| [Extragerea combinațiilor](../../scripts/whist-hand-patterns.mjs) | `describeHolding`, `PATTERNS` și generatorul `catalog` |
| [Calibrarea](../../scripts/whist-heuristic-calibrate.mjs) | Observații descriptive din partide de dezvoltare validate |
| [Manifestul contextual](../../config/whist-contextual-discard.yaml) | Reguli și teste pentru necesar, discard și atuul de pe masă |

Catalogul declară `RESEARCH_CATALOG_NOT_RUNTIME_CONFIGURATION` și `runtime_prior_influence: 0`. Editarea ponderilor sale nu schimbă automat AI-ul.

## Ce înseamnă o acoperire

O carte mică din aceeași culoare poate absorbi un tur în care adversarii consumă un onor superior. Promovarea depinde de conducere, reintrări, distribuție și posibilitatea de tăiere.

| Combinație | Ipoteză utilă | Limită |
|---|---|---|
| K + mică | Regele poate deveni câștigător după as | Asul poate fi păstrat; regele poate fi tăiat sau aruncat |
| Q + două mici | Dama poate supraviețui consumării A/K | Onorurile pot cădea împreună sau în tururi separate |
| J + trei mici / 10 + patru mici | Promovare posibilă într-o culoare lungă | Pot lipsi tururile sau reintrările necesare |
| A lateral | Cel mai mare rang al culorii | Poate fi tăiat sau aruncat pentru a evita o levată |
| A la atu | Câștigă levata în care este jucat | Celelalte cărți pot produce levate nedorite |
| K la atu după eliminarea publică a A | Cel mai mare atu rămas | Un A încă pe masa neterminată rămâne activ |
| A-K / A-K-Q | Secvența se evaluează împreună | Indiciile K/Q nu se adună din nou peste secvență |
| Atuuri mici și lipsă de culoare | Posibilități de tăiere | Atuul obligatoriu poate forța o levată nedorită |

- Familiile simple K+x și Q+xx exclud anumite secvențe proprii superioare.
- `HIGHEST_CARD_CAPTURE` descrie capturarea cu onorul cel mai mare.
- `SUIT_TRICKS` descrie levatele culorii, o cantitate diferită.
- `trick_hint` este contextual, fără adunare independentă între onoruri.

## Contextul disponibil

| Moment | Informație utilizabilă |
|---|---|
| Împărțire | Cărțile proprii, numărul jucătorilor, mărimea mâinii și cartea întoarsă |
| Licitare | Numai prefixul licitațiilor deja depuse |
| Joc | Toate contractele, istoricul public, masa curentă, necesarul și scorurile |
| Final mic | Lumile compatibile și continuările legale, dacă enumerarea este completă |

- Pentru 3–6 jucători, rangul minim este `15 − 2 × jucători`.
- Cărțile proprii și cele publice nu pot fi distribuite fictiv adversarilor.
- Atuul întors este nedistribuit; atuul încă pe masă poate câștiga levata curentă.
- Necesarul semnat este `licitat − levate câștigate`.
- Siguranța urmărește contractul exact, inclusiv zero.

## Ce măsoară calibrarea

[Calibratorul](../../scripts/whist-heuristic-calibrate.mjs) acceptă numai partide din partiția de dezvoltare. Numărul efectiv de partide și celule aparține fiecărei execuții, nu unei constante a motorului.

1. Validează regulile și scorul fiecărei partide.
2. Elimină urmele de acțiuni identice.
3. Grupează observațiile după lume inițială, loc și culoare.
4. Mediază politicile în fiecare grup înainte de agregare.
5. Aplică greutatea dovezii `n / (n + 20)`.
6. Exportă cel mult 128 de celule cu minimum 20 de lumi fiecare.

O rată observată de capturare cu K depinde de politica jucătorului, inclusiv de discardul intenționat. Nu reprezintă probabilitatea intrinsecă sau contrafactuală a combinației K+x.

```powershell
node scripts/whist-hand-patterns-cli.mjs catalog <fresh-output.yaml>
node --test scripts/whist-hand-patterns.test.mjs scripts/whist-heuristic-calibrate.test.mjs
```

## Legătura cu motorul curent

- [Hard](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/MonteCarloAi.java) evaluează cărțile prin modelul său și prin gărzi publice limitate.
- [Certificatul de contract](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ContractGuarantee.java) verifică finaluri cu două cărți proprii și toate continuările, în limitele sale.
- [Salvarea publică](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/PublicSalvage.java) cere contract imposibil și dominanță fără deteriorarea punctelor proprii.
- [Independentul](../../whist-lab-app/ai-lab/src/main/java/ro/whistlab/coach/IndependentPlayer.java) are implementare și utilitate separate.

În [cazurile contextuale](../AI_CONTEXTUAL_DISCARD.md), același AS/7H cere discardul asului la necesar zero și păstrarea lui la necesar unu. O carte mai mică nu este automat mai sigură.

Activarea unui indiciu nou cere componente justificate independent, sursă înghețată și evaluare separată pe partide nefolosite la dezvoltare. Catalogul nu promite joc perfect sau acoperirea exhaustivă a tuturor stărilor.
