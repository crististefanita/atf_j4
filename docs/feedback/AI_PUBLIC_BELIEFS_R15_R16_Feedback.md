# Whist Lab — inferență publică și licitații condiționate

Owner Whist Lab. Offline, 3 noiembrie 2026. J18 este consumat ca bibliotecă; nu s-a reprodus un defect generic Reporting și nu este cerut un patch modulul de biblioteci comune.

| Prioritate | Dovadă | Cerință verificabilă |
|---|---|---|
| P0 | R15 numai licitație: +30 puncte, +1 exact2–8 agregat, dar −2 exacte contra Hard R14. | Respingeți candidatul conform gate-ului de familie. Nu selectați numai agregatul favorabil. |
| P0 | R15 licitație+joc: −55 puncte, −5 exacte. 55 dintre70 de contracte nou pierdute sunt overshoot. | Nu activați ponderile. O predicție mai bună a licitației observate nu certifică utilitatea unei continuări. |
| P0 | Regresia R13 fresh consumată are105 overshoot din144 pierderi noi. | Separați ablația inferenței de ablația politicii și estimarea licitațiilor viitoare. Rezultatul terminal nu este o etichetă de mutare optimă. |
| P1 | Lipsa culorii este dovedită de reguli; licitația zero poate coexista cu atuuri majore. | Restricțiile publice elimină lumi; indiciile comportamentale păstrează suport pozitiv. Nu transformați o presupunere în certitudine. |
| P1 | Matching-ul amestecat nu este dovedit uniform; modelul observă șase seed-uri de validare. | Folosiți termenul „ponderi de model”. Nu declarați posterior calibrat sau perfecțiune. |
| P1 | Coeficienții sunt fixați, iar lumea posibilă și prefixul se schimbă. | YAML trebuie să separe coeficientul de contextul dinamic; generatorul și testul de identitate verifică documentația. |
| P1 | Testul de licitații viitoare injectate verifică doar `worldWeight`, nu întregul `chooseBid`. | Limitați textul dovezii la seam-ul efectiv verificat. Originalele R15 se păstrează; noua captură R16 are formularea corectă. |
| P1 | Estimarea veche a licitațiilor viitoare însuma onoruri fără prefix condiționat. | R16 schimbă numai acest seam; păstrează bidurile publice, contractul propriu, regulile, continuarea și gardurile acceptate. Calificați separat pe jocuri noi. |
| P2 | Arhiva brută repetă aceleași lecții; manifestul activ are100 de jocuri. | Nu adăugați toate turneele în regresia zilnică. Casebook-ul are maximum un exemplu per mărime2–8 și nu expune mâinile private adverse. |
| P2 | Gate-urile locale aveau nume identice între experimente. | Producătorii noi includ cohorta în ID. În raport se copiază capturile originale byte-for-byte și se afișează FAIL-urile reale. |

R16 este respins în development: +100 puncte și +6 exacte agregat, dar −1 victorie contra Hard și −1 exact contra independentului.

Nu deschidem lotul final și nu selectăm retrospectiv numai mărimile cu delta pozitivă. Cele510 componente sunt PASS; forța de joc este FAIL. Interfața și acceptanța Android rămân la iterația lor.
