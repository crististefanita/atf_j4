# Whist Lab — feedback de utilizare

Revizie: 3 noiembrie 2026. Domeniu: comparație între aplicația originală și
Whist Lab, verificarea ecranului de licitare și a informației publice de la
masă, plus șase mâini jucate efectiv. Partida de 19 mâini nu a fost terminată.
Feedback-ul privește experiența jucătorului, separat de acceptanța ATF și de
calitatea rapoartelor.

**Cod consultat:** `origin/master` la `7a49d2e`. Working tree-ul local diferă
față de acest cod numai prin guard-ul necomis pentru mâna goală; layout-ul și
testele analizate mai jos sunt cele din origin.

## Rulare manuală: șase mâini și blocaje confirmate

**Mediu:** emulatorul existent, Android API 28 x86_64, viewport aproximativ
`393×698 dp`. APK-ul livrat cel mai recent la începutul rulării a fost
`dist/WhistLab-0.1.2-debug.apk`. Acesta s-a blocat la ultima carte din mâna 1,
cu `Unsupported hand size: 0` în construirea mâinii goale. Licitația mâinii era
1 și legală; prin urmare, licitația 0 nu a cauzat blocarea.

Pentru a continua jocul am reconstruit local APK-ul 0.1.2 după o corecție minimă
în `GameTablePanel.java`, care returnează containerul mâinii înainte de a calcula
layout-ul pentru zero cărți. Cu acest APK local, reconstruit (nu cel din `dist`),
am terminat mâinile 1–6. Corecția nu este inclusă în acest commit de feedback și
nu a fost publicată; APK-ul din `dist` rămâne cunoscut ca defect pentru
închiderea mâinii.

| Mâna | Licitația omului | Levate obținute | Puncte afișate | Ce s-a verificat |
|---:|---:|---:|---:|---|
| 1 | 0 | 4 | +4 | Licitația 0 este acceptată; punctajul Oh Hell este levate, fără bonus exact. |
| 2 | 1 | 1 | +11 | Contract exact: 10 + 1 levată. |
| 3 | 2 | 3 | +3 | Ratarea contractului acordă numai levatele. |
| 4 | 3 | 3 | +13 | Ultima licitație care ar fi egalat totalul cu 7 cărți a fost omisă corect. |
| 5 | 0 | 0 | +10 | A doua confirmare practică: bid 0 este valid și scorul exact este +10. |
| 6 | 1 | 3 | +3 | Tururi și urmarea culorii; mâna s-a încheiat după reluarea activității. |

Aceste rezultate sunt observații din partida jucată, nu o acceptanță completă a
regulilor sau a tuturor variantelor. Jocul a rămas la mâna 6 din 19; nu afirmăm
că partida completă a fost verificată.

### Blocant — APK-ul publicat nu poate termina o mână

În APK-ul 0.1.2 din `dist`, eliminarea ultimei cărți ajunge la layout cu
`cardCount == 0`, iar `HandLayoutMetrics.requireHand` aruncă
`Unsupported hand size: 0`. Este un defect de progresie, nu un detaliu vizual:
oprește jocul la fiecare mână terminată în configurația reprodusă. Nu publicați
următorul APK până când buildul distribuit trece repetat tranziția
ultima carte → ecran de scor → mâna următoare. Adăugați un test de componentă
pentru mână goală și un test Android care termină efectiv o mână; buildul local
cu guard-ul a trecut șase mâini consecutive, dar nu înlocuiește verificarea de
release și nu a fost încă distribuit.

### P1 — Licitarea intră peste scaune și nu are aliniere tipografică stabilă

Captura `whistlab-hand3-bid.png` (mâna 3, ecran de licitare) arată concret
defectul la 9 opțiuni (`0–8`) și aproximativ 353 dp lățime utilă:
valorile `5–8` sunt desenate peste panourile Ava și Grace. Butonul `8` acoperă
conținutul din scaunul Grace; opțiunea `5` intră în dreptul panoului Ava. Așadar
grila nu doar atinge contururile: ascunde informația despre jucători exact în
timpul licitării.

Verificarea `origin/master` explică precis defectul: `BidGridMetrics` alege 5
coloane și 2 rânduri pentru 9 valori la 353 dp, dar `TableSeatLayout` mută
scaunele laterale numai când sunt **mai mult de 9** opțiuni. La exact 9,
scaunele rămân vest/est. `GameTablePanel` adaugă apoi grila peste ringul
scaunelor, deci butoanele sunt desenate în fața panourilor. Testul de componentă
confirmă 2 rânduri pentru 9 și afirmă explicit că la 9 scaunele rămân vest/est;
nu verifică intersectarea dreptunghiurilor. Prin urmare, testul trece în timp ce
captura arată text acoperit. Corectați politica pentru cazul 9 și adăugați
verificarea reală a intersecției, nu doar aserțiuni separate despre numărul de
rânduri și pozițiile scaunelor. Capturile mâinilor 4–6 arată rândul scurt
centrat în golul dintre scaune; nu generalizăm suprapunerea la toate licitările.

