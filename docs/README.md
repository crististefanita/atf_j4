# Documentația Whist Lab

## Pe scurt

Acest index separă ghidurile curente, contractele de testare și dovezile unei
execuții închise. Regulile descrise trebuie să corespundă motorului și profilului
ales, iar orice afirmație de acceptanță trebuie legată de versiunea verificată.

## Joc, motor și aplicație

| Întrebare | Ghid |
|---|---|
| Cum este organizat proiectul? | [Prezentarea proiectului](../README.md) |
| Cum se construiește aplicația și cum se păstrează partida? | [Aplicația Android](../whist-lab-app/README.md) |
| Unde sunt regulile și tranzițiile jocului? | [Motorul Java](../whist-lab-app/game-core/README.md) |
| Cum diferă profilurile original și românesc? | [Contractele de reguli](CONTRACT_WHIST_RULE_RECONSTRUCTION.md) |
| Cum se joacă? | [Română](rules/WHIST_RO.md) · [English](rules/WHIST_EN.md) |
| Ce se păstrează în tabela de scor? | [Istoricul scorului](WHIST_SCORE_HISTORY.md) |
| Ce înseamnă cele două niveluri? | [Normal și Hard](TWO_GAME_LEVELS.md) |

## Testare și învățare offline

| Activitate | Ghid |
|---|---|
| Alegerea unei colecții mici sau a regresiei complete | [Colecțiile de teste](TEST_COLLECTIONS.md) |
| Build fără SDK Android | [Build-ul algoritmului](../whist-lab-app/algorithm-build/README.md) |
| Formatul partidelor și validarea independentă | [Corpusul](AI_CORPUS.md) |
| Selecția diagnostică a partidelor utile | [Focusul de învățare](AI_LEARNING_FOCUS.md) |
| Separarea dezvoltării de evaluarea nouă | [Protocolul AI](AI_TRAINING_PROTOCOL.md) |
| Comparații între strategii și locuri la masă | [Arena](AI_STRATEGY_ARENA.md) |
| Onoruri, acoperiri și reguli contextuale | [Română](rules/AI_HAND_PATTERNS_RO.md) · [English](rules/AI_HAND_PATTERNS_EN.md) |
| Decizia de aruncare a unei cărți mari | [Context și limite](AI_CONTEXTUAL_DISCARD.md) |

## Android și dovezi

| Activitate | Ghid |
|---|---|
| Pregătirea dispozitivului și alegerea runnerului | [Testarea Android](ANDROID_TESTING.md) |
| Meniu, masă și scor pe mai multe ecrane | [Verificarea pe profiluri de ecran](WHIST_RESPONSIVE_UI_SANITY.md) |
| Ce s-a verificat pe versiunea 0.1.5? | [Acceptanța interfeței](WHIST_UI_015_ACCEPTANCE.md) |
| Testele și oracolele aplicațiilor | [Modulul Android](../atf-test-android/README.md) |
| Pregătirea unei partide în aplicația originală | [Runnerul originalului](ORIGINAL_ROMANIAN_HARD_LIVE.md) |
| Cerințe pentru un raport inteligibil separat de surse | [Contractul dovezilor](ATF_Android_Report_Contract.md) |
| Ultimul raport acceptat și ultima încercare, pe scop | [README-ul raportării](reporting/README.md) |
| Publicarea și reluarea offline a unui trace UI | [Handoff UI](WHIST_UI_REPORT_HANDOFF.md) |
| Probleme, responsabilități și criterii de închidere | [Feedback](feedback/README.md) |
| Rezultate ale unor versiuni anterioare | [Dovezile istorice](history/README.md) |

## Întreținerea documentației

- Ghidurile curente descriu API-urile, comenzile și limitele implementate.
- Rezultatele numerice, hash-urile și verdictul unei rulări apar în dovezile
  acelei rulări; un rezultat vechi nu certifică sursele modificate ulterior.
- O schimbare de reguli actualizează catalogul, exemplele de componentă și
  ghidurile generate în ambele limbi.
- O schimbare de persistență precizează migrarea, informația recuperabilă și
  tratarea datelor care lipsesc din salvări vechi.
- Guvernanța verifică structura, lizibilitatea, diagramele, linkurile și căile
  relocabile; ea completează verificarea factuală a documentelor față de cod.
