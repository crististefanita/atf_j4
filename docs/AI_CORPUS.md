# Corpus de partide

O partidă completă este un fișier JSON. Fiecare mână înregistrează dealerul, distribuția, cartea întoarsă, licitațiile în ordinea depunerii, levatele și scorurile pe locuri.

| Întrebare | Câmpuri sau verificare |
|---|---|
| Ce știa jucătorul înainte de decizie? | Mâna proprie, ofertele deja anunțate, cărțile publice, levatele câștigate și scorul. |
| Ce poate verifica oracolul după partidă? | Identitatea și proprietatea cărților, ordinea, urmarea culorii, obligația de atu, câștigătorul, plata seriei și fiecare total. |
| Cum se compară două politici? | Aceeași distribuție, același loc, aceiași adversari și profil. Seed-ul se păstrează ca șir pentru a conserva valoarea Java long. |
| Ce intră în regresia zilnică? | Exemple mici cu așteptare independentă. Selecția de învățare pornește dintr-un manifest cu hash-uri. |

## Date pentru învățare și verificare

- Directorul `games/development` conține partidele permise pentru analiză și export de exemple.
- Directorul `games/heldout` conține evaluările rezervate. Un lot deschis se înregistrează ca expus.
- Directorul `games/reference` păstrează referințele. Replay-ul citește fișierul ales, iar validarea întregului corpus parcurge arhiva.
- Exportul de exemple separă observația de acțiune și rezultat. Cărțile reale ascunse ale adversarilor rămân disponibile oracolului după partidă.

## Comenzi

```powershell
$corpus = Join-Path $env:WHIST_EVIDENCE_ROOT 'training-corpus'
$game = Join-Path $corpus 'games/development/android-round-v2-21049369348797.json'
node scripts/whist-corpus-cli.mjs replay $game
node --test scripts/whist-corpus.test.mjs
```

[Selecția de nouă partide](AI_LEARNING_FOCUS.md) și [protocolul evaluării](AI_TRAINING_PROTOCOL.md) descriu trecerea de la pierdere observată la regresie justificată.
