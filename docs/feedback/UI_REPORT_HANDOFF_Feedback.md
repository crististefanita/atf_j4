# Contractul handoff-ului Android

## Responsabilitate

Publicatorul consumerului și runnerul Android dețin înregistrarea execuției și pointerele externe. Rendererul generic primește contractul tipizat și nu compensează lipsa indexării consumerului.

## Lacune de proiectare

- Tipul de republicare offline nu poate reprezenta implicit o execuție Android nouă.
- Trecerea JUnit și a auditului structural nu produce automat acceptanță interactivă.
- Execuția din staging poate pierde contextul checkout-ului; destinațiile trebuie transmise explicit.
- Publicarea rezultatelor trebuie să scrie în arhiva externă, fără a genera inventare de rulări în README-urile proiectului.

## Contract cerut

| Suprafață | Cerință |
|---|---|
| Încercare | Verdictul observat, identitatea scenariului, dispozitivului și aplicației. |
| Publicare | Inputul original, integritatea HTML-ului și proprietatea checkpoint-urilor. |
| Acceptanță | Reviziile cerute de scop, legate de același conținut publicat. |
| Staging | Rădăcină de publicare configurată explicit, independentă de descoperirea Git. |
| Pointere | Ultima încercare și ultima acceptanță independente, ordonate după execuție. |

## Criterii de închidere

- Refuzați identități incompatibile, hash schimbat, raport lipsă și verdict nativ inconsecvent.
- Înregistrați timeout-ul și eșecul fără acceptanță implicită.
- Verificați că o republicare veche nu devine ultima execuție nouă.
- Verificați că publicarea nu rescrie documentația editorială a proiectului.

[Mecanismul de handoff](../WHIST_UI_REPORT_HANDOFF.md) · [Arhitectura publicării](../reporting/README.md)
