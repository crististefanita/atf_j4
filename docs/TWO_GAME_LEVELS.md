# Normal și Hard

[Enum-ul AiDifficulty](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/AiDifficulty.java) expune exact `NORMAL` și `HARD`. [Meniul](../whist-lab-app/app/src/main/java/ro/whistlab/app/MainActivity.java) construiește selectorul din `AiDifficulty.values()`, iar [textele interfeței](../whist-lab-app/app/src/main/java/ro/whistlab/app/WhistUiText.java) oferă etichetele în limba aleasă.

| Valoare salvată | Engleză | Română | Politică |
|---|---|---|---|
| `NORMAL` | Normal | Normal | Estimare simplă a licitației și alegerea unei cărți legale prin rang |
| `HARD` | Hard | Dificil | Motorul de decizie `MonteCarloAi` și gărzile sale publice |

Ambele niveluri respectă același [GameConfig](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/GameConfig.java) și aceleași [reguli](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/GameRules.java). Dificultatea schimbă decizia AI, nu obligația de culoare, atu, domeniul licitației sau scorul.

## Dispatch și informație disponibilă

[AiPolicy](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/AiPolicy.java) trimite `HARD` către `MonteCarloAi`. Supraîncărcările fără un argument de dificultate folosesc `NORMAL`.

Normal estimează puterea propriei mâini și alege dintre licitațiile permise. La joc, aplică întâi regulile și apoi preferă rangul mic dintre cărțile legale. Nu folosește certificatele de căutare ale nivelului Hard.

Hard observă mâna proprie, licitațiile, numărul de cărți și levate, scorurile publice, cărțile jucate, levata în curs și atuul întors.

Mâinile private reale ale adversarilor nu fac parte din `MonteCarloAi.View`. Distribuțiile ipotetice sunt construite din informația disponibilă, inclusiv lipsurile de culoare demonstrate public.

În profilul românesc, licitarea prioritizează șansa estimată de contract exact, apoi punctele.

Alegerea cărții prioritizează exactitatea, apoi punctele proprii și scorul rivalului dominant. Aceste priorități sunt implementate în motor; eticheta Hard nu înseamnă joc perfect sau o garanție de victorie.

## Estimări și certificate

[MonteCarloAi](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/MonteCarloAi.java) folosește estimări limitate și, unde se aplică, analiză pe un domeniu mic complet. [LateDiscard](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/LateDiscard.java), [ContractGuarantee](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/ContractGuarantee.java) și [PublicSalvage](../whist-lab-app/game-core/src/main/java/ro/whistlab/core/PublicSalvage.java) au condiții proprii, distincte de estimările statistice.

Certificatele curente de contract și salvare sunt limitate la finaluri românești cu patru jucători și două cărți proprii. Un rezultat parțial sau o căutare care depășește bugetul nu devine certitudine. Garda de salvare cere imposibilitatea contractului și dominanță fără deteriorarea punctelor proprii; nu sacrifică un contract realizabil pentru a produce ratări adverse.

La calculul levatelor inevitabile, un atu superior încă pe masă rămâne activ.

O carte întoarsă sau dintr-o levată deja încheiată nu poate consuma încă o carte majoră păstrată. Se verifică legalitatea răspunsului înainte de a aplica această distincție.

Prototipurile din afara surselor de producție și candidații aplicați prin overlay nu devin automat comportamentul Hard.

Testele de componentă, comparația pe partide noi și integrarea în sursele curente sunt calificări separate.

## Salvări și compatibilitate

`AiDifficulty.fromStoredName` convertește numele vechi `EXPERT` în `HARD`. Aliasul Java deprecated `AiDifficulty.EXPERT` este aceeași instanță ca `HARD`; nu apare în `values()` și nu creează o a treia opțiune de meniu. Numele necunoscute sunt respinse.

[GameSessionStore](../whist-lab-app/app/src/main/java/ro/whistlab/app/GameSessionStore.java) citește dificultatea prin acest parser și validează separat checkpoint-ul și istoricul de scor. Conversia numelui nu inventează date lipsă și nu schimbă regulile sau setările seriilor. O salvare invalidă nu este calificată prin înlocuirea silențioasă a dificultății.

## Verificare

[DifficultyContractTest](../whist-lab-app/game-core/src/test/java/ro/whistlab/core/DifficultyContractTest.java) verifică selectorul, aliasul, migrarea și dispatch-ul. Exemplele cu un posterior declarat sau un certificat finit verifică numai domeniul și ipotezele respective; nu sunt etichete universale pentru toate mâinile ascunse.

```powershell
.\scripts\run-whist-collection.ps1 -Collection quick
.\scripts\run-whist-collection.ps1 -Collection regression
```

Aceste execuții verifică JVM-ul fără emulator. Pentru instalare și acceptanță pe dispozitiv, folosește separat [ANDROID_TESTING](ANDROID_TESTING.md).
