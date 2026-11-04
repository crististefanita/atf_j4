# Principii de utilizare și verificare a interfeței

Interfața trebuie să permită identificarea cărților, înțelegerea obligațiilor și efectuarea unei acțiuni fără pierderea contextului partidei. Criteriile se aplică profilurilor de ecran și accesibilitate declarate.

## Contracte de prezentare

| Suprafață | Cerință |
|---|---|
| Cărți | Rangul și culoarea rămân identificabile prin suprapunere, selecție și derulare. |
| Masa | Geometria separă locurile jucătorilor, centrul și mâna proprie. |
| Licitare | Controalele legale au rol semantic distinct de valorile de scor cu același text. |
| Reguli | O valoare de licitare exclusă are explicație contextuală fără confundarea ei cu un control lipsă. |
| Atu | Culoarea și cartea întoarsă au reprezentări distincte și descriere accesibilă. |
| Istoric | Contractul, punctele și totalul sunt asociate locului și mâinii corecte. |
| Localizare | Etichetele și descrierile asistate corespund limbii selectate. |
| Navigare | Consultarea scorului și revenirea păstrează starea interacțiunii de joc. |

## Frontiere de verificare

- Dimensiuni logice, densități și text mărit.
- Primul și ultimul element din conținutul derulabil.
- Primul viewport și atingerea completă a țintelor.
- Focus asistat și citirea valorilor necunoscute sau negative.
- Intersecții de regiuni, clipping și contrast.
- Asocierea capturii cu aceeași stare semantică observată.
- Dispozitiv fizic separat de simularea rezoluției logice.

## Principii pentru oracole

Prezența unui nod sau dreptunghi nu dovedește singură lizibilitatea. Controalele semantice și geometrice se completează cu verificări raster focalizate și revizie vizuală.

Un defect nou se reduce la o componentă ori stare de display cu proprietate explicită. Situația controlată verifică mecanismul de randare, iar jocul normal verifică integrarea lui cu starea domeniului.

[Atlasul UI](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md) · [Scor](../WHIST_SCORE_HISTORY.md) · [Profile de ecran](../WHIST_RESPONSIVE_UI_SANITY.md)
