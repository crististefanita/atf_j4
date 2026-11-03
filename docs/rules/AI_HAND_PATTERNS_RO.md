# Onoruri, acoperiri și siguranța contractului

Acestea sunt euristici de strategie, nu reguli suplimentare ale jocului. Regulile obligatorii sunt în [Cum se joacă](WHIST_RO.md). Catalogul machine-readable este [whist-hand-patterns.yaml](../../config/whist-hand-patterns.yaml); generatorul și extragerea contextului sunt în `scripts/whist-hand-patterns.mjs`.

## Ce înseamnă o acoperire

O carte mică din aceeași culoare poate fi jucată când adversarii consumă un onor superior. Regele sau dama rămase se pot promova. Acoperirea nu retrage singură onorurile adversarilor: contează cine începe, cine poate continua culoarea și dacă alt jucător poate tăia.

| Forma mâinii | Ipoteză utilă | Ce poate schimba rezultatul |
|---|---|---|
| K + mică | Posibilă promovare după consumarea asului | Două cărți nu garantează două tururi; asul poate fi păstrat, regele tăiat sau aruncat intenționat |
| Q + două mici | Poate rezista până se consumă A și K | Onorurile pot cădea în aceeași levată sau în tururi diferite; contează distribuția și reintrările |
| J + trei mici / 10 + patru mici | Ipoteze de promovare pentru mâini lungi | Nu rămân neapărat suficiente tururi; nu avem o levată garantată |
| A în culoare laterală | Cel mai mare rang al culorii | Se poate pierde prin tăiere sau discard intenționat; la licitație zero poate fi o problemă |
| A la atu | Câștigă levata în care este jucat; zero devine imposibil | Trebuie evitate alte levate nedorite dacă ținta este unu |
| K la atu, A de atu expus/consumat | Este cel mai mare atu rămas și câștigă când este jucat | Gap-ul trebuie eliminat prin informație publică, nu presupus |
| A-K / A-K-Q | Secvență care trebuie evaluată împreună | Nu se însumează cu ipoteze independente pentru K/Q; lungimea proprie și adversă contează |
| Multe atuuri mici și lipsă de culoare | Posibilități de tăiere | La Whist românesc atuul este obligatoriu când lipsește culoarea; uneori aceasta forțează o levată nedorită |

Catalogul are 19 familii: atuuri majore, as lateral, rege singur, rege acoperit, KQ/KJ/KQJ cu acoperiri, grupuri de onoruri promovabile, damă scurtă/acoperită, QJ acoperit, valet/zece lung, AK/AKQ, atu lung/mic, lipsă de culoare laterală și culoare lungă fără atu. Familiile simple K+x și Q+xx exclud secvențele proprii superioare care le-ar contamina observațiile.

## Contextul este parte din ipoteză

Pentru 3–6 jucători, pachetul are rang minim `15 − 2 × jucători`. Înregistrăm numărul cărților împărțite, culoarea/atuul, gap-urile superioare rămase după cărțile proprii și cele publice, lungimea culorii, acoperirile mici, onorurile comune și posibilitatea de tăiere. A de atu întors este informație publică, nu un as ascuns distribuit fictiv unui adversar.

La licitare folosim numai licitațiile deja depuse. Totalul final al licitațiilor devine cunoscut abia după ultima licitație. În joc contează contractele adversarilor, levatele încă necesare, dacă un contract a devenit imposibil, locul în levată, cine poate lua conducerea și starea seriei. „Joacă la siguranță” urmărește probabilitatea **contractului exact**, nu numărul maxim de levate.

Nu însumăm `K+x ≈ 1` și `Q+xx ≈ 1` când fac parte din aceeași culoare. Simularea evaluează mâna comună și reacțiile jucătorilor, cu reguli legale și informație publică. Numărul de cărți necunoscute și riscul de tăiere pot schimba mult valoarea unui onor în mâinile de 2–8 cărți.

## Ponderi măsurate, încă inactive în decizie

Calibrarea v2 a analizat 4.869 de partide de **dezvoltare**, 2.551 de urme de acțiuni distincte și 6.516 celule contextuale. Cele 128 de celule cu suport mare sunt exportate în YAML; inventarul integral rămâne JSONL extern. Nu s-a citit heldout pentru învățare.

Eliminăm urmele identice, mediem rezultatele politicilor în cadrul aceleiași lumi inițiale/loc/culori/context și apoi dăm greutate egală lumilor. Ponderea dovezii este `n / (n + 20)`. `HIGHEST_CARD_CAPTURE` măsoară capturarea cu onorul; `SUIT_TRICKS` măsoară levatele întregii culori. Sunt cantități diferite.

Aceste rezultate depind de politică: un jucător poate arunca deliberat regele pentru a respecta zero. Frecvența observată de câștig cu regele nu este probabilitatea intrinsecă sau contrafactuală a combinației K+x. De aceea catalogul declară **`NOT_RUNTIME_CONFIGURATION` și influență runtime 0**. Ponderile nu schimbă automat jocul; activarea lor cere un experiment A/B separat, bugete egale, componente și un lot nou de evaluare.

```powershell
node scripts/whist-hand-patterns-cli.mjs catalog C:\work\whist_back\hand-patterns-preview.yaml
node --test scripts/whist-hand-patterns.test.mjs scripts/whist-heuristic-calibrate.test.mjs
```

Pentru protocolul de masă, rezultate și limite vezi [Arena AI](../AI_STRATEGY_ARENA.md). Nu există aici o promisiune de joc perfect sau acoperire exhaustivă a tuturor mâinilor posibile.
## Lecție verificată: evitarea unei levate în plus

După realizarea contractului, o carte mai mică nu este automat alegerea sigură. În contraexemplul R11, contract 1 și deja 1 levată, QH + 10D la atu treflă, aruncarea lui 10D păstrează exact contractul în toate cele 96 de distribuții public compatibile și toate continuările legale; aruncarea QH permite o levată suplimentară. Aceasta este o demonstrație contextuală, nu regula „păstrează mereu dama”. Garda se aplică doar finalurilor mici enumerate complet; fără dovadă sau la depășirea bugetului se păstrează plannerul anterior. Vezi [analiza R11](../AI_LOSS11_ACCEPTANCE.md).


## Licitație și informație disponibilă — R12

O deschidere trebuie aleasă înainte de a vedea lumea ascunsă. Alegerea altei cărți pentru fiecare lume și abia apoi medierea poate supraestima contractul. Contraexemplele și modelele respinse sunt în [analiza R12](../AI_ROUND12_ANALYSIS.md). Aceste lecții nu activează automat ponderi sau strategii. Nivelurile curente sunt [Normal/Hard](../TWO_GAME_LEVELS.md); Hard păstrează motorul acceptat, iar certitudinea se declară numai pentru cazurile demonstrate complet.

## Necesar și salvare certificată — R13/R14

Necesar=licitat−levate câștigate; oportunitățile includ levata incompletă. Cărțile jucate și atuul întors ies din cărțile posibile ale adversarilor. Ponderile contextuale de presiune au regresat în evaluare și rămân inactive; vezi [analiza R13](../AI_CONTRACT_PRESSURE_R13.md).

Hard activează numai [garda exactă R14](../AI_CONTRACT_SALVAGE_R14.md): pentru un contract deja imposibil, schimbă cartea doar dacă fiecare lume compatibilă și fiecare continuare legală păstrează punctele proprii și cel puțin același număr de adversari ratați, cu o îmbunătățire strictă. Cazul public7S/7H trece de la2 la3 adversari ratați la aceeași penalizare−4. Egalitatea mediilor sau o căutare incompletă nu certifică această regulă.
