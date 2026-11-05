# Onoruri, acoperiri și siguranța contractului

Acest ghid formalizează indicii strategice pentru Whist românesc. Regulile jocului sunt definite în [Cum se joacă](../game/ATF_RulesRo.md); catalogul de mai jos nu le poate modifica.

## Responsabilități și statut

| Sursă | Responsabilitate |
|---|---|
| [Catalogul de tipare](../../config/whist-hand-patterns.yaml) | Descrie clase de combinații, contextul relevant, aria indiciului și statutul cercetării. |
| [Extragerea tiparelor](../../whist-lab-app/ai-lab/tools/patterns/whist-hand-patterns.mjs) | Derivă descrieri și categorii din mâna vizibilă jucătorului. |
| [Calibrarea](../../whist-lab-app/ai-lab/tools/patterns/whist-heuristic-calibrate.mjs) | Analizează asocieri descriptive din jocurile alocate dezvoltării. |
| [Manifestul contextual](../../config/whist-contextual-discard.yaml) | Leagă reguli condiționale, limite de informație, implementări și teste. |
| [Obiectivele jucătorilor](../../config/whist-play-objectives.yaml) | Definește prioritățile Normal și Hard și distinge regulile active de experimente. |

Catalogul de tipare este marcat `RESEARCH_CATALOG_NOT_RUNTIME_CONFIGURATION`, iar influența lui directă asupra runtime-ului este zero. Modificarea catalogului nu schimbă de la sine politica jucătorului.

## Reguli generale de interpretare

- Un rang superior poate deveni câștigător după ce rangurile care îl bat au fost jucate sau au devenit imposibil de păstrat. Acoperirile oferă ocazii de a consuma aceste ranguri, nu levate garantate.
- Secvențele de onoruri se evaluează împreună. Același rang nu primește credit separat pentru fiecare tipar care îl conține.
- Numărul de cărți dintr-o culoare, rangurile lor și numărul de levate posibile în acea culoare sunt mărimi diferite.
- O carte mare poate pierde prin tăiere sau poate fi aruncată legal când jucătorul nu are culoarea cerută și nu este obligat să taie.
- O tăiere cu atu este supusă regulii de joc și poate produce o levată nedorită după realizarea contractului.
- Atuul întors, atuul jucat într-o levată terminată și atuul încă pe masa curentă au efecte distincte asupra cărților care mai pot câștiga.
- Orice indiciu despre promovare sau capturare se condiționează de poziția la masă, istoricul public, cărțile rămase, necesarul fiecărui jucător și posibilitățile de intrare.

## Informația permisă

| Etapa | Date disponibile politicii |
|---|---|
| Licitare | Mâna proprie, regulile active și prefixul licitațiilor deja depuse. |
| Joc | Mâna proprie, toate contractele depuse, levatele câștigate, istoricul public, masa curentă, atuul și scorurile publice. |
| Simulare | Numai lumi compatibile cu informația de mai sus; mâinile reale ale adversarilor rămân ascunse. |

[Mărimile de stare](ATF_DecisionModel.md#starea-care-schimbă-decizia) definesc necesarul și oportunitățile. Interpretarea unui onor trebuie să folosească ținta individuală și ieșirile legale; puterea rangului sau dezechilibrul mesei nu sunt suficiente.

## Cum se interpretează observațiile

Frecvența cu care o combinație a câștigat depinde de strategia care a jucat-o, inclusiv de alegerile deliberate de a păstra sau arunca ranguri. Asocierea observată nu este probabilitatea intrinsecă a combinației și nu dovedește ce s-ar fi întâmplat într-o acțiune alternativă.

Calibrarea folosește numai datele permise pentru dezvoltare, grupează observațiile dependente și păstrează separat lotul rezervat. Mărimea eșantionului, partiția, filtrele și pragurile se citesc din protocolul și artefactul rulării, nu se copiază ca rezultate permanente în acest ghid.

## YAML și experimente

[Reguli și configurare](ATF_RulesConfiguration.md) deține rolurile și stările manifestelor, iar [protocolul offline](../learning/README.md) deține calificarea unui candidat. Catalogul de aici exprimă clase de cărți și indicii, fără a modifica politica prin simpla editare a YAML-ului.

Calibrările produse, partidele și rezultatele sunt date externe. Parametrii versionați ai unui model identifică o implementare reproductibilă; activarea lor cere consumator și domeniu declarate.

## Legătura cu implementarea

- [Hard](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/engine/MonteCarloAi.java) evaluează cărțile prin simulări constrânse de informația publică și reguli de siguranță.
- [Certificatul de contract](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/engine/ContractGuarantee.java) verifică exhaustiv numai domeniul și limitele declarate de implementare.
- [Salvarea publică](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/engine/PublicSalvage.java) tratează separat protejarea punctelor proprii și ratarea contractelor adverse.
- [Independentul](../../whist-lab-app/ai-lab/src/main/java/ro/whistlab/coach/IndependentPlayer.java) folosește o implementare și o utilitate distincte; asemănarea numelui obiectivului nu dovedește echivalența politicilor.

```powershell
$output = Read-Host 'Fișierul YAML într-un director extern de rezultate'
node whist-lab-app/ai-lab/tools/patterns/whist-hand-patterns-cli.mjs catalog $output
node --test whist-lab-app/ai-lab/tools/patterns/whist-hand-patterns.test.mjs whist-lab-app/ai-lab/tools/patterns/whist-heuristic-calibrate.test.mjs
```

Catalogul nu promite joc perfect sau acoperire exhaustivă a tuturor stărilor. Afirmațiile despre optimalitate necesită un certificat complet în domeniul său; estimările din simulare rămân estimări.
