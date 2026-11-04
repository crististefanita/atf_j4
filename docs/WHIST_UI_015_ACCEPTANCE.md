# Whist Lab 0.1.5 — masă, atu și istoricul licitațiilor

[APK Android](../dist/WhistLab-0.1.5-debug.apk) · [SHA-256](../dist/WhistLab-0.1.5-debug.apk.sha256) · [Indexul Extent](feedback/whist-extent/README.md)

| Probă Android | Ce trebuie să se poată verifica în raport |
|---|---|
| [Meniu, atu și scor după reluare](<file:///C:/work/whist_back/ui-reports/whist-lab/runs/20261104-015-sanity-final-table-score/report.html#test-view>) | Cele șase variante din selector. O distribuție normală de Whist românesc. Rangul și culoarea atuului corespund descrierii accesibile. Chenarul are spațiu față de titlu și masă. O singură carte este centrată jos. După o mână și repornirea procesului, cele patru selecții de jucător păstrează licitația, punctele și totalul. |
| [Mâini de două și treisprezece cărți](<file:///C:/work/whist_back/ui-reports/whist-lab/runs/20261104-015-sanity-final-hand-fan/report.html#test-view>) | Două distribuții fixe pe același emulator. Se compară mijlocul rândului cu mijlocul zonei disponibile, distanța dintre cărți, linia de jos și distanța până la scaunul omului. |

## Ce aparține acestei versiuni

APK debug cu `versionCode 6`, `minSdk 24` și `targetSdk 36`. Hash-ul binarului instalat coincide cu cel construit în TMP. Fișierul de proveniență din fiecare arhivă identifică APK-ul și lista de surse, inclusiv versiunea locală de `PublicSalvage.java`. Emulatorul a rulat API 28 la 1080 × 1920 și 440 dpi. La final, aplicația a fost închisă și rezervarea emulatorului a fost eliberată.

**Incident de curățare pe emulator.** Scriptul temporar a interpretat eroarea Android `run-as: exec failed for test: Permission denied` ca absență a fișierelor salvate. A șters preferințele partidei și ale activității. La verificarea directă, `shared_prefs` era gol. Cele două verdicte JUnit acoperă scenariile din aplicație. Restaurarea stării existente înaintea lor a eșuat. Există un checkpoint independent de la sfârșitul pilotului de șase mâini. Identitatea sa cu ultima partidă a emulatorului nu a putut fi stabilită.

La cererea utilizatorului am restaurat pilotul arhivat, în faza SCORE după mâna 6 din 24. Hash-ul checkpoint-ului v3 coincide cu cel înregistrat în trace. Ofertele și soldurile pentru toate cele șase mâini provin din același trace. După deschiderea tabelei am comparat rândurile jucătorului și am recitit salvarea după închiderea aplicației. Totalurile sunt **25, 19, 26, 18**. [Captura celor șase mâini](<file:///C:/work/whist_back/ui-reports/whist-lab/recovery/20261104-pilot-six/restored-history.png>)

Pentru următoarea probă pe un emulator partajat, exportați preferințele în afara dispozitivului și comparați hash-urile înainte de prima acțiune asupra jocului. Păstrați copia până după verificarea restaurării. Orice eroare de citire sau comparație oprește curățarea.

Componentele verifică geometriile de la una la 17 cărți, cu cinci lățimi, patru densități și trei mărimi de text. Pentru un rând mai lat decât fereastra, marginile pot fi aduse în vedere prin derulare. Replay-ul independent al pilotului 0.1.3 leagă licitația fiecărei mâini de locul corect și verifică pe 0.1.5 cele 24 de rezultate după reîncărcarea salvării. [Tabelul pe șase mâini](WHIST_SCORE_HISTORY.md)

## Rezultate

| Verificare | Rezultat |
|---|---|
| Componente în TMP | 34 teste Android JVM și 82 teste de motor. Zero eșecuri și zero omisiuni, inclusiv replay-ul arhivat. |
| Sanity Android final | Două metode JUnit trecute. Șapte capturi pentru meniu, atu și istoric, două pentru mâinile fixe. |
| Revizie Extent în Edge | Meniu și scor — 18 noduri și 23 de secțiuni expandate. Mâinile fixe — 8 noduri și 8 secțiuni expandate. Ultimul rând al fiecărui tabel de aserțiuni a fost accesibil. Vizualizarea capturii la mărime originală și revenirea au păstrat expandările. |

## Trasabilitatea încercărilor

În prima încercare, jocul a trecut verificările de meniu și scor. Publicatorul a respins lipsa observației UI explicite din fiecare pas. A doua încercare a depășit limita de 240 de caractere în descrierea atuului și în lista dreptunghiurilor. Aserțiunile publicate descriu acum identitatea atuului sau dimensiunile capetelor rândului. XML-ul păstrează geometria completă. Metadatele încercărilor și rezultatul JUnit disting eșecul publicării de starea jocului. [Istoricul publicării](<file:///C:/work/whist_back/ui-reports/whist-lab/LATEST.md>)

Validarea vizuală acoperă emulatorul în portret cu fontul implicit. Citirea cu TalkBack, profilul fizic ROG 7 și navigarea prin istoric la text 150% au criterii separate în [feedback-ul UX](feedback/WHIST_LAB_User_Experience_Feedback.md).
