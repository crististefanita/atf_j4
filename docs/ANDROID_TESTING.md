# Testarea Whist Lab pe Android

J4 deține testarea offline a motorului Whist Lab și acceptanța interfeței proprii.
Testele offline nu depind de emulator; testele UI pornesc explicit aplicația
ro.whistlab.app și acționează controalele vizibile.

## Suprafețe testate

| Suprafață | Sursă | Ce verifică |
|---|---|---|
| Motor și AI | whist-lab-app/game-core și ai-lab | Reguli, licitații, levate, scor, persistență și evaluări repetabile. |
| Adaptor Android | atf-android | Comenzi ADB delimitate, capturi și contracte pentru dispozitiv. |
| UI și Extent | atf-test-android | Meniu, masă, licitare, cărți, scor, acțiuni, aserțiuni și capturi ale aplicației proprii. |

## Verificări offline

Din rădăcina J4: run-whist-collection.ps1 -Collection context și
run-whist-collection.ps1 -Collection regression.

Colecțiile JVM și limitele lor sunt descrise în
[TEST_COLLECTIONS.md](TEST_COLLECTIONS.md). Validarea AI și build-ul APK au
runner separat în scripts/run-whist-ai-validation.ps1. Runner-ele pregătesc
sursele și rezultatele în directoare temporare sau externe, conform ghidului
proiectului.

## E2E-ul Whist Lab

[run-whist-responsive-sanity.ps1](../scripts/run-whist-responsive-sanity.ps1)
pornește din meniul normal și verifică layout-ul pe profile de ecran.
[run-romanian-expert.ps1](../scripts/run-romanian-expert.ps1) joacă mâini
normale în jocul nostru. [run-complete-match.ps1](../scripts/run-complete-match.ps1)
reia în Whist Lab o partidă înregistrată și verifică regulile, mutările și
scorul. Acest replay folosește fixture-uri J5 numai ca intrări; interacțiunile
live cu aplicația-model aparțin J5.

E2E-urile de produs selectează semantic elementele afișate, verifică după
acțiune tranziția observată și păstrează captura aceleiași stări. Un tap ADB
reușit singur nu certifică acceptarea mutării. Rezultatul testului, acceptanța
vizuală și obiectivul partidei rămân verdicte distincte.

## Dispozitiv și rezultate

Pentru un test live, identifică serialul, verifică disponibilitatea exclusivă a
emulatorului și folosește runnerul dedicat. Niciun test dezactivat sau sărit nu
este acceptanță UI. Rapoartele și capturile se păstrează în directorul extern
configurat; sursele J4 nu sunt destinație pentru artefacte.

Dacă o comparație generică din Extent sau din fallback nu respectă contractul
public al bibliotecii Reporting, înregistrează problema în
[J0 Reporting feedback](../../atf_j0/docs/feedback/README.md). J4 păstrează
așteptările specifice jocului și dovada consumerului.