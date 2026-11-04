# Colecție mică pentru diagnosticarea jocului offline

[Manifestul](../config/whist-learning-focus.json) selectează nouă partide de dezvoltare pentru înțelegerea erorilor mari de contract. Statutul său este `DIAGNOSTIC_NOT_OPTIMAL_ACTION_REGRESSIONS`.

## Ce conține selecția

| Indicator din manifest | Valoare |
|---|---|
| Partide selectate | 9 |
| Octeți ai partidelor | 561.259 |
| Grupuri de seed-uri | 8 |
| Manifest părinte | 100 de partide |
| Categorii cerute și acoperite | 28 |
| Rulare implicită sau turneu implicit | Nu |

- Categoria este `expert/independent × 2–8 cărți × over/under`.
- Se cere o diferență absolută de cel puțin două între licitat și câștigat.
- `over` înseamnă mai multe levate câștigate decât licitate; `under` înseamnă mai puține.
- Aceste categorii descriu rezultate observate, fără a măsura acoperirea codului sau acțiunea optimă.
- Controlerele istorice includ versiuni anterioare ale ambilor jucători.

## Selecția deterministă

La fiecare pas se alege partida care acoperă cele mai multe categorii încă neacoperite. Egalitățile păstrează ordinea exactă din manifestul părinte legat prin SHA-256.

| Ordine | Seed | ID salvat | Categorii noi |
|---|---|---|---|
| 1 | 7100 | `arena-safety10-arena-mixed-seed7100-seating4` | 8 |
| 2 | 76200 | `round11-enum16-development-player-expert_v10-seed76200-seat2-after` | 6 |
| 3 | 7106 | `arena-safety10-arena-factorial-seed7106-seating3` | 4 |
| 4 | 6705 | `round7-rollout01-development-expert-expert_v6-seed6705-seat1-after` | 3 |
| 5 | 7100 | `arena-safety10-arena-factorial-seed7100-seating0` | 2 |
| 6 | 5501 | `round5-large01-development-player-expert_v4-seed5501-seat0-before` | 2 |
| 7 | 6703 | `round7-mixed08-development-expert-player_v6-seed6703-seat0-after` | 1 |
| 8 | 6704 | `round7-mixed08-development-expert-expert_v6-seed6704-seat1-after` | 1 |
| 9 | 6402 | `round6-belief02-development-player-player_v4-seed6402-seat2-after` | 1 |

Două partide folosesc seed 7100. Ele nu reprezintă două grupuri independente de seed-uri.

## Transformarea într-o regresie mică

1. Verifică SHA-256 față de manifest și partiția de dezvoltare.
2. Rejoacă independent regulile și scorul celor 24 de mâini.
3. Extrage numai cărțile proprii și informația publică disponibilă actorului.
4. Stabilește așteptarea printr-o regulă, un invariant sau un oracol complet și limitat.
5. Păstrează cel mai mic caz care exprimă lecția.

| Intrare permisă în decizie | Date rezervate auditului |
|---|---|
| Cărțile actorului | Mâinile reale ale adversarilor |
| Licitații deja cunoscute | Licitații încă nedepuse |
| Cărți jucate, masă și carte întoarsă | Continuarea efectivă încă nejucată |
| Număr de cărți, levate și scoruri publice | Rezultatul final folosit ca etichetă de mutare optimă |

O înfrângere salvată nu dovedește că mutarea a fost greșită. Nici o victorie salvată nu justifică o așteptare de regresie.

## Contraste istorice prioritare

Manifestul păstrează separat trei contraste din experimentul respins R15_B, anterior corecției limitei de atu. Ele au statutul `PRIORITY_CONTRAST_NOT_PROVEN_ERROR`.

| Seed | Observator | Mână | Cărți | Prima schimbare |
|---|---|---|---|---|
| 15100 | 0 | 9 | 6 | JS → QH |
| 15104 | 2 | 7 | 4 | QD → 8D |
| 15105 | 2 | 6 | 3 | KH → QC |

- În fiecare caz, prima diferență de carte și un contract exact pierdut apar în aceeași mână.
- Coincidența localizează analiza; nu dovedește cauzalitate sau o mutare optimă.
- Manifestul leagă SHA-ul sursei, invocările, partidele și contextul public.
- Rezultatele aparțin sursei istorice înghețate, fără a măsura motorul curent.

Selecția nu modifică manifestul părinte, pointerul activ sau partidele și nu autorizează citirea unui lot nou de evaluare. O schimbare de strategie cere propriul protocol și propria evaluare separată.
