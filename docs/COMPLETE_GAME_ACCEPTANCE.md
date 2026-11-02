# Acceptanță completă — Contract Whist și Whist Lab

Rezultat la 2 noiembrie 2026: **PASS funcțional**, cu observație vizuală
**WARNING** pentru randarea originalului pe emulatorul API 28.

## Rapoarte și dovezi

- Partida originală, replay-ul Android și integrarea normală Romanian Expert:
  `C:\work\whist_back\deep-app-test\runs\20261101-231857-full-match\final-extent-report.html`.
- Reguli românești, componente, model independent și AI:
  `C:\work\whist_back\rules-components\20261102-card-acceptance\component-extent-report.html`.
- Contractul normativ și matricea RW01–RW07:
  [game-core README](../whist-lab-app/game-core/README.md).
- Distincția obligatorie Oh Hell / Whist românesc:
  [reconstrucția regulilor](CONTRACT_WHIST_RULE_RECONSTRUCTION.md).

Raportul Android nativ Extent J12 are **107.332.432 bytes**, 6.655 aserțiuni
de reguli UI fără eșec, 581 înregistrări ACTION/OBSERVE și 325 checkpoint-uri
cu capturi. HTML-ul conține 307 URI-uri PNG Base64, cu 305 hash-uri distincte;
inventarul checkpoint-urilor și inventarul payload-urilor sunt populații
distincte. Verificatorul a comparat bytes PNG decodați cu fiecare captură sursă
și valorile complete expected/actual cu HTML-ul publicat. Dovada este
`accepted-report-verification.properties`, `status=PASS`, legată de SHA-256:

```text
2458bd209cb7b1e46aebfd18a58351efffdb9628f7ad7e746c4892ad388b16fe
```

Preview-ul rămâne 240 caractere. Corpurile TEXT native sunt nelimitate explicit
și păstrează integral datele lungi ale acțiunilor și aserțiunilor; J4 nu are
renderer HTML propriu. J0 păstrează implementarea Reporting. Feedback-ul său
conține separat problema de memorie la volum și pierderea datelor după preview.

## Ce a trecut efectiv

Inventarul acceptat din `components/` are **219 cazuri JUnit/Gradle** în 25 de
fișiere XML, fără FAIL/ERROR/SKIP. Publisherii au fost executați separat;
6.655 aserțiuni, 436 mutări și 219 cazuri nu se adună ca teste distincte.

| Probă | Rezultat observat |
|---|---|
| Original v4.5/145, Oh Hell, 4 locuri, Normal | 19 mâini 10→1→10, 109 levate, 436 cărți; Game Over confirmat în preferință și PNG |
| Audit original independent | posesie, actor, culoare, atu, câștigător, conducerea următoare, oferte și scor verificate la fiecare tranziție |
| Replay pur Java | aceleași distribuții, atuuri, oferte și 436 intrări prin motorul obișnuit |
| Replay Whist Lab Android | aceleași intrări, legalitatea butoanelor, cartea efectiv acceptată, starea și rândurile de scor UI |
| Totaluri finale în ambele aplicații | `[89,126,145,119]`; suma 479 = 109 puncte din levate + 370 din 37 contracte exacte |
| Joc normal Romanian Whist + Expert pe Android | ieșire din replay, distribuție normală, o mână completă, câștigător și scor independente, dealer și totaluri păstrate la mâna 2 |
| Componente reguli și AI | 187 cazuri PASS; 88 partide complete cu model independent, 40.304 mutări, 8.712 levate |
| Domenii exhaustive finite | 6.375 configurații de carte legală, 4.960 comparații de câștigător și toate cele 81 perechi ofertă/levate 0…8 |
| JaCoCo GameRules | 77/77 linii, 94/94 ramuri: 100% |
| JaCoCo întreg game-core | 595/612 linii: 97,22%; 469/515 ramuri: 91,07% |
| Expert față de Hard | 144 comparații pereche; 14.604 vs 7.689 puncte, 2.792 vs 1.777 contracte exacte; prag trecut la fiecare masă de 3–6 locuri |

Originalul a permis 20 de decartări cu atu disponibil. Profilul românesc
implementat cere atu când lipsește culoarea; nu cere supratăiere. Nu combinăm
regulile acestor două jocuri. Expert vede numai mâna proprie și informații
publice; testele permută cărțile ascunse și cer aceeași decizie.

## Proveniență și reluare

Originalul instalat a rămas neschimbat, SHA-256:
`884c492e4410d197199d23051d5405dd478620122ee70124426abd4a2e3d8d05`.
Nu s-a recreat emulatorul, nu s-au șters datele și nu s-a modificat contul Google.