În aceleași panouri, `Licitat —` și `Levate 0` sunt rânduri text fără separator
vizual între etichetă și valoare. Codul construiește aceste șiruri ca etichetă,
spațiu și valoare, fără două puncte sau câmpuri separate; numele sunt centrate,
iar rândurile de stare sunt aliniate la stânga. Așadar defectul nu este că
numele și rândurile au alinieri diferite, ci că perechea etichetă–valoare nu se
citește ca două câmpuri stabile. Folosiți `Licitat: —` și `Levate: 0` și
randare pe coloane fixe pentru etichetă și valoare, identic la fiecare scaun;
nu simulați coloanele cu spații într-un singur șir.

Butoanele de bid nu sunt grupate într-un container cu limite clare. În sursă,
fiecare țintă are 48 dp, dar fundalul vizibil este retras cu 7 dp pe fiecare
latură, lăsând o suprafață desenată de circa 34 dp; conturul butoanelor normale
este și el foarte apropiat de culoarea lor de fundal. În captura cu 9 valori,
rândul 5–8 pare plutitor peste jucători, iar auriul lui 0 este singura stare
care iese clar în evidență. Faceți selectorul o zonă coerentă, măriți suprafața
vizibilă și contrastul opțiunilor nealese, păstrând ținta tactilă de minimum
48 dp. Starea selectată și cea indisponibilă trebuie să se distingă fără să
înghesuiți cifrele.

**Remediere cerută:** calculați layout-ul din viewport, numărul de opțiuni,
dimensiunea textului și dreptunghiurile ocupate de scaune. Selectorul trebuie să
primească o regiune proprie; dacă nu încape, rearanjați dinamic masa și scaunele
sau folosiți un panou compact/modal, fără ca un bid să treacă peste un nume,
etichetă ori valoare. Nu hardcodați doar un număr de coloane: captura actuală
  folosește deja 5 coloane, dar al doilea rând tot intră în scaune. Captura este
  păstrată local în `%TEMP%`, nu este inclusă în repo.

Ca reglaj suplimentar, scaunele laterale pot fi împinse încă 12–16 dp (circa
2–2,5 mm la scala dp standard) spre margini, dar codul le ancorează deja la
START/END-ul ringului. Pentru a obține deplasarea cerută trebuie ajustat
containerul/insetul exterior sau extins ringul controlat, nu doar schimbat
anchor-ul. Păstrați cel puțin 8–12 dp până la marginea sigură, simetria și
lizibilitatea textului; reduceți offsetul dinamic la viewport îngust ori text
mărit. Măsura ajută la distanță, dar nu înlocuiește grila într-o zonă fără
intersecții.

**Criterii de acceptare obligatorii:** capturi reale la 320, 353, 393 și 600 dp;
5–10 opțiuni; turul primului și al ultimului licitator; text implicit și mărit;
limite vizuale și hitbox-uri măsurate separat. Zero intersectări între selector
și scaune, etichete/valori cu baseline și coloane consecvente, două puncte după
fiecare etichetă și toate valorile de bid ușor de distins înainte de atingere.
Verificați și offsetul lateral la ambele margini, cu păstrarea unui inset sigur și
a simetriei; un test de bounds fără revizie vizuală a capturii nu trece acest
criteriu.

### P2 — Chenarele locurilor par rame goale, nu grupuri de informație

În captura existentă, locurile Ava, Grace și Miles au contururi mari, rotunjite,
fără fundal distinct. Codul confirmă că scaunele AI folosesc exact culoarea pâslei
ca umplere și un contur verde discret, în timp ce scaunul activ primește placă
crem, contur auriu și elevare. Astfel, rama AI ocupă mult spațiu fără să separe
conținutul de fundal, iar starea activă pare alt tip de componentă, nu aceeași
componentă într-o stare activă.

Simplificați locurile: folosiți panouri compacte cu padding consecvent și fundal
discret pentru toate; indicați tura printr-un accent controlat (de exemplu,
contur/accent și marcaj), nu printr-o ramă goală foarte mare pentru AI și o placă
complet diferită pentru om. Dacă se păstrează contururile, reduceți aria lor la
conținut, uniformizați raza și grosimea și aliniați numele cu rândurile
`Licitat: valoare` și `Levate: valoare`. Starea activă trebuie să rămână clară
și pentru utilizatorii care nu disting culorile.

Verificați capturi cu 3–6 jucători, turul omului și al AI-ului, licitare și joc.
Nu acceptați un redesign care doar face rama mai subțire dacă grila încă îi
acoperă textul; validarea trebuie să includă ierarhia vizuală și zero
intersectări.

### P2 — Ultima licitație ascunde o valoare fără să explice regula

În captura mâinii 4, suma existentă este `5/7`, iar jucătorul este ultimul la
licitat. Grila afișează `0, 1, 3, 4, 5, 6, 7`; lipsește 2 fiindcă ar duce suma
exact la 7. Regula este corect aplicată, dar interfața nu explică absența; un
jucător poate crede că opțiunea s-a pierdut sau că bidul 0 este interzis.

Arătați un mesaj scurt, localizat și contextual numai când opțiunea este
exclusă: de exemplu, `Ultimul bid nu poate face suma egală cu 7`. Păstrați
separat suma curentă și numărul de cărți și nu blocați sume mai mari decât
numărul de levate: în mâna 4, rezultatul `8/7` după confirmare este permis de
regula verificată. Confirmați printr-un caz cu valoarea omisă, bid 0 disponibil
și o sumă finală mai mare decât numărul de cărți.

Codul întărește observația: `buildBiddingCenter()` ascunde headingul când
selectorul folosește cele 5 coloane dense, iar lista primită de componentă nu
afișează motivul pentru care lipsește 2. Nu cereți jucătorului să deducă regula
dintr-un număr absent.

