# Contract Whist (Oh Hell) — reconstrucția regulilor

## Scop și metodă

Acest document separă trei lucruri care nu trebuie confundate: regulile scrise
în aplicația originală, comportamentul observat efectiv pe emulator și regulile
implementate în Whist Lab. O captură de ecran confirmă doar ce arată ecranul;
un test al motorului local confirmă doar implementarea locală. Nu tratăm niciuna
dintre ele drept dovadă automată pentru cealaltă.

Sursele primare sunt pagina **Instructions** din aplicația originală
`com.qsoft.android.whist` versiunea 4.5 (145), capturile/XML-urile ADB din
rulările J4 și teste cu deal-uri deterministe în `whist_lab_app/game-core`.
Artefactele unei rulări sunt păstrate lângă raportul Extent din
`artifacts/deep-app-test/runs/<run-id>/evidence/`.

## Matricea regulilor și a dovezilor

| Regulă / comportament | Textul original | Observație originală | Test Whist Lab | Încredere / pas următor |
|---|---|---|---|---|
| Pachetul jocului | Pachet standard de 52 de cărți | Configurarea oferă 3–6 jucători în total (utilizator plus 2–5 AI) | `standardDeckContainsEveryUniqueCard`; distribuția verifică unicitatea | Textul și modelul coincid; verificarea pe toate configurările originale este în curs |
| Mărimea primei mâini | Nu precizează intervalul mâinilor | Capturile bid-grid arată 10 cărți la 3, 4 și 5 jucători; 8 la 6. Pentru 4 jucători, foaia de scor coboară 10→9→…→1, iar `Next Hand` deschide licitarea cu 9 | `firstOhHellDealSizeMatchesOriginalScreenshotsAtEverySupportedTableSize`; modelul rulează cap-coadă secvențele candidate pentru 3–6 | Prima mărime e confirmată vizual la toate cele patru configurații. Doar progresia 4-player 10→1 este observată cap-coadă în original; pentru 3/5/6 continuarea descendentă este încă model local |
| Licitare | Fiecare declară numărul exact de levate pe care crede că le va câștiga. `Bid Other` oferă valori mai mari decât cele afișate | În mâna de 10 cărți, ultimului ofertant i se afișează `Bid 0`–`Bid 10`; dacă ofertele precedente însumează 5, `Bid 5` este dezactivat. În acest ecran nu apare `Bid Other` | Testele verifică intervalul, ordinea ofertelor și interdicția ultimei oferte; sesiunea respinge oferta egală cu totalul mâinii | Regula de total interzis este confirmată de controlul dezactivat și de testul de domeniu. Pragul pentru `Bid Other` și dacă apare în alte moduri/mărimi de mână rămân de explorat |
| Cine începe licitarea/jocul | Jocul începe cu jucătorul din stânga dealerului | Ecranul expune locurile și ofertele, dar UI Automator nu oferă o structură semantică fiabilă pentru masa desenată | `GameSession` pornește bidding-ul și jocul la stânga dealerului | Textul este clar; trebuie adăugat un scenariu determinist cu dealer/seat vizibil pentru a confirma corespondența dintre locurile vizuale și indicii motorului |
| Culoare cerută | Jucătorul trebuie să urmeze culoarea condusă dacă poate | Capturile arată cărți și mutări, dar pictogramele din `View` nu sunt expuse ca noduri UI | `legalPlayMustFollowLedSuitWhenPossible`; `cannotPlayOutOfTurnOrDiscardWhenFollowingSuit` | Implementare locală acoperită; încă nu este o verificare semantică independentă în aplicația originală |
| Jucător fără culoare | Poate decarta sau juca atu | Nu s-a construit încă un deal original controlat care să demonstreze ambele căi | `playerMayDiscardOrTrumpWhenVoidInLedSuit` | Modelul acoperit; originalul rămâne neconfirmat. Următorul pas: repetiții seeded/plan de capturi de mâini reale, apoi verificarea legalității mutărilor vizuale |
| Câștigarea levatei | Cea mai mare carte din culoarea condusă câștigă; atuul bate culoarea condusă; cel mai mare atu câștigă | O mână completă a acceptat 10 mutări umane și AI-ul a avansat între ele; rasterul nu permite atribuirea sigură a tuturor cărților și câștigătorilor | teste cu atu, fără atu și decartare; `trickWinnerLeadsTheNextTrickAndAceBeatsLowerLedSuitCards` | Regulă locală acoperită, validarea originalului la nivel de levată este încă deschisă |
| Conducerea următoarei levate | Câștigătorul levatei începe următoarea | Testul UI confirmă că jocul avansează până la următoarea mutare umană după AI; nu identifică independent cine a câștigat | testul cu deal fix verifică explicit seat-ul câștigător și primul seat al următoarei levate | Local confirmat; originalul necesită recunoaștere de cărți și nume/seat pe aceeași captură |
| Atu | `NT` înseamnă fără atu; instrucțiunile enumeră spade, inimi, treflă, caro, fără atu | Primele mâini observate au: 3 jucători → **caro**, 4 → **inimă**, 5 → **treflă**, 6 → **pică**. În jocul 4-player 10→9, atuul a trecut **inimă→caro**. Ecranele inițiale diferite nu stabilesc dacă originea depinde de configurație, sesiune sau altă stare | Motorul local pornește cu spade și folosește ordinea textuală declarată | **Originea și tranziția rămân deschise.** Nu deducem o regulă din aceste mostre izolate. Următorul pas este repetarea jocurilor noi per configurare și urmărirea a cel puțin cinci mâini consecutive, cu `NT` și revenirea la început |
| Punctaj | 1 punct per levată câștigată și încă 10 pentru ofertă exactă | După prima mână, captura foii de scor arată celulele `3, 3, 4, 0` (citire vizuală manuală; suma 10). Textul spune că tabelul afișează scorurile jucătorilor până acum. Suprafața nu are text accesibil și XML-ul nu permite citire automată | `scoreAddsOnePerTrickAndTenForAnExactBid`; fixture-ul cu o levată exactă verifică `+11`; jocul complet verifică acumularea pe fiecare mână | Formula textuală și implementarea coincid. Scorul original al acestei mâini nu are un caz exact observat și nu este un oracle OCR; raportul păstrează observația vizuală ca WARNING |
| Foaia de scor / tranziții | Coloana din stânga arată cărțile din mână, celelalte scorurile jucătorilor | După 10 mutări apare `Next Hand`; atingerea lui deschide licitarea cu 9 cărți | Modelul rulează mâinile 10→1 și verifică scor cumulativ plus dealerul următor | Pragul UI 10→9 confirmat; terminarea jocului original la mâna 1 și ecranul final nu au fost încă observate |
| Salvare / reluare | Instrucțiunile nu documentează salvarea | `Resume Game` a păstrat ecranul de joc și după force-stop/restart | Testează checkpoint serializat în fazele BIDDING, PLAYING și SCORE | Comportament specific aplicației; local este verificat la nivel de motor și UI, semantica exactă a originalului rămâne limitată la ecranul reluat |

