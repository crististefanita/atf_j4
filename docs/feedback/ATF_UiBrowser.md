# Adaptor comun de browser pentru jocurile de cărți

## Pe scurt

**WEB-01** folosește [adaptorul comun](../../atf-ui-browser/README.md) pentru Whist, Rentz și Hold’em. Ediția Whist de pe GitHub Pages rulează motorul compilat în browser și salvează local profilul, realizările, partida și clasamentele. Calificarea identității cu APK-ul rămâne deschisă până la comparația pe același telefon.

## Calificare rămasă

| Arie | Criteriu rămas |
|---|---|
| Telefon Android | Compară același profil util, aceleași sesiuni și capturi APK/browser. Verifică fonturile, etichetele, rândurile, dialogurile și comenzile în ambele limbi. |
| Funcții de prezentare | Califică zonele tactile ale mâinii suprapuse, marcajele și fața compactă la densități și scări de text diferite. Compară atingerea și focusul în APK și Chrome pe același telefon. Rulează de la același checkpoint o ridicare manuală, apoi AutoGame până la scor. Măsoară zona utilă și păstrează capturi din colectare. |
| Consumatori | Folosește JAR-ul și probele comune pentru integrare. Confruntă separat fluxurile Rentz și Hold’em cu APK-urile proprii, inclusiv variantele de joc și documentele de calificare ale fiecărui consumator. |
| Publicare online | Pentru Whist, verifică pe un telefon redeschiderea ediției Pages cu același profil. Pentru Rentz și Hold’em, califică pachetul fiecărui joc înainte de publicarea adresei sub APK. [Fluxul Pages](../../atf-ui-browser/README.md#whist-pe-github-pages) |

## Calificarea pe același dispozitiv

| ID | Schimbare | Criteriu de acceptare |
|---|---|---|
| WEB-07 | Califică aceeași partidă în Chrome și în APK pe același telefon, cu aceleași date de intrare. | Confruntă setările, Joacă din nou, istoricul cu ajustări și reluarea după închiderea aplicației. Capturează RO/EN la aceeași dimensiune utilă și scară de text pe Android. |
| WEB-11 | Califică vizual pe Android mărirea cifrelor 2–10. | Capturează APK-ul la 360 × 800 și 414 × 896 cu text mărit. Verifică dacă `10` rămâne complet vizibil în evantai, pe masă și în zona de atu. Compară cu proba Playwright din [verificarea adaptorului](../../atf-ui-browser/README.md#verificare). |

## Profilurile de referință

Recomandarea pentru primul prototip desktop este un cadru portrait de **414 × 896 în unități logice**. Această dimensiune conduce eșantionul Statcounter de navigare mobilă din octombrie 2026, cu 12,67% la nivel global și 17,89% în Europa. Eșantionul măsoară afișări de pagini web, iar spațiul util al aplicației depinde de zonele sistemului. [Date globale](https://gs.statcounter.com/screen-resolution-stats/mobile/worldwide/#monthly-202610-202610-bar) · [Date europene](https://gs.statcounter.com/screen-resolution-stats/mobile/europe/#monthly-202610-202610-bar) · [Metodologie](https://gs.statcounter.com/faq)

| Profil | Rol în acceptanță |
|---|---|
| 414 × 896, cu 1242 × 2688 px la 480 dpi ca profil Android de probă | Candidat pentru afișarea centrată implicită. Măsoară spațiul util și vizibilitatea mâinii la același cadru pe APK, în Edge și în Chrome. |
| 384 × 832 și 360 × 800 în unități logice | Verifică accesul la comenzi și comportamentul mâinii în cadre mai înguste. |
| 1200 × 1920 px la 440 dpi (aprox. 436 × 698 dp) | Reper lat, calificat în Whist cu toate cele 17 cărți vizibile simultan. La 720 × 1280/360 dpi și 1080 × 1920/440 dpi, aceeași probă a accesat toate cărțile prin derulare. [Dosarul APK](../../release/README.md) · [Strategia de afișaj](../testing/ATF_Display.md) |

- Măsoară dreptunghiul util APK din captură și ierarhie după zonele rezervate sistemului. Fixează dimensiunile normalizate, limba, fontul și SHA-ul APK-ului. La 414 × 896 verifică dacă toate cele 17 cărți încap simultan sau devin accesibile prin derulare, în aceleași condiții pe ambele platforme.

## Afișare pe telefon și Windows

În ambele medii, antetul, locurile, comenzile și cărțile folosesc aceleași resurse, mărimi de text, spațieri și reguli de rearanjare ca pe APK. Măsurătorile țin cont de zonele rezervate de sistem și de browser, de orientare și de mărirea textului.

| Mediu | Contract de afișare |
|---|---|
| Telefon Android | Folosește viewportul mobil la lățimea dispozitivului și aplică aceleași reguli de layout și derulare ca APK-ul pe același dispozitiv. Compară în zona utilă comună aceleași ancore, dimensiuni, rânduri de text, cărți și controale vizibile, în aceeași stare de joc. |
| Browser pe Windows | Cu zona Android utilă de referință `Wref × Href`, pornește de la scara `Hdisponibil / Href` și folosește lățimea `Wref × scară`. Dacă nu încape, limitează scara la `Wdisponibil / Wref`. Centrează rezultatul pe ambele axe, cu aceeași scară inclusiv pentru cărți, text și zonele interactive. |
| Ferestre foarte mici | Permite derularea dacă încadrarea ar face textul ori țintele interactive prea mici. Păstrează accesul la toate controalele și cărțile și înregistrează separat dimensiunea utilă în CSS, zoomul și `devicePixelRatio`. [CSSOM View](https://www.w3.org/TR/cssom-view-1/#dom-window-devicepixelratio) |

## Responsabilități

| Strat | Contract propus |
|---|---|
| Whist Lab, modulul comun | Deține versiunea modulului, cadrul portrait, contractele de geometrie, antetul, locurile, cărțile, controalele, dialogurile, focalizarea și probele reutilizabile. Randarea DOM folosește aceleași resurse și contractele portabile ale interfeței comune. [Interfața comună](../../whist-lab-app/atf-card-table-ui/README.md) |
| Adaptor de joc | Leagă comenzile și proiecția de motorul JVM al jocului. Prima implementare poate rula local, cu o sesiune per partidă, un checkpoint versionat și reluare după reîncărcare. Numerele de revizie permit refuzul comenzilor repetate sau trimise pe o stare veche. [Motorul Whist](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/GameSession.java) |
| Extensii în consumatori | Whist aduce licitarea și levatele. Rentz adaugă alegerea sub-jocurilor, secvențele și punctajul specific. Hold’em adaugă străzile, acțiunile de pariere, poturile și clasamentul turneului. Fiecare consumator declară versiunea comună și deține datele, localizarea și oracolele proprii. |
| Intrare și vizibilitate | Mouse-ul și tastatura trimit aceleași comenzi ca atingerea pe APK. DOM-ul păstrează rolurile accesibile și ordinea focalizării. Proiecția către pagină include doar cărțile permise jucătorului curent și evenimentele publice. |
| Acceptanță | O distribuire controlată și o secvență de comenzi reproduc partida pe Android și în browser. Compară starea publică după fiecare comandă, inclusiv refuzurile, și atașează capturi native și web cu metadatele ambelor medii. [Strategia de testare](../testing/README.md) |

## Criterii de închidere

1. Rulează o partidă românească 1–8–1 de patru jucători, Hard, cu aceeași distribuire și aceleași comenzi. Compară recomandarea, mutările legale, actorul, fiecare levată, scorul, seriile, Jocul la 0 și clasamentul. Componentele extind paritatea la modurile Normal și Hard, variantele și mesele de 3–6 locuri, AutoGame și reluarea fără credit dublu.
2. Pe același telefon Android, compară APK-ul cu Chrome și Edge după fiecare tranziție reprezentativă, inclusiv licitare, joc, scor, istoric și realizări. Cere aceeași compoziție, aceeași împărțire a textului pe rânduri și aceeași vizibilitate a cărților. Acceptă maximum o unitate logică pe margine pentru rotunjire și documentează separat diferențele de rasterizare ale textului.
3. Pe Windows, verifică în Edge și Chrome ferestrele de 1280 × 720 și 1920 × 1080, zoomul de 100% și 125% și o fereastră îngustă. Măsoară înălțimea utilă, scara unică, lățimea rezultată și distanțele la margini. Verifică și că focalizarea, dialogurile și toate comenzile sunt accesibile cu tastatura.
4. Califică integrarea comună prin câte un parcurs public reprezentativ de Rentz și Hold’em cu un oracol din motorul lor. Păstrează scenariile detaliate în proiectul fiecărui joc.
5. Publică în Extent distribuirea, comenzile, comparațiile și capturile inline pentru cele două platforme. Criteriul cere o partidă completă Whist și verificarea vizuală în ambele browsere, cu Android drept referință.