### P2 — Butonul „Licitează” domină numele și starea jucătorilor

În aceeași captură, confirmarea centrală este mare și are text proeminent;
privirea ajunge la ea înainte să poată scana numele, licitațiile și levatele
celorlalți. Acțiunea trebuie să fie clară, dar nu trebuie să concureze cu
identitatea și starea mesei, de care omul are nevoie pentru a decide ce licitează.

Sursa îl fixează la 132 dp lățime și 14 sp, în timp ce numele scaunului are
13 sp, iar scaunul lateral la 353 dp are 102 dp lățime. Reduceți moderat lățimea,
padding-ul și greutatea textului butonului — fără să-i eliminați rolul de acțiune
principală — și legați lățimea de grupul selectorului.
Păstrați contrastul și o țintă tactilă de minimum 48 dp; dacă suprafața desenată
se micșorează, extindeți hitbox-ul separat. Verificați în capturi că titlurile
scaunelor și perechile `Licitat: valoare` / `Levate: valoare` se citesc înaintea
butonului, iar butonul rămâne ușor de găsit și atins.

### P2 — Cărțile din mână arată ca plăci albe, nu ca fețe complete de cărți

În capturile de joc, fiecare carte are un singur indice rang–simbol în colțul
stânga sus; restul feței este gol. Codul desenează un dreptunghi alb cu contur și
un singur `shortLabel`, fără indice oglindit sau simboluri centrale. La 7–10
cărți, rândul compact e practic, dar cărțile par butoane mari și goale, iar
identificarea depinde de o singură etichetă din zona suprapusă.

Păstrați rândul compact și lizibilitatea etichetelor, dar evaluați un indice
oglindit în colțul opus și/sau simboluri discrete pe față. Nu aglomerați mâna cu
ornamente: rangul și culoarea trebuie să se distingă rapid în 13–17 cărți,
inclusiv pentru cartea selectată și pentru stările în care cartea nu este legală.

### P1 — Activitatea de audit întrerupe jocul interactiv

În timpul mâinii 4 și din nou în mâna 6, procesul din prim-plan s-a schimbat la
`ro.whistlab.app.atfaudit/.MainActivity`. Captura imediată a tranziției era
aproape albă/roz; jocul principal nu raportase `FATAL EXCEPTION`, iar
`Reia partida` a restaurat mâna și scorul. Așadar nu există dovadă că Whist Lab
a crăpat, dar experiența utilizatorului este tot întreruptă și pare că aplicația
l-a scos din joc.

**Remediere cerută:** identificați cine lansează pachetul de audit și în ce
condiții; în rulările manuale țineți auditul oprit sau separat de sesiunea de
joc. Adăugați în diagnosticul de acceptanță verificarea activității din
prim-plan și capturați separat orice schimbare de pachet. Nu ascundeți problema
sub formularea „checkpoint-ul a păstrat jocul”: păstrarea salvează progresul,
dar nu repară întreruperea.

### P2 — Etichetele scorului nu spun suficient de precis ce măsoară

Ecranul rezultatului folosește coloanele `Bid`, `Lev.`, `Mână`, `Total`. În
practică, `Mână` este punctajul mâinii curente, nu numărul de cărți, iar `Total`
este scorul cumulativ. Înlocuiți-le cu `Licitat`, `Levate câștigate`, `Puncte
mâna` și `Total puncte`. În antet, scrieți explicit `Suma licitațiilor / cărți`
în locul unui raport `3/6` izolat; numărul licitațiilor depuse rămâne o valoare
separată. Păstrați diferența dintre punctele mâinii și totalul partidei în
capturile de regresie, inclusiv când licitația este zero.

### Ce a funcționat în jocul observat

- Licitația 0 a fost acceptată în două mâini; opțiunea nu este interzisă în Oh
  Hell. Restricția se aplică numai ultimei licitații dacă suma ar deveni egală
  cu numărul de cărți.
- La mâna 4, cu 7 cărți și suma celorlalte licitații 5, opțiunea 2 lipsea; bid
  3 a fost permis. Aceasta confirmă regula vizibilă pentru cazul testat.
- Capturile mâinilor 4–6 arată că rândurile de 2 sau 1 opțiuni rămân în spațiul
  central. Suprapunerea din captura mâinii 3 este un caz-limită concret cu 9
  valori, nu o problemă observată uniform pe toate grilele.
- Indicația de urmare a culorii corespundea cărților disponibile, iar cărțile
  legale se distingeau prin contur. După terminarea mâinilor, scorul afișat a
  corespuns formulei Oh Hell observate.
- La revenirea din activitatea de audit, checkpoint-ul a păstrat mâna activă;
  nu s-au pierdut scorurile deja obținute.

### Limitele acestei rulări

Nu s-a terminat partida de 19 mâini. Testul a folosit emulator API 28 și un APK
debug local reconstruit după blocarea APK-ului publicat. Nu a fost testat pe ROG
7, pe Android actual, cu TalkBack, text mărit sau pe dispozitivul fizic. Capturile
manuale au rămas în `%TEMP%` și nu sunt atașate repo-ului; observațiile de mai
sus descriu ce s-a văzut live și nu pretind o matrice completă.

## Ce trebuie să înțeleagă jucătorul dintr-o privire

