# Interfața Whist — atlas de orientare

## Pe scurt

Acest pachet compune interfața Android dintr-un `GameSession.Snapshot`.
Regulile, licitațiile legale și scorurile aparțin `game-core`; geometria nu
modifică distribuția sau rezultatele ca să facă un ecran să pară corect.

## Flux și responsabilități

```mermaid
flowchart LR
SNAP[Stare capturată] -->|alimentează| ACT[MainActivity]
ACT -->|compune| TABLE[GameTablePanel]
TABLE -->|măsoară| GEO[Aranjare și geometrie]
GEO -->|poziționează| VIEW[Elemente accesibile]
VIEW -->|trimite intenții| ACT
```

<details>
<summary>Legendă și limite</summary>

Diagrama descrie prezentarea și interacțiunea. Motorul validează orice comandă rezultată, iar geometria adaptează spațiul fără a modifica regulile sau cărțile.

</details>

## Traseul unei randări

| Suprafață | Cod și rol |
|---|---|
| Masa | MainActivity alege starea, GameTablePanel construiește masa, iar GameBoardLayout și TableGeometry măsoară regiunile. |
| Mâna | HandLayoutMetrics distribuie cărțile în zona disponibilă. |
| Scor | ScoreTableView compară locurile. ScoreHistoryColumn furnizează licitația, punctele și totalul afișate în ScoreHistoryView pentru locul selectat. |

Componentele de mai jos dețin geometria, interacțiunea și prezentarea acestor suprafețe. Invarianta fiecăreia se verifică separat de regulile și scorul motorului.

| Intrare | Responsabilitate și invariantă |
|---|---|
| `MainActivity` | Tranziții, persistență, meniu, limbă, pauza levatei; nu calculează coordonatele scaunelor. |
| `GameBoardLayout` | Măsoară scaunele și banda sigură. În joc, stratul transparent folosește lățimea mesei; la licitare, centrul rămâne exclusiv. |
| `TableGeometry` | Dreptunghiuri pentru scaune și banda sigură; omul rămâne sud. În joc, scaunele diagonale stau în afara rândurilor cărților. |
| `TrickCardMetrics` | Rezervă rândurile levatei și fixează pozițiile cărților după ancorele scaunelor. La patru jucători formează crucea, cu cartea omului jos și centrată. Fața are 104 dp pe ferestre normale și 96 dp sub 600 dp înălțime logică; ține cont de etichete și spațiu. |
| `WhistUiPalette.seatFrame` | Furnizează același fundal și contur pentru scaunul activ și indicatorul de atu. `TrumpIndicatorView` adaptează textul și simbolul la contrastul ramei; E2E compară umplerea și lizibilitatea în captura fizică. |
| `TableViewportPolicy` | Recuperează spațiu decorativ și compactează rame/levată pe ferestre scunde; nu reduce țintele de 48 dp sau fețele din mână de 98 dp. Pe ferestre compacte, cărțile din levată au 96 dp; pe celelalte, 104 dp. |
| `HandLayoutMetrics` | Păstrează un rând și un avans cel puțin egal cu banda măsurată a indicelui; textul mărit poate necesita derulare orizontală. |
| `CardFaceBackground` | Desenează simbolul central și colțul inversat. Semantica și indicele principal rămân în elementul vizual accesibil. |
| `DropdownBackground` | Săgeată vizibilă pe selectorul Android nativ; nu înlocuiește interacțiunile, focalizarea sau semantica acestuia. |
| `DisplayFixtureDeals` | Date deterministe pentru mesaje Android protejate de `BuildConfig.DEBUG`; cele patru culori de 10 apar fără cărți duplicate. Nu sunt distribuții ale pilotului normal. |
| `ScoreHistoryView` | Păstrează selectorul și antetele deasupra listei. Doar rândurile se derulează. Deschiderea din foaia consultativă păstrează masa dedesubt. |
| `ScoreHistoryColumn` | Asociază licitația locului selectat cu punctele și totalul de la încheierea mâinii. Produce aceeași ordine și aceleași valori pentru afișare, citire asistată și reluarea de componentă. |
| `WhistUiText` | Localizarea EN/RO; engleza este implicită la prima instalare. |

## Principii de aranjare

