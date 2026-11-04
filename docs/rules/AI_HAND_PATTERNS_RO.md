# Onoruri, acoperiri și siguranța contractului

Acest ghid formalizează indicii strategice pentru Whist românesc. Regulile jocului sunt definite în [Cum se joacă](WHIST_RO.md); catalogul de mai jos nu le poate modifica.

## Responsabilități și statut

| Sursă | Responsabilitate |
|---|---|
| [Catalogul de tipare](../../config/whist-hand-patterns.yaml) | Descrie clase de combinații, contextul relevant, aria indiciului și statutul cercetării. |
| [Extragerea tiparelor](../../scripts/whist-hand-patterns.mjs) | Derivă descrieri și categorii din mâna vizibilă jucătorului. |
| [Calibrarea](../../scripts/whist-heuristic-calibrate.mjs) | Analizează asocieri descriptive din jocurile alocate dezvoltării. |
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

Necesarul semnat al unui jucător este contractul său minus levatele câștigate. O valoare pozitivă cere levate, zero cere evitarea lor, iar o valoare negativă arată depășirea contractului. Nevoile individuale se evaluează separat; dezechilibrul total al licitațiilor nu înlocuiește obiectivul realizabil al unui jucător.

Numărul de levate încă posibile se calculează din cărțile rămase și din levata neterminată, numărată o singură dată. Necesarul raportat la oportunitățile rămase este un indicator contextual, nu o probabilitate de reușită.

## Cum se interpretează observațiile

Frecvența cu care o combinație a câștigat depinde de strategia care a jucat-o, inclusiv de alegerile deliberate de a păstra sau arunca ranguri. Asocierea observată nu este probabilitatea intrinsecă a combinației și nu dovedește ce s-ar fi întâmplat într-o acțiune alternativă.

Calibrarea folosește numai datele permise pentru dezvoltare, grupează observațiile dependente și păstrează separat lotul rezervat. Mărimea eșantionului, partiția, filtrele și pragurile se citesc din protocolul și artefactul rulării, nu se copiază ca rezultate permanente în acest ghid.

## YAML și experimente

Rolul fiecărui YAML, ciclul de analiză și limita dintre regulile stabile și datele variabile sunt definite în [protocolul de învățare offline](../AI_TRAINING_PROTOCOL.md). Acest catalog exprimă doar clase generale de cărți și indicii; el nu este un jurnal de partide sau de rezultate.

Partidele și rapoartele de evaluare rămân în directorul de dovezi din afara ATF. Un rezultat concret poate motiva o regulă, dar intră în documentația stabilă numai după ce a fost generalizat, justificat și acoperit prin teste.

## Legătura cu implementarea

- [Hard](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/MonteCarloAi.java) evaluează cărțile prin simulări constrânse de informația publică și reguli de siguranță.
- [Certificatul de contract](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ContractGuarantee.java) verifică exhaustiv numai domeniul și limitele declarate de implementare.
- [Salvarea publică](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/PublicSalvage.java) tratează separat protejarea punctelor proprii și ratarea contractelor adverse.
- [Independentul](../../whist-lab-app/ai-lab/src/main/java/ro/whistlab/coach/IndependentPlayer.java) folosește o implementare și o utilitate distincte; asemănarea numelui obiectivului nu dovedește echivalența politicilor.

```powershell
node scripts/whist-hand-patterns-cli.mjs catalog <output.yaml>
node --test scripts/whist-hand-patterns.test.mjs scripts/whist-heuristic-calibrate.test.mjs
```

Catalogul nu promite joc perfect sau acoperire exhaustivă a tuturor stărilor. Afirmațiile despre optimalitate necesită un certificat complet în domeniul său; estimările din simulare rămân estimări.