| Întrebare | Aplicația originală | Whist Lab |
|---|---|---|
| Ce pot licita? | Toată mâna este vizibilă înaintea licitării. | Mâna rămâne pe un singur rând; 13 cărți încap în 320, 393 și 600 dp. Pentru 4–9 biduri, lățimile de 260–399 dp folosesc cinci coloane, iar forma 3 × 3 revine de la 400 dp. Seturile mai mari folosesc șase coloane de la 312 dp. Toate țintele rămân de 48 dp. |
| Cât a licitat fiecare? | Valorile sunt pe locurile jucătorilor. | Fiecare loc arată propria licitație; rezumatul arată separat câte licitații s-au depus și suma lor raportată la mărimea mâinii. |
| Câte levate a câștigat fiecare? | Informația este lângă jucători. | Fiecare loc arată bid-ul și levatele câștigate; punctele rămân în foaia de scor. |
| Unde este atuul și ce s-a jucat? | Interfața este funcțională, dar are o ierarhie vizuală mai slabă. | Antetul arată rangul și simbolul cărții întoarse când există; la mâinile cu tot pachetul împărțit arată culoarea și precizează că nu există carte întoarsă. Levata curentă și numele jucătorilor stau în centrul mesei. |
| Ce se întâmplă când ating o carte? | — | Cartea legală se ridică vizual și se mută în levata centrală; locul omului rămâne fix. |
| Pot verifica scorul fără să schimb jocul? | Meniul are acces la tabela de joc și scor. | Hamburgerul din stânga sus deschide `Score sheet` sau `Main menu`. Scorul este doar pentru citire și păstrează licitația neconfirmată. |

Matricea live din 3 noiembrie, API 28 și densitate 440 dpi, a rulat ambele flow-uri
la 320, 393 și 600 dp. La 320 dp, viewport-ul logic a fost `880×1920`; la 393 dp,
dimensiunea fizică `1080×1920`; la 600 dp, override-ul logic a fost `1650×2560`,
cu PNG fizic `1080×1920` letterboxat de emulator. Toate cele șase rulări au trecut.
Rapoartele Extent conțin capturi inline, pași expandabili și câte 19 aserțiuni
trecute per flow. Aceste flow-uri verifică prezentarea și navigarea, nu jocul
complet; rezultatele jocului manual sunt documentate separat mai sus.

Față de mock, totalul licitațiilor rămâne în rezumatul vizibil din antet, lângă
numărul licitațiilor depuse; centrul zonei de joc rămâne rezervat alegerii și
confirmării bidului. Forma 3 × 3 se păstrează pe ecrane largi, iar grila de cinci
coloane apare numai în intervalul îngust în care forma 3 × 3 intra în locurile
laterale. Totuși, captura la 9 valori arată că simpla trecere la cinci coloane nu
eliberează complet scaunele; spațiul central nu este garantat liber de intersecții.
Suprafața verde continuă pe tot ecranul, conform cerinței aplicației native.

## Feedback prioritar

### P1 — Mâna, masa și decizia trebuie să rămână împreună

Jucătorul trebuie să compare toată mâna cu decizia curentă. Înainte de confirmare,
valorile disponibile trebuie să fie vizibile direct pe suprafața mesei, nu ascunse
într-un dropdown. Atuul, locurile, licitațiile și levatele câștigate rămân lizibile.

**Implementare confirmată în cod:** pâsla continuă umple zona de joc. Locurile au
ancore perimetrale, iar omul rămâne la sud. Pentru patru jucători, însă, politica
schimbă pozițiile AI-urilor de la `WEST/NORTH/EAST` la `NORTH_WEST/NORTH/NORTH_EAST`
când sunt mai mult de 9 opțiuni. Această rearanjare poate muta scaunele între
mâini când numărul opțiunilor trece pragul 9; testați stabilitatea vizuală a
scaunelor, nu doar dacă fiecare anchor este unic. La șase jucători, forma este
`N; NW/NE; SW/SE; S`. Numele sunt centrate; bidul și levatele sunt un singur
șir pe două rânduri, aliniat la stânga. Locul activ primește placă crem și badge,
iar AI-urile păstrează umplerea pâslei cu un contur verde discret.

Selectorul folosește trei coloane sub 260 dp, cinci coloane pentru 4–9 opțiuni
între 260 și 399 dp, 3 × 3 de la 400 dp, și șase coloane pentru seturi mai mari
de nouă valori de la 312 dp. La `393×698 dp`, lățimea efectivă a tablei este
353 dp. La exact 9 opțiuni, grila cu cinci coloane are două rânduri, dar scaunele
nu sunt mutate; captura mâinii 3 confirmă că al doilea rând intră peste ele.
Pentru mai mult de 9 opțiuni, scaunele se mută sus, iar testul componentă verifică
ancorele, nu stabilitatea poziției între mâini. Opțiunea aleasă și
`Place bid` / `Licitează` stau împreună în centru. Indicatorul
din antet afișează cartea întoarsă reală, dacă există; la mâna de 13 cărți din
`Descending`, pachetul este împărțit complet și nu există carte întoarsă, deci
interfața spune explicit că atuul este doar culoarea stabilită de variantă. În
joc, centrul mesei ține levata
curentă centrată în zona de joc; o singură carte stă chiar pe axa mesei, iar cărțile
următoare se aliniază în două coloane. Mâna este un fan
pe un singur rând, cu 98 dp
înălțime și 28 dp rezervați pentru animația verticală. Cartea ridicată se deplasează
20 dp și se scalează la 1,12; vecinele rămân în stratul din față.

