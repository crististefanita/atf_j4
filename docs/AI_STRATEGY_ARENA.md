# Compararea strategiilor la aceeași masă

Arena rulează partide românești complete cu patru locuri și profilul de serie +10. Configurația stabilește seed-urile, strategiile și permutările locurilor.

## Cum se citește rezultatul

| Observație | Interpretare |
|---|---|
| Patru profiluri la aceeași masă | Fiecare primește propria mână și informația publică disponibilă la decizie. |
| Același seed, alte locuri | Rotația reduce avantajul locului. Partidele din același grup împart distribuțiile. |
| Contract ratat | Se verifică licitația, levatele câștigate, cărțile rămase și prima decizie care diferă. |
| Câștig într-o ligă | Este un rezultat de dezvoltare. Promovarea cere un lot rezervat și comparații în perechi pentru rolul evaluat. |

## Dintr-o pierdere într-un test mic

1. Reconstruiți starea observabilă chiar înaintea mutării.
2. Separați restricțiile certe de ipotezele sugerate de licitații.
3. Construiți o situație de control în care aceeași carte mare trebuie păstrată sau eliminată din alt motiv.
4. Verificați cazul cu un oracol independent și comparați politica pe distribuții noi.

[Protocolul arenei](../config/strategy-arena.json) · [Colecțiile disponibile](TEST_COLLECTIONS.md) · [Reguli contextuale](rules/AI_HAND_PATTERNS_RO.md)
