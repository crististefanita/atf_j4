# Whist Lab — feedback de utilizare

Revizie: 3 noiembrie 2026. Domeniu: comparație între aplicația originală și
Whist Lab, verificarea ecranului de licitare și a informației publice de la
masă, plus șase mâini jucate efectiv. Partida de 19 mâini nu a fost terminată.
Feedback-ul privește experiența jucătorului, separat de acceptanța ATF și de
calitatea rapoartelor.

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

În aceleași panouri, `Licitat —` și `Levate 0` sunt rânduri text fără separator
vizual între etichetă și valoare. Etichetele și valorile nu au coloane fixe, iar
alinierea pare diferită între scaune și față de numele centrate. Pentru un
jucător nou nu este instant clar unde se termină eticheta și unde începe
valoarea. Puneți separator explicit — de exemplu `Licitat: —` și `Levate: 0` —
și aliniați eticheta și valoarea în coloane stabile, identice pentru toate
scaunele; nu obțineți alinierea prin spații în text.

Butoanele de bid au suprafețe deschise pe pâsla verde, dar nu sunt grupate
într-un container cu limite clare. Rândul 5–8 pare plutitor peste jucători, iar
starea selectată 0 este singura care se distinge ferm prin auriu. Faceți întregul
selector o zonă vizuală coerentă: contrast suficient pentru butoanele normale,
contur clar, selecție evidentă și stare dezactivată distinctă. Păstrați ținte
tactile reale de minimum 48 dp; măriți hitbox-ul fără să micșorați sau să
înghesuiți cifrele.

**Remediere cerută:** calculați layout-ul din viewport, numărul de opțiuni,
dimensiunea textului și dreptunghiurile ocupate de scaune. Selectorul trebuie să
primească o regiune proprie; dacă nu încape, rearanjați dinamic masa și scaunele
sau folosiți un panou compact/modal, fără ca un bid să treacă peste un nume,
etichetă ori valoare. Nu hardcodați doar un număr de coloane: captura actuală
  folosește deja 5 coloane, dar al doilea rând tot intră în scaune. Captura este
  păstrată local în `%TEMP%`, nu este inclusă în repo.

**Criterii de acceptare obligatorii:** capturi reale la 320, 353, 393 și 600 dp;
5–10 opțiuni; turul primului și al ultimului licitator; text implicit și mărit;
limite vizuale și hitbox-uri măsurate separat. Zero intersectări între selector
și scaune, etichete/valori cu baseline și coloane consecvente, două puncte după
fiecare etichetă și toate valorile de bid ușor de distins înainte de atingere.
Un test de bounds fără revizie vizuală a capturii nu trece acest criteriu.

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
laterale. Suprafața verde continuă pe tot ecranul, conform cerinței aplicației
nativă de a păstra același fundal în jurul mesei și al mâinii.

## Feedback prioritar

### P1 — Mâna, masa și decizia trebuie să rămână împreună

Jucătorul trebuie să compare toată mâna cu decizia curentă. Înainte de confirmare,
valorile disponibile trebuie să fie vizibile direct pe suprafața mesei, nu ascunse
într-un dropdown. Atuul, locurile, licitațiile și levatele câștigate rămân lizibile.

**Rezolvare în cod:** pâsla continuă umple zona de joc; nu se mai desenează o
ramă ovală. Locurile au ancore numite pe perimetru, nu celule de grilă: la șase
jucători, forma este
`N; NW/NE; SW/SE; S`, iar omul rămâne în sud. Ancorele nu se schimbă între
licitare și joc; flancurile sunt apropiate de margini cu 18% din înălțimea mesei
în licitarea normală, 16% în licitarea compactă și 23% în joc, pentru a păstra
centrul liber ca în mock și a lăsa grila în dreptul scaunelor. Pentru 3–5 jucători,
locurile ocupă ancorele simetrice disponibile. Locurile laterale
flanchează grila centrală fără să intre în ea. Numele ocupă
primul rând al fiecărui loc; licitația și levatele sunt pe rânduri separate,
aliniate la stânga (`Bid` / `Won`, `Licitat` / `Levate`). Jucătorul
activ primește badge-ul `BID NOW` sau `PLAY NOW` (în română `LICITEAZĂ` sau
`JOACĂ ACUM`), dar fiecare loc rezervă aceeași înălțime pentru badge, ca turele
să nu deplaseze scaunele. Descrierea TalkBack păstrează formularea completă.
Selectorul păstrează forma 3 × 3 pentru 4–9 valori de la 400 dp. Între 260 și
399 dp folosește cinci coloane pe două rânduri, iar sub 260 dp trei coloane; la
mai mult de nouă valori folosește șase coloane de la 312 dp, altfel trei. La
`393×698 dp`, lățimea efectivă a tablei este 353 dp: forma 3 × 3 intersecta în
mod măsurabil locurile laterale. Varianta de cinci coloane și insetul compact au
eliminat intersectarea fără să reducă țintele de 48 dp. Opțiunea aleasă și
`Place bid` / `Licitează` stă împreună cu valoarea în centrul mesei. Indicatorul
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
lizibile simultan cu alegerea centrală și mâna omului.

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
- APK-ul verificat a fost construit în `%TEMP%`; probele au folosit `393×698 dp`
  pe API 28 x86_64. Flow-ul cu șase jucători a verificat opt cărți, hitbox-uri,
  hamburgerul, scorul doar pentru citire, păstrarea bidului neconfirmat și home.
  Flow-ul cu 13 cărți a jucat o singură carte și a verificat că locul omului nu se
  deplasează. Nicio partidă nu a fost terminată.
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