**Verificare adăugată:** `GameTableLayoutComponentTest` acoperă mâini de 1–17,
derularea orizontală la 17 pe 353 dp, rezerva reală pentru ridicare, grila de
biduri, ancorele simetrice pentru 3–6 jucători și inseturile de fază. E2E-ul
verifică pozițiile efective în ierarhia Android, nu doar geometria modelului.
Flow-ul cu 13 cărți verifică fanul, atuul disponibil, badge-ul și valorile de la
loc, apoi atinge o carte și verifică levata centrală, cele 12 cărți rămase și
coordonatele neschimbate ale locului omului. Un slot fix pentru instrucțiune
păstrează locurile nemișcate când indicatorul de tură dispare în timpul turei AI.
E2E-ul verifică și centrul real al cărții jucate, balanța verticală dintre masă și
mână pe ecrane înalte, precum și culoarea eșantionată din screenshot după maparea
coordonatelor logice la PNG-ul letterboxat. Testele de text acoperă bid/levate și
badge-urile în engleză și română.

**Rezultat live pentru această revizie:** cele 19 teste Android de componentă au
trecut, iar testul E2E pentru pauza levatei a trecut pe emulator API 28. APK-ul
debug a fost construit în `%TEMP%`. Cele două flow-uri UI responsive documentate
mai jos au trecut anterior la 320, 393 și
600 dp:
`sixPlayerEightCardHandFitsOneRowAndMenuKeepsScoreAndHomeAccessible` și
`thirteenCardFanKeepsOneRowAndMovesTheChosenCardIntoTheTable`. Raportul de masă
are șase PNG-uri originale și nouă atașări inline; raportul fanului are două PNG-uri
și cinci atașări inline per viewport. Fiecare raport include tabelul standard
expandabil de aserțiuni, capturile inline și manifest `PASS`; toate cele 19
aserțiuni din fiecare flow au trecut. Aserțiunea E2E pentru șase locuri măsoară
ancorele `N; NW/NE; SW/SE; S`; captura confirmă că niciun loc nu mai intră peste
grila de biduri. În 8 cărți, indicatorul arată cartea întoarsă completă (`6♠` în
rularea documentată). În mâna Descending de 13 cărți, distribuirea consumă tot
pachetul; nu există o carte fizică întoarsă, iar indicatorul spune `NO TURNED CARD`.
După click, limita accesibilă a cărții jucate a fost centrată la `x=539.5`, iar
centrul mesei la `x=540.5` px; raportul include ambele limite și aserțiunea `PASS`.

La prima încercare, UI Automator a expus descrierea mâinii pe două containere cu
aceleași limite (`[55,1409][1026,1756]`). Driver-ul de test unește doar nodurile
cu descriere identică și limite identice; dacă limitele diferă, păstrează eroarea.
Nicio aserțiune despre geometria jocului nu a fost relaxată.

Rapoartele finale complete sunt în `%TEMP%`:

- `whist-j4-display-final-v13-320dp-20261103\table-presentation\whist-lab-table-presentation.html`
- `whist-j4-display-final-v13-320dp-20261103\thirteen-card-fan\whist-lab-thirteen-card-fan.html`
- `whist-j4-display-final-v12-393dp-20261103\table-presentation\whist-lab-table-presentation.html`
- `whist-j4-display-final-v12-393dp-20261103\thirteen-card-fan\whist-lab-thirteen-card-fan.html`
- `whist-j4-display-final-v11-600dp-20261103\table-presentation\whist-lab-table-presentation.html`
- `whist-j4-display-final-v11-600dp-20261103\thirteen-card-fan\whist-lab-thirteen-card-fan.html`

**Dovadă live din revizia anterioară, nu pentru redesignul curent:** PNG-ul la
`393×698 dp` arată opt cărți distincte pe o singură
linie, fără tăiere; toate valorile permise de regulă, cele șase locuri și
butonul de confirmare rămân vizibile. Când omul este ultimul la licitat, valoarea
care ar duce suma exact la numărul de cărți este exclusă corect. UI Automator a
găsit opt etichete de carte cu aceeași coordonată verticală și ținte numerice de
`132 × 132 px` la densitatea `2,75` (`48 × 48 dp`). Cipurile insetate sunt vizibil
mai mici decât țintele lor tactile.
După alegerea lui 3, `Score sheet` a afișat `Bid not placed`; la închidere,
valoarea 3 a rămas selectată, iar rezumatul nu s-a schimbat. Nicio licitație nu a
fost confirmată și nicio carte nu a fost jucată.

**Cazul de margine revizuit:** 18 valori de bid folosesc șase coloane la 353 dp
și trei coloane când lățimea scade sub pragul de 312 dp; 4–9 valori trec de la
3 × 3 la cinci coloane sub 400 dp, unde suprapunerea cu scaunele laterale apărea
la 353 dp. Mâna de 17 cărți rămâne pe un singur rând și se derulează orizontal
când nu încape. Contractul JVM fixează aceste praguri, iar E2E-ul confirmă 13
cărți fără derulare la 320/393/600 dp. Textul mărit și TalkBack rămân de
verificat live.