- Pâslă continuă, fără oval decorativ; tabla și decizia curentă sunt dominante.
- Nume, licitat și levate formează un grup compact. Valoarea este aliniată
  separat de etichetă; indicatorul turei folosește și text, nu numai culoare.
- Zona de licitare folosește banda măsurată dintre jucătorii de sus și omul de
  jos. Când rămâne spațiu, coboară 62% din surplus către locul omului; cu o
  fereastră scurtă, păstrează distanța sigură și toate țintele de 48 dp.
  Pe ferestre compacte se elimină numai spațiu decorativ deasupra numelui și
  din interiorul ramei, fără a micșora numele sau valorile licitate/levate.
- Mâna omului stă jos, centrată după lățimea reală a ferestrei. La două sau
  trei cărți, avansul se oprește la 72% din lățimea feței. Dacă indicii cer
  derulare, vederea inițială pornește din mijloc, iar jucătorul poate ajunge
  la ambele capete. Treisprezece cărți au
  indici verticali expuși. Cartea selectată se ridică în spațiul rezervat și
  rămâne în spatele vecinelor; selectarea nu mută scaunul omului.
- Levata ocupă pozițiile scaunelor: nord, vest, est și sud la patru jucători;
  alte configurații folosesc ancorele nord–sud disponibile. Poziția sudică a
  omului rămâne fix chiar dacă ordinea cărților jucate diferă. Rândurile
  libere sunt rezervate dinainte, astfel încât cărțile să nu sară între ture.
  Pe ecranele scunde, masa completă poate cere o derulare verticală; fața de
  96 dp (compact) sau 104 dp trebuie să rămână întreagă după aducerea levatei
  în zona vizibilă.
- Atuul își măsoară chenarul după text și păstrează spațiu față de titlu și masă. Cartea întoarsă apare pe un fond ivoire, cu rang și simbol. Când regulile stabilesc doar culoarea, indicatorul o afișează cu explicația localizată.
  Fundalul și conturul indicatorului de atu urmează exact stilul scaunului al
  cărui rând este. Centrul mesei este al licitației sau al levatei.
- Selectorul limbii este listă derulantă. Configurarea păstrează opțiunile relevante,
  iar regulile și programul complet se consultă la cerere.
- Scorul precedă explicațiile. Navigarea secundară nu concurează cu acțiunea
  de continuare; foaia din timpul jocului nu confirmă licitația.
- Dacă dimensiunile sau textul mărit nu încap, păstrăm derularea accesibilă.
  Nu confundăm existența nodurilor XML cu absența decupării din captură.

## Cum verificăm

`TrickCardMetricsComponentTest` verifică pozițiile ancorate, crucea cu patru
jucători, înălțimile responsive de 96/104 dp, lipsa intersecțiilor cu scaunele și lățimi de
240–760 dp la trei scări de text. `GameTableLayoutComponentTest` verifică intersecțiile regiunilor, iar
`TableViewportPolicyTest` verifică ferestrele scunde și rotunjirea dp–pixeli.
`DisplayFixtureDealsTest` verifică unicitatea și diversitatea indicilor.

În `atf-test-android`, `WhistDenseLayoutE2eTest` compară banda expusă, pixelii
rangului și simbolului, înălțimea feței și zonele tactile. `HandIndexAudit` are
cazuri negative pentru simbol lipsă, indice acoperit și margine tăiată. Capturile sunt
revizuite și vizual; oracolul raster verifică lizibilitatea de bază, nu poate
certifica singur frumusețea interfeței sau identitatea optică a fiecărui simbol.

`WhistMenuLayoutE2eTest` parcurge meniurile normale în EN/RO.

- `WhistResponsivePlaySanityE2eTest` verifică profilurile, fața din mână de 98 dp, cărțile jucate de 96/104 dp după înălțimea logică, licitarea mai apropiată de locul omului, poziția sudică, distanța față de rame și contrastul atuului. Matricea nu repetă fluxul de joc la fiecare profil.
- Strategia Hard are o verificare separată de geometrie, cu bugetul de calcul declarat.
- Pilotul românesc joacă mâini normale și verifică scorul independent.
- Compilările rămân în TMP, iar rezultatele execuției în depozitul extern configurat.
