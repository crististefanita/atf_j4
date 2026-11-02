# Whist Lab — feedback de utilizare

Revizie: 2 noiembrie 2026. Domeniu: comparație între aplicația originală și
Whist Lab, verificarea ecranului de licitare și a informației publice de la
masă. Nu se joacă o partidă completă. Feedback-ul privește experiența
jucătorului, separat de acceptanța ATF și de calitatea rapoartelor.

## Ce trebuie să înțeleagă jucătorul dintr-o privire

| Întrebare | Aplicația originală | Whist Lab |
|---|---|---|
| Ce pot licita? | Toată mâna este vizibilă înaintea licitării. | Opt cărți sunt pe un singur rând; cipurile numerice din centrul mesei sunt discrete, iar țintele tactile rămân de 48 dp. Auriul mat indică selecția; confirmarea este separată. |
| Cât a licitat fiecare? | Valorile sunt pe locurile jucătorilor. | Fiecare loc arată propria licitație; rezumatul arată separat câte licitații s-au depus și suma lor raportată la mărimea mâinii. |
| Câte levate a câștigat fiecare? | Informația este lângă jucători. | Fiecare loc și foaia de scor numesc explicit levatele câștigate, alături de scor. |
| Unde este atuul și ce s-a jucat? | Interfața este funcțională, dar are o ierarhie vizuală mai slabă. | Atuul apare o dată în centrul mesei; în joc, levata curentă, jucătorul și cartea lui sunt grupate. |
| Pot verifica scorul fără să schimb jocul? | Meniul are acces la tabela de joc și scor. | Hamburgerul din stânga sus deschide `Score sheet` sau `Main menu`. Scorul este doar pentru citire și păstrează licitația neconfirmată. |

În prima verificare de layout, Whist Lab ascundea mâna omului înainte de licitare.
Această lacună a fost corectată. Revizia curentă a fost verificată live pe
emulator la `393×698 dp` (`1080×1920`, `440 dpi`) pentru șase jucători și opt
cărți; s-au inspectat PNG-urile, ierarhiile UI și raportul Extent produs de E2E.

## Feedback prioritar

### P1 — Mâna, masa și decizia trebuie să rămână împreună

Jucătorul trebuie să compare toată mâna cu decizia curentă. Înainte de confirmare,
valorile disponibile trebuie să fie vizibile direct pe suprafața mesei, nu ascunse
într-un dropdown. Atuul, locurile, licitațiile și levatele câștigate rămân lizibile.

**Rezolvare în cod:** valorile sunt compuse în centrul mesei, câte cel mult șase
pe rând. Cipul vizibil este redus la aproximativ 34 dp prin inset de 7 dp, în
timp ce controlul exterior păstrează ținta tactilă de 48 dp. Atuul este în același
antet; locurile sunt distribuite pe marginile mesei; confirmarea rămâne unică și
separată. Mâinile de cinci până la opt cărți folosesc coloane cu lățimi egale și
un singur rând, cu etichete de 14 sp.

**Verificare live:** PNG-ul la `393×698 dp` arată opt cărți distincte pe o singură
linie, fără tăiere; toate cele nouă valori, cele șase locuri și butonul de
confirmare rămân vizibile. UI Automator a găsit opt etichete de carte cu aceeași
coordonată verticală și nouă ținte numerice de `132 px` la densitatea `2,75`
(`48 dp`). Cipurile insetate sunt vizibil mai mici decât țintele lor tactile.
După alegerea lui 3, `Score sheet` a afișat `Bid not placed`; la închidere,
valoarea 3 a rămas selectată, iar rezumatul nu s-a schimbat. Nicio licitație nu a
fost confirmată și nicio carte nu a fost jucată.

**Caz de margine rezolvat:** la trei jucători în Descending, mâna de 17 cărți
permite 18 opțiuni și necesită trei rânduri. Înălțimea mesei se adaptează
numărului de rânduri; proba live confirmă toate opțiunile vizibile și locurile
neacoperite. Mâna de 17 cărți depășește înălțimea ecranului și cere derulare;
ecranele mai mici și textul mărit rămân în matricea de validare.

**Risc rămas pentru mâinile lungi:** după alegerea valorii 17, derularea la baza
paginii păstrează butonul selectat și face vizibile toate cele 17 cărți și
acțiunea `Place bid`, dar ascunde rândurile superioare ale grilei. Pentru o
decizie fără navigare repetată, următoarea iterație UX poate ancora confirmarea
și valoarea aleasă în timp ce mâna se derulează; toate opțiunile și țintele de
48 dp trebuie păstrate.

### P1 — Totalul licitațiilor și rezultatul individual trebuie să fie neechivoce

Numărul licitațiilor depuse nu este același cu suma lor. Rezumatul mâinii trebuie
să spună ambele: de exemplu, `Bids submitted 3/4 · Bid total 5/8`. La fiecare
loc trebuie să se distingă licitația jucătorului de numărul de levate câștigate;
aceleași valori apar în foaia de scor. Situația unui jucător care încă nu a licitat
este marcată ca nepusă, nu ca zero.

**Rezolvare în cod:** rezumat separat pentru licitații depuse/total licitat;
fiecare loc folosește eticheta explicită `Tricks won` / `Levate câștigate`, iar
descrierea accesibilă include licitația, levatele, scorul, împărțitorul și rândul.
Foaia de scor folosește aceleași noțiuni.

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

**Rezolvare în cod:** cartea legală primește semn vizual și contur; toate cărțile
pot fi parcurse cu accesibilitatea, cu descrierea cărții și starea ei. Locurile,
atuul, levata și butoanele numerice au descrieri localizate. Butoanele de licitare
au țintă de 48 dp; înălțimea mesei crește cu numărul de locuri și cu fontul.

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
  scurtă de utilizator; nu s-a jucat o partidă completă.
- APK-ul verificat a fost construit în `%TEMP%`; probele au folosit `393×698 dp`.
  E2E-ul de inițializare Oh Hell cu patru jucători și E2E-ul de prezentare cu șase
  jucători au trecut. Ultimul a verificat rândul de opt cărți, hitbox-urile,
  hamburgerul, scorul doar pentru citire, persistența selecției și revenirea la
  home. Nu s-a jucat nicio carte și nu s-a confirmat vreo licitație.
- O probă anterioară în trei jucători/17 cărți a confirmat grila de trei rânduri,
  dar nu este repetată de această revizie de layout.
- Ecranele înguste, textul mărit, TalkBack, toate combinațiile de locuri/mâini și
  foaia de scor în faza de joc rămân neverificate manual.
- Feedback-ul UX nu generează rapoarte Extent și nu păstrează artefacte de joc în
  J4; testele ATF și dovezile lor rămân separate.

## Repere de interfață consultate

- [Arkadium Whist — meniu, tabel și scor](https://support.arkadium.com/support/solutions/articles/44002656380--whist-the-game-main-menu-how-to-play-scoring): tabela de joc și opțiunile sunt separate de suprafața mâinii.
- [Spades+ — capturi ale licitării și levatelor](https://mwm.ai/apps/spades/904702317): valorile publice rămân lângă jucători, iar levata curentă ocupă centrul.
- [CardGames.io Whist](https://cardgames.io/whist/): reper suplimentar pentru masa clasică, mâna jucătorului și scor.

Principiul aplicat aici: masa și mâna domină, licitarea este vizibilă dar discretă,
iar scorul și navigarea sunt la un gest distanță. Acestea sunt repere de ierarhie,
nu modele de copiat ca stil sau reguli.