### P1 — Totalul licitațiilor și rezultatul individual trebuie să fie neechivoce

Numărul licitațiilor depuse nu este același cu suma lor. Rezumatul mâinii trebuie
să spună ambele: de exemplu, `Bids submitted 3/4 · Bid total 5/8`. La fiecare
loc trebuie să se distingă licitația jucătorului de numărul de levate câștigate;
aceleași valori apar în foaia de scor. Situația unui jucător care încă nu a licitat
este marcată ca nepusă, nu ca zero.

**Rezolvare în cod:** rezumat separat pentru licitații depuse/total licitat;
fiecare loc afișează numele, licitația și `Tricks won` / `Levate câștigate`.
Punctele sunt păstrate în foaia de scor, ca să nu aglomerăm conturul mesei.
Descrierea accesibilă a locului include scorul, împărțitorul și rândul.

**Verificare live:** ecranul cu șase locuri arată licitațiile depuse pe fiecare
loc, levatele câștigate, punctele, progresul `3/6` și suma `6/8`. Valorile sunt
lizibile simultan cu alegerea centrală și mâna omului în acel flow automat cu
șase jucători. Această observație nu acoperă captura manuală cu patru jucători
și nouă opțiuni, unde grila acoperă scaunele laterale.

### P1 — Foaia de scor trebuie să fie o vedere sigură, doar pentru citire

În verificarea live, foaia deschisă în timpul licitării a afișat punctele,
licitațiile și levatele tuturor celor șase jucători. Licitația proprie selectată
dar neconfirmată a rămas nepusă în scor; după închiderea foii, aceeași valoare a
rămas selectată în grila centrală. Nu s-a confirmat nicio licitație și nu s-a
jucat nicio carte.

**Limita dovezii:** starea de joc nu a fost verificată manual în acea parcurgere.
Codul o oferă în faza de joc, dar comportamentul trebuie verificat fără a duce
partida la final.

**Actualizare vizuală:** captura păstrată a Whist Lab arată un rând lung de text
pentru fiecare jucător, cu etichete abreviate și delimitatori greu de scanat.
Randarea curentă grupează valorile în coloane aliniate — jucător, bid, levate,
mână și total — și marchează discret rândul omului. La finalul mâinii arată
rezultatul mâinii curente; în timpul licitării/jocului păstrează `—` în coloana
rezultat și marchează separat bid-ul încă nedepus. E mai ușor de comparat pe
verticală, dar nu trebuie prezentată ca istoric complet.

**Lacună funcțională P1:** captura foii originale păstrate în `whist_back` conține
un registru pe mâini cu câte o coloană pentru fiecare jucător. Captura este rotită
în telefon și nu constituie un reper de layout reușit, însă păstrează informație
istorică pe care Whist Lab nu o mai poate reconstrui: `GameSession.beginHand()`
resetează bid-urile și levatele curente, iar `Snapshot`/checkpoint-ul păstrează
doar scorul cumulativ și starea mâinii active. Dacă vrem aceeași valoare de
auditare, următorul pas trebuie să adauge rezultate imutabile per mână și migrare
versionată a checkpoint-ului; nu deducem bid-uri sau rezultate istorice din total.

### P1 — Suprafața de joc trebuie să fie continuă până la marginea viewport-ului

Captura anterioară `previous-screen-04.png` arată rama lată de fildeș în jurul
mesei, cărțile pe o bază separată și un gol mare sub vechiul buton plin. Această
zonă slăbea masa ca element dominant și părea spațiu nefolosit.

**Rezolvare în cod:** pâsla continuă până la marginea viewport-ului. Nu se mai
desenează un oval sau o ramă de fildeș. `Place bid` / `Licitează` este
conturat și stă în centrul zonei de joc, deasupra mâinii. Mâna are o înălțime
fixă, un singur rând și
derulare orizontală, astfel că zona cărților nu rearanjează masa.

**Contract E2E actualizat:** verifică ordinea verticală dintre acțiunea centrală
și mână și include un flow separat care atinge o carte legală. Raportul include
capturile inline și pasul colapsabil standard pentru aserțiuni; vechile capturi
ale butonului plin nu sunt dovadă pentru această versiune.

### P1 — Toate cele patru cărți ale levatei trebuie să rămână vizibile înainte de următoarea

**Rezolvat:** după ultima carte, interfața ține împreună cele patru cărți timp de
exact 1.500 ms, apoi golește centrul și oferă tura următoare câștigătorului. În
fereastra de afișare nu acceptă input pentru următoarea carte. `CompletedTrickPauseComponentTest`
verifică limita la 1.499/1.500 ms și pornirea unei ferestre noi pentru levata
următoare.

**Verificare live:** `completedTrickRemainsRenderedBeforeTheNextTrickBecomesInteractive`
a trecut pe emulator API 28. E2E-ul eșantionează pe dispozitiv 36 de capturi PNG,
verifică patru fețe fildeș cu marcaje roșii în pozițiile levatei și compară
pixeli identici pe două cadre la 900–1.400 ms distanță. Următorul checkpoint
confirmă că masa s-a golit și câștigătorul are tura. Raportul Extent păstrează
acțiunile, aserțiunile expandabile și imaginile inline în `%TEMP%`.

### P1 — Navigarea trebuie să lase masa drept elementul dominant

