# Evaluarea offline a unui jucător

Fiecare candidat reprezintă o modificare identificată a licitării sau a alegerii cărții. Protocolul precizează rolul evaluat, sursele, familiile de adversari, profilul de scor și distribuțiile înaintea execuției.

| Etapă | Ce se verifică |
|---|---|
| Diagnostic | Se extrage o observație publică dintr-o partidă de dezvoltare. Un contraexemplu trebuie justificat independent de rezultatul înregistrat. |
| Componentă | Se verifică legalitatea mutării, necesarul contractului, cartea întoarsă și cazurile în care concluzia încetează să fie valabilă. |
| Dezvoltare | Fiecare pereche folosește distribuții și locuri identice. Rezultatele se separă pe rol, familie de adversari și mărimea mâinii. |
| Evaluare rezervată | Sursele sunt înghețate înainte de deschiderea intervalului. Registrul extern păstrează intervalele utilizate. |
| Decizie | Se aplică pragurile declarate și cerința de progres strict. Rezultatele neutre și negative rămân disponibile. |

## Interpretare

- Un seed cu locurile rotite dă mai multe partide corelate.
- Contractele exacte, punctele și victoriile împărțite la egalitate se citesc separat.
- Numărul lumilor fezabile enumerate este o măsură a domeniului demonstrației. O probabilitate necesită și un model al distribuției lor.
- O alegere diferită poate schimba licitațiile și scorurile mâinilor ulterioare. Prima divergență trebuie identificată înainte de atribuirea câștigului.

## Reproducere

Runnerul selectează turneul din schema protocolului și refuză candidații incompatibili cu sursele înghețate. Rezultatele vechi se reproduc din snapshot-ul lor.

[Colecțiile și parametrii](TEST_COLLECTIONS.md) · [Arena](AI_STRATEGY_ARENA.md) · [Corpusul](AI_CORPUS.md) · [Dovezile istorice](history/README.md)
