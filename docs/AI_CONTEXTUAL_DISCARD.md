# Contextul schimbă valoarea onorurilor

[Manifestul RO/EN](../config/whist-contextual-discard.yaml) leagă regulile de implementare și de teste. El nu activează ponderi noi sau modelele de cercetare respinse.

## Aceleași cărți, necesar diferit

Un AS lateral poate fi aruncat dacă nu ai culoarea cerută și nici atu. Existența unei asemenea ieșiri depinde de conducere și de cărțile încă disponibile.

[Componentele](../whist-lab-app/game-core/src/test/java/ro/whistlab/core/ContextualHonorDiscardTest.java) reconstruiesc un prefix legal al unei mâini de opt cărți fără atu. Actorul are AS/7H sau KS/7H, iar pe masă sunt trei trefle.

| Necesar propriu | Alegere sigură | Rezultat în cele șase lumi compatibile |
|---|---|---|
| 0 | Aruncă AS/KS; păstrează 7H | Nicio levată proprie în plus |
| 1 | Aruncă 7H; păstrează AS/KS | Exact o levată proprie în plus |

- Istoricul și cărțile rămân aceleași; se schimbă numai contractul propriu.
- Hard și independentul sunt verificați separat.
- Oracolul verifică toate lumile și continuările legale ale acestor cazuri.
- Mâinile reale ale adversarilor nu intră în API-ul de decizie.
- Un caz cu șapte cărți și atu caro cere 7D pe trefle, inclusiv la contract zero.

## Limita publică pentru atuuri majore

[`PublicSalvage.quota`](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/PublicSalvage.java) calculează necesarul, oportunitățile și o limită inferioară a levatelor inevitabile. Un atu public încă pe masă trebuie tratat diferit de o carte deja consumată.

| Situație | Efect asupra limitei |
|---|---|
| Un atu major propriu poate fi jucat legal sub un atu superior curent | Scade cu cel mult unu |
| Asul superior este cartea întoarsă | Nu scade |
| Asul superior este într-o levată încheiată | Nu scade |
| Atuul curent este inferior celor proprii | Nu scade |
| Obligația de culoare interzice jucarea atuului propriu acum | Nu scade |

Exemplu: KS/QS pot părea două câștiguri inevitabile după eliminarea AS din pool-ul necunoscut. Dacă AS este încă pe masă, unul poate pierde acum, iar cel păstrat câștigă ultima levată.

Corecția păstrează recuperabil un contract de o levată în acest exemplu. Ea repară clasificarea publică, fără a demonstra o îmbunătățire statistică generală.

## Necesar și presiune

| Mărime | Definiție | Evoluție în aceeași mână |
|---|---|---|
| Necesar semnat | `bid − won` | Scade când actorul ia o levată |
| Oportunități | `(cărți rămase + cărți pe masă) / jucători` | Include levata neterminată o singură dată |
| Diferență semnată | `sum(bid − won) − oportunități` | Egală cu `sum(bid) − mărimea inițială`, deci constantă |
| Cerere pozitivă | `sum(max(0, bid − won))` | Nu compensează necesarul pozitiv cu valori negative după depășirea contractului |
| Urgență individuală | Necesar raportat la oportunități | Se schimbă împreună cu nevoia și informația publică |

Masa sublicitată nu obligă fiecare jucător să evite levatele. În cazul cu licitații `[2,1,0,0]` și levate `[0,0,2,0]`, diferența este −1, dar primul jucător are nevoie de ambele levate rămase.

- Necesarul pe locuri este `[2,1,−2,0]`.
- Cererea pozitivă este 3, cu două oportunități disponibile.
- AS/KS păstrează exact două levate și șapte puncte pentru actor.
- Martorul legal folosește atu pică și carte întoarsă 7S, nu 7D.

## Obiective și limite

| Motor | Comportament implementat |
|---|---|
| [Hard](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/MonteCarloAi.java) | Preferă șansa estimată a contractului exact; punctele proprii și scorul rival rup egalitățile |
| [Independent](../whist-lab-app/ai-lab/src/main/java/ro/whistlab/coach/IndependentPlayer.java) | Utilizează propria implementare de utilitate și certificat |
| [Certificat de contract](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ContractGuarantee.java) | Caută o garanție universală în finaluri eligibile cu două cărți proprii |
| Salvare Hard | La contract imposibil și două cărți proprii, cere dominanță fără deteriorarea punctelor în fiecare lume și toate răspunsurile |

„Să nu greșesc singur” rămâne condiționat de fezabilitate și de protejarea punctelor proprii. Nu justifică sacrificarea unui contract recuperabil sau deducerea unor cărți ascunse din intenții presupuse.

Cele șase metode de componentă verifică legalitatea, schimbarea necesarului și distincția dintre atu curent, întors și consumat. [Dosarul dovezilor](AI_COMPONENT_EVIDENCE_R17.md) păstrează calificarea martorilor și corectarea explicită a fixture-ului.

## English orientation

- A side ace or king may be a discard exit or a required winner.
- Changing remaining need from zero to one reverses the safe action in the proved cases.
- Follow-suit and mandatory-trump rules override intention.
- A higher trump on the unfinished table may consume one held master; a turned or completed-trick trump cannot.
- Auction imbalance stays constant within a hand; individual urgency and feasible card beliefs change.
- The correction fixes a public lower bound and makes no general perfect-play claim.
