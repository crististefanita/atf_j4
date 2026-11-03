# Mese virtuale A/B și acceptanță pe rol

Stare: 3 noiembrie 2026. Această iterație rulează exclusiv pe JVM și JSON, fără Android sau interfață.

## Strategiile aflate la aceeași masă

`config/strategy-arena.json` declară două ligi, fiecare cu patru strategii distincte. Fiecare strategie primește propria mână și informația publică: licitații deja depuse, cărți jucate, levate, scor și serii. Cărțile adversarilor din jurnalul final nu devin input de decizie.

| Profil | Obiectiv | Continuare cu acoperiri | Buget licitare / carte |
|---|---|---|---|
| point_greedy | puncte | fără ghid | 24 / 48 |
| point_covers | puncte | ghid V8 | 24 / 48 |
| safe_greedy | contract exact | fără ghid | 24 / 48 |
| safe_covers | contract exact | ghid experimental | 24 / 48 |
| expert_points | puncte, Expert V6 | continuare înghețată | 32 / 32 |
| expert_safety | contract exact, Expert Safety10 | continuare înghețată | adaptiv 32–128 / 32 |

Liga `factorial` folosește primele patru profiluri: obiectivul și ghidul sunt factorii 2×2, cu bugete egale. Liga `mixed` compară Expert V6, Expert Safety10, independent V8 și independent Safety10; aici diferă mai mulți factori simultan.

Pentru fiecare seed folosim aceleași împărțiri și opt aranjamente ciclice/inverse. Fiecare profil ocupă fiecare loc de două ori per seed. Sunt **128 de partide complete**, cu 24 de mâini și 94 de levate fiecare: două ligi × opt seed-uri × opt aranjamente. Cele opt seed-uri sunt unitățile independente de distribuție; rotațiile nu reprezintă 128 de împărțiri independente. Nu enumerăm toate cele 24 de permutări.

Protocolul unei execuții este copiat și înghețat înainte de joc. Protocolul istoric `arena-safety10-arena` a folosit independentul experimental ca `PLAYER_CANDIDATE`. Configurația curentă îl leagă explicit de `SAFETY_COVERS`, exclusiv în test, deoarece independentul de producție a revenit la V8. Două teste de reluare compară toate acțiunile și scorurile cu partidele originale; al treilea verifică fidelitatea clonei independente V8.

## Rezultatul măsurat al primei ligi

Fiecare profil din tabel are 64 de partide și 1.024 de contracte de 2–8 cărți.

| Profil factorial | Puncte medii | Pondere victorii | Contracte exacte 2–8 |
|---|---:|---:|---:|
| point_covers | 90,719 | 32,813% | 64,746% |
| point_greedy | 85,438 | 28,906% | 62,793% |
| safe_covers | 83,297 | 15,625% | 61,230% |
| safe_greedy | 81,469 | 22,656% | 60,449% |

Contrastul descriptiv al ghidului este +1,367 puncte procentuale pentru contracte exacte; cel al obiectivului experimental de siguranță este −2,930 puncte procentuale. Interacțiunea este −1,172 puncte procentuale. Aceste contraste includ reacțiile adversarilor; nu demonstrează un avantaj cauzal izolat ori o superioritate pe alte distribuții.

În liga mixtă, Expert Safety10 obține 88,594 puncte medii, 32,813% pondere de victorii și 65,137% contracte exacte de 2–8 cărți. Expert V6 are 84,531 / 25,781% / 62,207%. Aceasta este observație de dezvoltare. Promovarea depinde de evaluarea separată descrisă mai jos.

## Ce păstrăm în aplicație

| Rol | Politică acceptată | Evaluare separată | Rezultat față de V6 |
|---|---|---|---|
| Expert | Safety10 | 16 seed-uri noi × două familii de adversari × patru locuri = 128 perechi | +271 puncte, +4,5 ponderi de victorie, +40 contracte exacte 2–8 |
| Independent | V8 | 12 seed-uri noi la înghețarea V8 × două familii × patru locuri = 96 perechi | +130 puncte, +1 pondere de victorie, +2 contracte exacte 2–8 cumulat |

Expertul a trecut criteriul **SAFETY**, declarat înainte de dezvoltare/evaluare: fără pierdere de contracte exacte 2–8 sau pondere de victorii în fiecare familie, scădere medie de scor de cel mult două puncte, plus progres strict de contracte exacte cumulat. Punctele sunt criteriu secundar de alegere, după probabilitatea contractului exact.

Independentul V8 a trecut criteriul **POINTS** al experimentului său original. Într-o familie pierde trei contracte exacte de 2–8 cărți; nu îl declarăm retrospectiv acceptat prin SAFETY. Varianta independentă Safety10 pierde 192 de puncte și 8,5 ponderi de victorie față de V8, deși are +4 contracte exacte cumulat. Este respinsă și păstrată numai pentru cercetare.

