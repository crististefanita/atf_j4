# Modelul istoricului de scor

Istoricul păstrează licitația, punctele mâinii și totalul fiecărui loc după o mână încheiată. Prezentarea și persistența consumă același registru, iar datele necunoscute dintr-o salvare veche rămân marcate ca necunoscute.

Registrul curent stochează licitațiile și soldurile, iar punctele sunt diferențe de sold. Levatele istorice nu au un câmp separat; [cerința pentru completarea lor](../feedback/ATF_Android.md) are nevoie de schemă și migrare înaintea afișării.

## Fluxul datelor

```mermaid
flowchart LR
    END[Mână încheiată] -->|calculează| SCORE[Reguli de scor și serii]
    SCORE -->|adaugă o înregistrare| HISTORY[ScoreHistory]
    HISTORY -->|persistă| STORE[Checkpoint JSON]
    HISTORY -->|proiectează pe toate locurile| COLUMN[ScoreHistoryTable]
    COLUMN -->|afișează| VIEW[Istoricul UI]
```

<details>
<summary>Legendă: scor și prezentare</summary>

Scorul este calculat o singură dată la închiderea mâinii. Registrul păstrează totalurile și contractele în ordinea locurilor; tabelul UI urmează ordinea cronologică a programului, fără schimbarea proprietății valorilor.

</details>

## Invariante

| Contract | Regulă |
|---|---|
| Punctele mâinii | Diferența dintre totalul nou și soldul anterior, incluzând seria. |
| Licitația | Asociată explicit locului, chiar dacă acțiunile au fost capturate în altă ordine. |
| Înregistrare | O singură avansare pentru fiecare mână încheiată. |
| Reîncărcare | Ultimul sold trebuie să corespundă stării jucătorilor și fazei sesiunii. |
| Date vechi | Soldul cunoscut se păstrează; cărțile, licitațiile și mâinile neînregistrate nu sunt reconstruite prin presupunere. |
| Partidă nouă | Registru propriu și sold inițial validat. |

## Compatibilitate și UI

[GameSessionStore](../../whist-lab-app/app/src/main/java/ro/whistlab/app/persistence/GameSessionStore.java) definește migrarea fiecărei scheme și validarea checkpoint-ului. [ScoreHistory](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/ScoreHistory.java) definește integritatea registrului.

- Toți jucătorii au coloane proprii; antetele rămân deasupra rândurilor derulabile vertical și se deplasează împreună cu acestea la derularea orizontală.
- Programul complet include mâinile încheiate, mâna curentă și mâinile viitoare; punctele neacordate se afișează ca necunoscute, fără totaluri inventate.
- Contractul zero se afișează ca valoare, iar contractul necunoscut are marcaj și text accesibil distinct.
- Istoricul rămâne disponibil după terminarea partidei.
- Proiecția furnizează aceeași ordine și aceleași valori afișării și citirii asistate.

## Verificarea mecanismului

Componentele verifică închiderea mâinii, migrarea, coruperea checkpoint-ului și proiecția tuturor locurilor. Replay-ul unui trace extern este opt-in și verifică asocierea contract–puncte–total după reîncărcare, cu identitatea și hash-ul intrării controlate.

[Motorul](../../whist-lab-app/game-core/README.md) · [Persistența și interfața](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md)
