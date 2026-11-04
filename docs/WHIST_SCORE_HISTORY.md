# Whist Lab — istoricul scorului

În timpul partidei, **Meniu joc → Scorul partidei** compară jucătorii la mâna curentă.

- După prima mână încheiată, **Istoricul scorului** arată evoluția jucătorului selectat.
- Ultima mână apare prima.
- Cele patru coloane sunt **Mână**, **Licitat**, **Puncte** și **Total**.
- Licitația zero apare ca `0`.
- O licitație veche necunoscută apare ca `—` și se citește „neînregistrat” cu cititorul de ecran.
- De exemplu, după un total de 25, o penalizare de un punct apare ca `−1` la puncte și `24` la total.
- Selectorul și antetele rămân deasupra rândurilor care se derulează.
- Foaia este disponibilă și după ultima mână.

## Ce se salvează

| Moment | Date consultabile după reluare |
|---|---|
| Partidă nouă | Sold inițial zero pentru fiecare loc. O mână în desfășurare nu are încă punctaj. |
| Ultima carte jucată | Motorul aplică punctajul și seria o singură dată, apoi păstrează totalurile pe locuri. Diferența față de soldul precedent este punctajul mâinii. |
| Salvare v1–v3 în timpul jocului | Istoricul pornește de la soldul cunoscut după mâinile deja încheiate. Mâna activă apare când se termină. |
| Salvare v1–v3 la ecranul de scor | Licitația, levatele și punctele de serie permit recuperarea ultimei mâini. Pentru mâinile mai vechi apare soldul anterior. |
| Salvare v4 | Punctele și totalurile deja înregistrate rămân consultabile. La ecranul de scor se recuperează licitațiile ultimei mâini din contractele jucătorilor. Licitațiile mai vechi rămân necunoscute. |
| Reluări succesive | Registrul se păstrează prin JSON. Mâna următoare continuă după ultima înregistrare. O partidă nouă pornește cu un registru propriu. |

Checkpoint-ul v5 păstrează și ofertele, în ordinea locurilor, pentru fiecare mână înregistrată de versiunea nouă.

- La încărcare sunt verificate valorile permise, suma ultimei licitații și corespondența cu contractele mâinii curente încheiate.
- Registrul cere numărul corect de mâini încheiate și câte un total întreg pentru fiecare loc.
- La reluare, motorul compară ultimul sold cu scorurile jucătorilor și, în faza de scor, cu rezultatul contractului.
- O salvare v1 cu levate lipsă în mâna activă își păstrează marcajul vechi până la o distribuție completă.
- Registrul de scor se salvează și în acest interval.

## Cum se verifică offline

În copia de build din TMP, indicați directorul unei arhive complete a pilotului românesc de șase mâini. Testul citește seed-ul, verifică SHA-256 pentru trace și rezultatul jocului, apoi introduce ofertele și cărțile în ordinea capturată. Compară rândurile efectiv furnizate interfeței după reîncărcări JSON în timpul jocului, la scor și la distribuția următoare.

```powershell
$pilot = (Join-Path $env:WHIST_EVIDENCE_ROOT 'ui-reports\whist-lab\runs\20261104-pilot-052110z')
.\gradlew.bat "-Pwhist.scoreHistoryTraceDir=$pilot" :app:testDebugUnitTest
```

Pilotul are patru jucători, șase mâini, nouă levate și 36 de cărți jucate.

Cele 24 de combinații licitație/punctaj/total conduc la soldurile finale `25, 19, 26, 18` în ordinea locurilor. Testele de migrare și de corupere a salvării rulează și fără arhivă. Gradle marchează separat replay-ul ca omis dacă directorul nu este furnizat.

În ordinea locurilor, fiecare celulă arată **licitația / punctele mâinii / totalul**. Licitațiile din trace sunt în ordinea acțiunilor și sunt asociate explicit locurilor înainte de verificare.

| Mână | Tu | Locul 2 | Locul 3 | Locul 4 |
|---:|---:|---:|---:|---:|
| 6 | 0 / -1 / 25 | 1 / +6 / 19 | 1 / +6 / 26 | 0 / +5 / 18 |
| 5 | 0 / +5 / 26 | 0 / +5 / 13 | 1 / -1 / 20 | 0 / +5 / 13 |
| 4 | 1 / +6 / 21 | 1 / -1 / 8 | 0 / +5 / 21 | 0 / +5 / 8 |
| 3 | 0 / +5 / 15 | 0 / -1 / 9 | 0 / +5 / 16 | 0 / +5 / 3 |
| 2 | 0 / +5 / 10 | 0 / +5 / 10 | 0 / +5 / 11 | 0 / -1 / -2 |
| 1 | 0 / +5 / 5 | 0 / +5 / 5 | 1 / +6 / 6 | 1 / -1 / -1 |

Pilotul de șase mâini provine din 0.1.3 și este verificat prin replay pe 0.1.5. Capturile Android pentru noua tabelă provin dintr-o probă separată pe 0.1.5. [Rapoartele și limitele acceptanței](WHIST_UI_015_ACCEPTANCE.md)