## Reguli de implementare care sunt deja testate local

- un pachet standard conține exact 52 de cărți distincte;
- un jucător trebuie să joace cartea cerută dacă are culoarea condusă;
- un jucător fără acea culoare poate juca orice carte;
- levata se compară întâi după categorie (atu, culoare condusă, altă culoare),
  apoi după rang; fără atu câștigă cea mai mare carte din culoarea condusă;
- dealerul se rotește cu un loc după fiecare mână;
- oferta finală care face suma egală cu numărul cărților este interzisă;
- scorul este `levate câștigate + 10 dacă oferta este exactă`;
- modul Oh Hell de patru jucători folosește 10→1; secvența 3/5/6 rămâne
  ipoteză de model până la măsurare în original.

## Planul următor de reconstrucție

1. Mărimea primei mâini e acum observată pentru 3–6; repetă fiecare configurație
   într-o sesiune nouă și măsoară și ultima mână, dealerul și numărul efectiv de
   mâini. Notează `Bid Other` și enabled/disabled pentru ofertantul final.
2. Într-un joc Oh Hell nou, capturează atuul și numărul cărților la fiecare
   `Next Hand` până treci de `NT` și vezi repetarea. Repetă pentru 3–6 jucători
   ca să separi offset-ul per sesiune/configurare de ordinea mâinilor.
3. Construiește un oracol de levată din deal-uri determinate: fiecare culoare
   condusă, jucător cu/fără culoare, decartare, unul sau mai multe atuuri,
   ranguri egale imposibile între cărți distincte și câștigătorul care conduce
   următoarea levată.
4. Verifică punctajul cu fixture-uri: exact și inexact pentru 0, 1 și mai multe
   levate; suma levatelor tuturor jucătorilor trebuie să fie mărimea mâinii;
   bonusul exact trebuie să fie exact 10, o singură dată per jucător/mână.
5. Rulează jocul complet în model la 3–6 jucători și pentru fiecare mod cunoscut;
   verifică distribuția, turn order, ofertele, legalitatea, scorurile cumulative,
   rotirea dealerului și finalHand. Nu deduce comportamentul AI-ului original din
   politica AI locală.
6. Apoi adaugă scenarii E2E reale doar pentru ce poate fi observat în UI; rapoartele
   trebuie să separe PASS funcțional, WARNING vizual și necunoscut, cu screenshot
   inline exact la checkpoint-ul care susține afirmația.

## Limitări curente

1. UI-ul original desenează cărțile și tabelul într-un singur `View`; textul și
   rangurile nu sunt disponibile în UI Automator. Fără OCR/vision validat nu
   pretindem să știm automat ce carte a jucat fiecare AI.
2. Un singur joc original complet confirmă tranziția de mână, nu formula de scor
   exact, toate ramurile de legalitate sau toate modurile.
3. Secvența atuului este în contradicție între textul instrucțiunilor și primele
   două capturi. Acesta este un blocker pentru a pretinde că modelul local
   reproduce complet jocul original; discrepanța rămâne vizibilă în raport.
4. UI-ul original este rotit/necalibrat în emulatorul curent, cu text și cărți
   distorsionate. Capturile sunt păstrate pentru acceptanța vizuală, nu folosite
   drept dovadă de lizibilitate.