Legăturile `Score sheet` și `Main menu` de la baza ecranului concurau cu acțiunea
de joc și împingeau atenția în afara mesei. Navigarea trebuie să fie previzibilă,
standard și mereu accesibilă, fără să consume lățime sau să arate ca a doua acțiune
principală.

**Rezolvare în cod:** un buton hamburger cu trei linii ocupă colțul stânga sus;
meniul conține `Score sheet` și `Main menu`. Scorul folosește o vedere consultativă;
home nu șterge partida salvată.

**Verificare live:** E2E-ul a deschis meniul, a verificat ambele acțiuni, a consultat
scorul celor șase jucători și s-a întors la home. Scorul nu a trimis licitația
selectată, iar la închiderea dialogului valoarea a rămas selectată. Screenshoturile
meniului, scorului și home-ului sunt în raportul Extent cu PNG-uri Base64 inline.

### P1 — Limba trebuie să fie consecventă în tot produsul

Home și configurarea erau în engleză, în timp ce regulile începeau în română.
Selectarea limbii doar pentru ajutor nu rezolva incoerența.

**Rezolvare în cod:** limba devine o preferință globală păstrată pe dispozitiv,
disponibilă din ecranul principal, configurarea mesei și regulile. Instalarea
nouă folosește engleza; româna traduce meniul, configurarea, tabelul, licitarea,
scorul și regulile. Salvările existente care aveau deja o alegere explicită de
limbă pentru ajutor o păstrează la migrare.

**Verificare necesară:** testele unitare verifică fallback-ul EN, textele RO/EN și
rezumatele; după instalare se inspectează schimbarea pe home, configurare, reguli,
licitare și joc, fără completarea unei partide.

### P2 — Programul mâinilor trebuie explicat înainte de prima distribuire

Eticheta `1-8-1` singură nu explică numărul de mâini, ordinea sau repetările.
Descrierea trebuie să se schimbe odată cu varianta și numărul de jucători.

**Rezolvare în cod:** configurarea calculează preview-ul direct din
`GameRules.handSizes`; arată că numerele sunt cărți pe mână, comprimă repetările
consecutive și afișează numărul total de mâini. Preview-ul se actualizează când
se schimbă varianta sau numărul de jucători. Variantele și regulile lor rămân
distincte.

**Verificare:** testele unitare compară dimensiunea preview-ului cu programul
regulilor pentru toate variantele și 3–6 jucători. Mai trebuie confirmată
vizual actualizarea spinnerelor în configurare.

### P2 — Contrast, dimensiunea textului și descrierile TalkBack

Starea cărților legale trebuie să se distingă și fără culoare sau transparență.
Fiecare carte și loc trebuie să aibă o descriere utilă. Textul mărit nu trebuie să
taie informația sau să facă opțiunile centrale imposibil de atins.

**Rezolvare în cod:** cartea legală are contur și descriere localizată; toate
cărțile sunt expuse în arborele de accesibilitate. Locurile, atuul, levata și
bidurile au descrieri localizate. Opțiunile de licitare au ținte de 48 dp; masa
își rezervă înălțime pentru grila activă, numărul de locuri și scala textului.

**Limită:** nu s-a efectuat încă o parcurgere TalkBack; nici matricea 3–6 jucători,
text mare, ecran îngust și mâini minime/maxime nu este completă.

## Întrebări pentru un coleg nou

- Whist Lab pornește implicit în Oh Hell. Este aceasta varianta intenționată
  pentru cine vine din aplicația originală sau trebuie propus Whist românesc?
- Configurarea ar trebui să compare sumar punctajul și atuul variantelor înainte
  de începerea jocului?
- Ce dimensiuni de telefon/tabletă și scale de text definesc contractul suportat?
- Aplicația originală este oracol numai pentru regulile variantei sale sau pentru
  comportamentul tuturor variantelor din Whist Lab?

## Dovezi și limite

- Comparația folosește aplicațiile instalate, instrucțiunile lor și o parcurgere
  scurtă de utilizator. Separat, au fost jucate șase mâini din partida de 19;
  partida completă nu a fost terminată.
- APK-ul verificat a fost construit în `%TEMP%`; probele UI au folosit `393×698 dp`
  pe API 28 x86_64. Flow-ul cu șase jucători a verificat opt cărți, hitbox-uri,
  hamburgerul, scorul doar pentru citire, păstrarea bidului neconfirmat și home.
  Flow-ul cu 13 cărți a jucat o singură carte și a verificat că locul omului nu se
  deplasează; aceste flow-uri nu au terminat o partidă. Separat, rularea manuală
  a terminat șase mâini, descrise la începutul documentului.
- Raportarea prezentării folosește contractul `AssertionStep` al ATF: comparațiile
  UI apar într-un singur tabel standard colapsabil, iar manifestul ordonat este
  păstrat separat cu hash. Corpul text lung era trunchiat la preview-ul rendererului
  și nu oferea dovadă auditabilă; verificarea pe captura live existentă a confirmat
  toate cele 11 rânduri în tabelul Extent. Identitatea dispozitivului în raport
  citește aceeași proprietate sau variabilă de mediu ca adaptorul ADB.
- Captura live curentă confirmă butonul `Place bid` conturat și compact, mâna pe un
  singur rând și tabela de scor cu bid, levate, mâna curentă și total. Verificarea
  acoperă API 28 și dimensiunea de referință; dispozitivele înguste, textul mărit
  și TalkBack rămân de verificat live.
