# R12 — respingeri și cerințe pentru următoarea îmbunătățire

Owner J4. Offline, 3 noiembrie 2026. Nu se cere modificarea JAR-ului sau rendererului în J0.

| Prioritate | Dovadă | Cerință verificabilă |
|---|---|---|
| P0 | Câștigul independentului de +131 puncte / +13 exacte în development devine +24 / −4 în evaluarea nouă; victoriile scad cu 7. | Respingeți candidatul. Păstrați politica R11 și publicați FAIL-ul original. Nicio alegere de seed-uri, prag sau pondere după evaluare. |
| P0 | Per-world, o deschidere diferită poate inventa o medie de succes imposibilă pentru o singură decizie publică. | Separați alegerea înainte de informație de alegerea după informație. Contraexemple mici cu două lumi indistinguibile; expected literal și scope explicit. |
| P1 | Mediile de puncte pot crește deși două cărți pierd 8 contracte exacte și unele familii pierd victorii. | Publicați separat rolul, familia și mărimea mâinii; nu ascundeți regresiile în agregat sau în câștigul la opt cărți. |
| P1 | Modelul unor adversari mai prevăzători sau o căutare completă într-o lume nu devin automat mai bune în jocul real. | Păstrați modelele respinse în `research`, în afara regresiei zilnice și a APK-ului. Expected-ul modelului nu devine etichetă de mutare optimă cu informație ascunsă. |
| P1 | Metadatele unui experiment menționau 96 PLAY, sursa executa 48 PLAY. | Originalele rămân; neconcordanța este declarată. Controalele noi de 64/24 verifică obiectul de dovezi executat, nu o valoare presupusă din protocol. |
| P1 | Un snapshot istoric cerea aceeași medie numerică după schimbarea estimatorului. Mixture a avut 519/520 PASS. | Păstrați FAIL-ul istoric; nu îl transformați în calificare. Delimitați semantica/unitatea dovezii de identitatea unui estimator înghețat. |
| P2 | La 55 de origini protejate, selecția greedy la 96 de jocuri omite șase categorii. | Schimburi de acoperire fără pierderea originilor; trei controale pentru cap, determinism și alias protejat. Declarați capul final și lacunele efective. |
| P2 deschis | Șapte contraexemple de dependență a RNG de translația comună a scorului rămân în cercetare. | Nu declarați invarianta rezolvată și nu reactivați varianta de normalizare respinsă anterior. |

J18 fizic este consumat fără modificări de bibliotecă. Noile costuri și selecții sunt responsabilitatea inputului J4. Nu a fost reprodus un defect nou generic Reporting. Interfața și acceptanța Android rămân la iterația și ownerul lor.

## Închiderea contractului Normal/Hard

Cerința nouă de două niveluri este implementată și verificată separat: 486 componente de domeniu, quick 162 ca subset, 7 research, 17 app JVM și o pereche completă Hard/R11. `EXPERT` este numai compatibilitate de sursă/salvare. Perfecțiunea generală rămâne nedemonstrată; nu folosiți acest adjectiv în interfață sau în acceptanță. J18 publică 27 capturi/2.802 comparații în 6.272.901 bytes, cu FAIL-ul real al evaluării de forță păstrat și auditul structural trecut. Nu este cerut un patch Reporting.

[Contract și dovezi Normal/Hard](../TWO_GAME_LEVELS.md). Sursele APK 0.1.1 sunt delimitate de integrarea UI 0.1.2 modificată concurent; aceste schimbări nu au fost suprascrise și nu sunt prezentate ca verificate pe Android de această iterație.
