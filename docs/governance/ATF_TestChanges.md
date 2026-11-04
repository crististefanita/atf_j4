# Guvernanța schimbărilor în teste

O modificare a testelor păstrează sensul contractului și motivul pentru care o intrare este admisă sau refuzată. Schimbarea implementării nu justifică ajustarea rezultatului așteptat doar pentru a obține succes.

## Criterii

| Suprafață | Contract |
|---|---|
| Expected | Derivat din cerință, invariant sau oracol independent. |
| Control negativ | Eșecul intenționat demonstrează că testul detectează defectul vizat. |
| Identitate | Cazul, sursele, configurația și domeniul sunt explicite. |
| Verdict | Cauza originală se păstrează prin captură și publicare. |
| Populație | Selecția și omisiunile sunt declarate fără numărători istorice fixate în ghid. |
| Rezultate | Rulările concrete și comparațiile de versiuni se păstrează în arhiva externă. |

## Revizia unei schimbări

1. Formulați proprietatea generală și frontierele ei.
2. Reproduceți defectul într-un caz mic cu date controlate.
3. Verificați eșecul controlului negativ și succesul cazului valid.
4. Reconciliați sursele concurente înaintea calificării.
5. Actualizați colecția, contractul și documentația mecanismului în aceeași revizie.

[Guvernanța documentației](ATF_Documentation.md) · [Colecții](../testing/ATF_Collections.md)

## Mutarea proprietății unei verificări

Transferul unei implementări către o bibliotecă păstrează toate contractele native la producător. Consumatorul păstrează acceptanța API-ului folosit, identitatea JAR-ului încărcat și refuzurile pentru metadate ori livrări inconsistente.

- Cererea de schimbare identifică sursele afectate, proprietarul nou, înlocuirea echivalentă și criteriile independente de acceptare.
- Inventarele concrete, bazele Git, hash-urile și rezultatele transferului sunt înregistrări externe, legate de livrarea verificată.
- Ștergerea copiei locale nu suprimă un caz negativ și nu transformă o verificare de bibliotecă într-o acceptanță Android.
- Documentația curentă descrie limita de responsabilitate, fără liste istorice de teste șterse.

[Guvernanța calității](README.md) verifică perimetrul curent și interzice sursele paralele ale bibliotecilor binare.