- O probă anterioară în trei jucători/17 cărți a confirmat grila de trei rânduri,
  dar nu este repetată de această revizie de layout.
- Ecranele înguste, textul mărit, TalkBack, toate combinațiile de locuri/mâini și
  foaia de scor în faza de joc rămân neverificate manual. Cele șase mâini au fost
  jucate pe emulator API 28, nu pe dispozitiv fizic.
- Feedback-ul UX nu generează rapoarte Extent și nu păstrează artefacte de joc în
  J4; testele ATF și dovezile lor rămân separate.

### Capturi locale consultate pentru această revizie

În `%TEMP%` sunt 98 de PNG-uri cu `whist` în nume, dintre care 91 încep cu
`whistlab`; inventarul include capturi duplicate, tranziții și imagini dinaintea
rebuildului local, deci nu reprezintă 98 de ecrane distincte ale aceleiași
versiuni. Am comparat în special:

- `whistlab-hand3-bid.png` — cazul de 9 valori; butoanele 5–8 intră în panourile
  laterale, iar 8 acoperă textul lui Grace.
- `whistlab-hand4-bid.png` și `whistlab-hand4-result.png` — valoarea 2 lipsește
  la ultima licitație (`5/7`), apoi suma licitațiilor apare `8/7` după bidul 3.
- `whistlab-hand5-bid.png` și `whistlab-hand6-bid.png` — rândurile mai scurte
  rămân în golul central; nu arată aceeași acoperire ca mâna 3.
- `whistlab-hand3-play.png`, `whistlab-hand4-play.png` și
  `whist-hand5-current.png` — instrucțiunea de tură, cărțile legale și fața
  simplificată a cărților.
- `whistlab-hand2-result.png`, `whistlab-hand3-result.png`,
  `whistlab-hand4-result.png`, `whist-hand5-end.png` și
  `whistlab-hand6-end.png` — etichetele și ierarhia tabelului de scor.
- `whistlab-hand6-trick3.png` — captura alb/roz din schimbarea activității;
  nu este un ecran normal al jocului și nu trebuie folosită ca defect de temă.

`whistlab-table.png` și `whistlab-new-game.png` sunt capturi mai vechi, de
dinaintea rebuildului local; nu le-am folosit pentru a atribui defecte layoutului
curent. `whistlab-hand1-result.png` surprinde launcherul după blocare, nu ecranul
de scor. Capturile rămân în TEMP și nu sunt versionate în repo.

## Repere de interfață consultate

- [Romanian Whist Gold — listare App Store și captură MWM](https://mwm.ai/apps/romanian-whist-gold/733772257) — **89/100**: varianta cea mai apropiată și indicatori individuali de scor/levate. Captura reală are textură puternică și cărți centrale suprapuse; luăm doar vizibilitatea informației publice.
- [Classic Bid Whist — captură MWM](https://mwm.ai/de/apps/classic-bid-whist/1546101065) — **88/100**: face vizibile bid-ul, atuul și scorul echipelor. Ecranul este dens, cu multe valori și controale înghesuite; păstrăm gruparea, nu volumul.
- [World of Card Games — Whist](https://worldofcardgames.com/whist) — **86/100**: cel mai bun reper pentru pâslă verde continuă și distribuția locurilor în jurul levatei. Captura peisaj și scorul de echipă nu se transferă direct la telefon.
- [Coppercod — Minnesota Whist](https://coppercod.games/game/minnesota-whist/) — **85/100**: cel mai bun reper pentru mâna ordonată pe un rând și accesul la levatele precedente; lipsesc scorurile individuale și regulile sunt diferite.
- [Whist Card Game pentru iPhone — captură Softonic](https://whist-card-game.en.softonic.com/iphone) — **78/100**: scorurile echipelor și locurile se disting, dar mâna semicirculară, avatarele și levata suprapusă aglomerează ecranul; e utilă mai ales ca exemplu de evitat.

Notele sunt evaluări euristice pentru potrivirea cu obiectivul J4: relevanța
variantei, lizibilitatea reală a capturii, vizibilitatea stării publice, mâna de
opt cărți și densitatea vizuală. Nu sunt
scoruri de produs sau teste de utilizabilitate. Previzualizările celor cinci
capturile salvate și sursele lor sunt în `WHIST_J4_UI_REFERENCES_20261103` din
`%TEMP%`.

Principiul aplicat aici: masa și mâna domină, licitarea este vizibilă dar discretă,
iar scorul și navigarea sunt la un gest distanță. Pentru foaia de scor, separăm
clar rezultatul mâinii active de un viitor registru istoric. Acestea sunt repere
de ierarhie, nu modele de copiat ca stil sau reguli.

## Normal / Hard — cerință nouă, 3 noiembrie 2026

Modelul expune numai NORMAL/HARD; EXPERT este doar alias Java și nume vechi migrat la citirea salvării. Hard folosește motorul R11 Expert validat, fără activarea candidaților R12 respinși. Spinnerul citește enum.values(), deci layoutul rămâne neschimbat. WhistUiText elimină numai al treilea case, iar GameSessionStore folosește fromStoredName. Verificările LIVE Android sunt separate; adaptoarele noi trebuie să aștepte difficulty=HARD și să păstreze citirea urmelor istorice EXPERT fără a rescrie dovezile. Nu afișați o afirmație de joc perfect fără demonstrație.