Loturile 103000–103011 și 104000–104015 sunt consumate. Nu se folosesc la reglarea politicilor și nu se prezintă ulterior ca evaluări noi. O victorie împărțită între doi jucători contribuie cu 0,5 la ponderea fiecăruia; aceste valori nu sunt puncte procentuale.

## Din partidă în test mic

1. Salvăm partida completă și reluăm independent legalitatea, câștigătorii și scorurile.
2. Selectăm per profil/ligă câte o eroare de sublicitare, supralicitare și un exemplu pozitiv. Primul casebook are 24 de exemple, cu observații publice și rezultat separat.
3. Eticheta `NEEDS_INDEPENDENT_EXPECTATION` interzice transformarea automată a mutării observate în răspuns optim. Reducem situația la un contraexemplu cu regulă, invariant justificat sau oracol exact pentru final mic.
4. Testul mic intră în colecția rapidă/regresie; turneul rămâne opt-in. Un model nou este înghețat și verificat pe un lot neconsumat înainte de promovare.

Componentele noi verifică obiectivul de siguranță, legalitatea licitației interzise, păstrarea valorilor de puncte din diagnostic, informația publică, evitarea contractelor imposibile, folosirea licitațiilor adversarilor și limitele garanțiilor pentru onoruri. Regresia curentă are **500 de cazuri**; cele **136 rapide** sunt un subset. Cele trei reluări de profil sunt controale suplimentare, opt-in, nu probe de joc optim.

## Execuție și artefacte

```powershell
.\scripts\run-whist-collection.ps1 -Collection quick
.\scripts\run-whist-collection.ps1 -Collection regression
.\scripts\run-whist-collection.ps1 -Collection arena `
  -ProtocolFile .\config\strategy-arena.json `
  -CorpusRoot C:\work\whist_back\training-corpus
```

Runnerul cere output nou, copiază sursele în TMP propriu și păstrează XML/JSON/loguri în `C:\work\whist_back`. Nicio dependență Android nu este configurată de build-ul `algorithm-build`.

Execuția acestei iterații: `C:\work\whist_back\ai-training\20261103-safety10`.

- `protocol`: criterii, freeze, decizii pe rol și inventarul surselor promovate;
- `evaluation`: populațiile finale complete și varianta independentă respinsă;
- `arena`: protocolul original, partide/capturi, clasamente, audit și casebook;
- `components`: rezultatele native și istoricul tentativelor nereușite;
- `teaching`: calibrare de dezvoltare, selecție activă și audit de corpus;
- `reports`: inputul selectat, inventarul complet și Extent pe JAR-ul fizic curent J0.

`config/learning-corpus.json` trimite la manifestul activ verificat prin SHA-256. Arhiva completă nu se citește implicit și nu se șterge. Se păstrează și exemple pierdute; reducerea zgomotului se face prin selecție, nu prin ascunderea rezultatelor.

## Report input contract / English

J4 owns the evidence supplied to Reporting; J0 owns its binary and renderer. `scripts/whist-report-input.mjs` validates source hashes, native counts, complete game replays and selected invocation identities before preparing a bounded collection. Original receipts are copied byte for byte. Full source inventories distinguish detailed HTML examples from omitted raw captures. An invalid or non-PASS member of an accepted cohort aborts collection; rejected models are explicitly identified as rejected research, never recast as accepted game results.

The current publication includes all small safety regressions, boundary examples for the measured 2–8 matrices, the smallest seed/seat per accepted opponent family, and one full game per heterogeneous league. The three large snapshot controls remain counted and hashed instead of embedding duplicate full traces. This reduces the Java publisher's supplied population; it does not claim that its generic loader streams an arbitrary large manifest.

The new report uses **Reporting J17**, the current J0 distribution. It includes no fresh device screenshots: this iteration is offline. Static evidence audit and interactive rendering acceptance are separate outcomes. Historical J16 reports retain their own identities.

See the [Romanian honor/cover notes](rules/AI_HAND_PATTERNS_RO.md), [English notes](rules/AI_HAND_PATTERNS_EN.md), [collection catalog](TEST_COLLECTIONS.md) and [constructive feedback](feedback/AI_SAFETY_ARENA_Feedback.md).

Sursele politicilor au fost promovate și verificate. Această iterație nu construiește sau instalează un APK nou; aplicația Android aflată deja în execuție poate folosi o versiune anterioară până la următorul build controlat.

Raport final: `reports/final-extent-report.html`, 56 capturi, 2.879 comparații, 6.613.666 bytes. Auditul static a trecut pe JAR-ul J17 `b7df32027a91f29b3662d0de179bc329ab3018006e4894fad594401d6fda9f0a`; acceptanța interactivă a acestui HTML nu este revendicată. Colectarea finală a reluat independent 578 de jocuri din cohortele sursă. Verificarea întregii arhive numără 7.477 de jocuri și 2.811.352 de cărți jucate, pe 213 seturi distincte de împărțiri; aceasta nu este o măsurare a superiorității modelului.
