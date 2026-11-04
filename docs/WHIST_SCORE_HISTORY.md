# Whist Lab — istoricul scorului

În timpul partidei, **Meniu joc → Scorul partidei** compară jucătorii la mâna curentă. După prima mână încheiată, **Istoricul scorului** arată evoluția jucătorului selectat. Ultima mână apare prima. De exemplu, după un total de 25, o penalizare de un punct apare ca `−1` la puncte și `24` la total. Selectorul și antetele rămân deasupra rândurilor care se derulează. Foaia este disponibilă și după ultima mână.

## Ce se salvează

| Moment | Date consultabile după reluare |
|---|---|
| Partidă nouă | Sold inițial zero pentru fiecare loc. O mână în desfășurare nu are încă punctaj. |
| Ultima carte jucată | Motorul aplică punctajul și seria o singură dată, apoi păstrează totalurile pe locuri. Diferența față de soldul precedent este punctajul mâinii. |
| Salvare v1–v3 în timpul jocului | Istoricul pornește de la soldul cunoscut după mâinile deja încheiate. Mâna activă apare când se termină. |
| Salvare v1–v3 la ecranul de scor | Licitația, levatele și punctele de serie permit recuperarea ultimei mâini. Pentru mâinile mai vechi apare soldul anterior. |
| Reluări succesive | Registrul se păstrează prin JSON. Mâna următoare continuă după ultima înregistrare. O partidă nouă pornește cu un registru propriu. |

Checkpoint-ul v4 cere numărul corect de mâini încheiate și câte un total întreg pentru fiecare loc. La reluare, motorul compară ultimul sold cu scorurile jucătorilor și, în faza de scor, cu rezultatul contractului. O salvare v1 cu levate lipsă în mâna activă își păstrează marcajul vechi până la o distribuție completă. Registrul de scor se salvează și în acest interval.

## Cum se verifică offline

În copia de build din TMP, indicați directorul unei arhive complete a pilotului românesc de șase mâini. Testul citește seed-ul, verifică SHA-256 pentru trace și rezultatul jocului, apoi introduce ofertele și cărțile în ordinea capturată. Compară rândurile efectiv furnizate interfeței după reîncărcări JSON în timpul jocului, la scor și la distribuția următoare.

```powershell
$pilot = 'C:\work\whist_back\ui-reports\whist-lab\runs\20261104-pilot-052110z'
.\gradlew.bat "-Pwhist.scoreHistoryTraceDir=$pilot" :app:testDebugUnitTest
```

Pilotul are patru jucători, șase mâini, nouă levate și 36 de cărți jucate. Cele 24 de perechi punctaj/total conduc la soldurile finale `25, 19, 26, 18` în ordinea locurilor. Testele de migrare și de corupere a salvării rulează și fără arhivă. Gradle marchează separat replay-ul ca omis dacă directorul nu este furnizat.

În ordinea locurilor, fiecare celulă arată punctele mâinii și totalul rezultat.

| Mână | Tu | Locul 2 | Locul 3 | Locul 4 |
|---:|---:|---:|---:|---:|
| 6 | −1 / 25 | +6 / 19 | +6 / 26 | +5 / 18 |
| 5 | +5 / 26 | +5 / 13 | −1 / 20 | +5 / 13 |
| 4 | +6 / 21 | −1 / 8 | +5 / 21 | +5 / 8 |
| 3 | +5 / 15 | −1 / 9 | +5 / 16 | +5 / 3 |
| 2 | +5 / 10 | +5 / 10 | +5 / 11 | −1 / −2 |
| 1 | +5 / 5 | +5 / 5 | +6 / 6 | −1 / −1 |

Capturile Extent provin din 0.1.3. Verificarea offline pe 0.1.4 acoperă calculul, localizarea rândurilor și persistența. Acceptanța vizuală a istoricului pe telefon are criteriile proprii. [Pilotul Extent și metadatele](feedback/whist-extent/README.md) · [Feedback UX](feedback/WHIST_LAB_User_Experience_Feedback.md)