Replay-ul complet Android Whist Lab a rulat pe APK
`d2e660c671541ad6e2e689e05fe316af068deb91075ddfd70b8346d6581da441`,
păstrat în `diagnostics/lab-replay-validated.apk`. Ulterior, `New game` și
`Play again` au primit resetarea `recordedReplay`. APK-ul instalat atunci avea
SHA-256 `7047266162fcd170ac7a470b29be5ca5eab29f5a3d8940b8172a3e116bf3dc23`;
acesta a trecut atunci testul mobil normal Romanian Expert. Replay-ul complet nu
este declarat reexecutat pe acest al doilea APK. Detaliile sunt în
`lab-apk-provenance.json` și în raport.

Au existat pauze și întreruperi ale harness-ului. Continuarea originalului și
a Whist Lab a revalidat dovezile reținute înaintea noilor acțiuni. În Lab,
primele 9 mâini sunt capturi live reținute, urmate de continuarea aceleiași
instanțe la mâna 10, fără injectarea rezultatelor. Verificarea refuză o stare
vie nepotrivită. Logurile eșuate și diagnosticarea sunt păstrate, nu numărate
printre cazurile acceptate. Timpii includ pauze și nu sunt un benchmark.

Jocurile și publisherul final au folosit Core C2 și Reporting J12 fizice.
Adopția ulterioară Core C3 are [proba sa separată](ATF_Core_C3_Verification.md);
nu schimbă retrospectiv classpath-ul rulărilor. `publisher-verification.xml`
și `library-binaries-j12.json` păstrează proveniența executată.
Suplimentar, J4 a compilat reactorul necesar într-un staging TMP nou și a
trecut 6 contracte pe C3/J12: Core, Extent, PNG inline și controalele de valori
complete. XML-urile, classpath-ul și hash-urile sunt în `c3-consumer/`; acestea
nu se adună retrospectiv la cele 219 de cazuri ale raportului.

## Reproducere

Componentele se execută primele, fără emulator:

```powershell
C:\work\1\atf_j4\scripts\run-rule-components.ps1
```

Cu emulatorul pornit și pachetul original instalat, o acceptanță nouă:

```powershell
$env:ADB_SERVER_SOCKET = 'tcp:192.168.56.1:5038'
C:\tools\whist-android-sdk\platform-tools\adb.exe connect 127.0.0.1:5555
C:\work\1\atf_j4\scripts\run-complete-match.ps1 -Serial '127.0.0.1:5555'
```

Scrcpy poate rămâne închis. Scriptul construiește și instalează Whist Lab;
pornește jocuri noi și lasă aplicația într-un joc normal Romanian Expert.
La încheierea acelei acceptanțe, ecranul era la licitarea mâinii 2.
Ulterior a trecut [partida normală completă Romanian Expert](ROMANIAN_EXPERT_ACCEPTANCE.md),
cu un APK nou consemnat separat; starea curentă este scorul final al mâinii 24.

Republicarea dovezilor existente nu cere Android și nu justifică reluarea
partidelor. Din J4, cu binarele configurate instalate în repository-ul Maven:

```powershell
$run = 'C:\work\whist_back\deep-app-test\runs\20261101-231857-full-match'
mvn -q -pl atf-tests test -Dtest=FullMatchExtentReportTest `
  -Datf.match.publish=true -Datf.test.heap.max=3g `
  "-Datf.match.evidenceDir=$run\evidence" `
  "-Datf.match.reportPath=$run\republished-extent-report.html"
```

O republicare folosește binarele curente și produce o dovadă nouă; raportul
acceptat și hash-ul de mai sus rămân referința datată. Publisherul actualizează
metadatele mutabile `full-match-report-verification.properties`; copia
`accepted-report-verification.properties` păstrează acceptanța acestui raport.
În configurația măsurată publicarea a trecut cu heap 3 GB; 1 GB a produs OOM în inlining-ul
fonturilor J12. Cauza și logul sunt păstrate în diagnostic și transmise J0.

## Limitele verdictului

Nu s-au enumerat toate partidele posibile. Benchmark-ul AI este un eșantion
repetabil, nu o dovadă de optimalitate sau performanță împotriva oamenilor.
În această primă rulare Android Romanian Expert a acoperit o mână și tranziția.
[Acceptanța ulterioară](ROMANIAN_EXPERT_ACCEPTANCE.md) a trecut partida normală
completă, 24 de mâini pe Android, cu APK și raport separate. Multiplayer, toate variantele
originalului și alte dispozitive/API-uri rămân necertificate.

Capturile originale arată în continuare text suprapus și cărți tăiate; verdictul
vizual rămâne WARNING. PNG-urile cheie au fost inspectate direct. Auditul
vizual/interactiv al HTML-ului nu a fost executat: MCP a refuzat schema locală
`file:`. Nu s-a ocolit restricția. Verificarea HTML automată dovedește păstrarea
datelor și imaginilor, nu lizibilitatea fiecărui rând în DOM-ul activ.
