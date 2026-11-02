# Whist românesc cu Expert — acceptanță Android completă

Rularea din 2 noiembrie 2026:
`C:\work\whist_back\romanian-expert\20261102-131344`.
Partida Android, auditul offline și publicarea Extent sunt **PASS**.
Raport: `romanian-expert-extent-report.html`, **41.278.016 bytes**, cu
**11.047 aserțiuni fără eșec**, 405 înregistrări ACTION/OBSERVE și **287
checkpoint-uri cu capturi inline Base64**. HTML-ul conține 180 de URI-uri PNG
cu 178 de hash-uri distincte; checkpoint-urile repetate nu sunt payload-uri
unice sau cazuri JUnit suplimentare. Fiecare captură sursă și fiecare valoare
expected/actual completă au trecut verificarea automată.

SHA-256 HTML:
`342e0da369e5551c25ded7beb6abc961793bfcaec5fff77eccf58eef47bbd70b`.
`accepted-report-verification.properties` păstrează dovada acceptată.
Raportul componentelor este `component-acceptance/component-extent-report.html`.

## Rezultatul partidei

Toate cele **24 de mâini, 94 de levate și 376 de cărți** au trecut verificarea.
Scoruri finale în ordinea locurilor: **You 12, Ava 87, Miles 91, Grace 61**.
UI afișează **Game complete** și **Winner: Miles (91 points)**; nu există
**Next hand**. Același APK instalat a executat întreaga partidă normală.

Auditul offline a reconstruit **260 de snapshot-uri** și toate rândurile de
scor din dovezile salvate. A reconfirmat cele 376 de mutări, scorurile finale
și checkpoint-ul exact reluat după oprirea procesului, fără ADB sau game-core.

Cazuri observate: 94 conduceri de levată, 159 răspunsuri la culoare,
12 tăieri obligatorii și 111 decartări permise. Acestea însumează 376 de mutări.
128 dintre ele aparțin celor patru mâini fără atu. Cele 96 de contracte
individuale includ 31 exacte zero, 22 exacte pozitive, 18 oferte prea mari
și 25 oferte prea mici. Pentru fiecare, scorul UI a fost comparat cu formula
românească și cu totalul anterior.

Inventarul acceptat are **233 cazuri JUnit/Gradle**, fără FAIL/ERROR/SKIP:
210 motor/AI, 21 contracte Android/raportare, partida live și auditul offline.
Publisherii sunt probe separate. Durata de 2.024,693 s din JUnit este timpul
testului UI complet; nu este un benchmark de viteză al jocului sau AI-ului.

## Contractul partidei

Jocul este pornit din **New game**, cu patru locuri, **Whist romanesc (1-8-1)**
și **Expert (simulation)**. Nu se folosesc fixture, debug replay, seed fix,
distribuții, mutări sau rezultate injectate. Operatorul automat al locului uman
alege oferta din propriile cărți mari și domeniul permis de UI, apoi variază
cărțile legale din mâna proprie. Ceilalți trei jucători folosesc politica Expert
normală a aplicației.

Secvența este `1,1,1,1,2,3,4,5,6,7,8,8,8,8,7,6,5,4,3,2,1,1,1,1`:
**24 mâini, 94 levate, 376 cărți, 94 acțiuni umane de carte**.
La opt cărți, pachetul redus de 32 de cărți este împărțit complet: nu există
carte întoarsă sau atu. Cele patru mâini fără atu au exact 128 de mutări.

Profilul are atu obligatoriu când lipsește culoarea, fără supratăiere
obligatorie. Exact: `5 + levate`; inexact: `-abs(levate - ofertă)`.
[Contractul RW01–RW07](../whist-lab-app/game-core/README.md) păstrează sursele,
variantele excluse și diferențele față de Oh Hell.

## Oracol și capturi

`RomanianMatchOracle` din adapterul J4 folosește cărți întregi și un model
propriu. Nu apelează GameRules, GameSession sau AI pentru rezultatele așteptate.
Observă read-only checkpoint-ul aplicației noastre și reconstruiește fiecare
carte din istoricul public append-only: actor, posesie, culoare/atu, câștigător,
conducerea următoare, contoare și scor. La fiecare limită compară și mâinile
rămase, levata curentă/precedentă și totalurile Android.

Oferta finală nu poate egaliza suma cu mărimea mâinii. Testul verifică întregul
domeniu numeric expus de selector și toate ofertele din ordinea dealerului.
Pentru fiecare tură umană verifică exact cărțile activate/dezactivate, apoi
identitatea cărții acceptate. La scor compară formula și fiecare rând UI.
În timpul unei mâini oprește procesul, reia aceeași partidă și cere paritate
exactă a checkpoint-ului. Finalul verifică absența **Next hand** și câștigătorul,
inclusiv egalitățile.

PNG-urile sunt capturi reale la licitare, înainte/după mutările umane și la
scorul fiecărei mâini. O captură după acțiune poate acoperi mai multe cărți
automate Expert; raportul explică separat fiecare dintre ele din istoricul
observat. Nu pretindem o fotografie pentru fiecare cadru sau carte AI.

Extent folosește sesiunile native J0, SETUP / TEST BODY / TEARDOWN, preview 240
și corpuri TEXT integrale. Verificatorul compară fiecare PNG cu payload-ul
Base64 decodat și păstrează valorile complete expected/actual. Proprietățile
de publicare rămân `PENDING` până trec toate controalele și hash-ul HTML.

## Componente și îmbunătățiri

Înainte de Android au trecut **210 cazuri motor/AI și 21 de contracte
Android/raportare**, fără eșecuri/erori/omiteri. Publisherii rulează separat.
Cele 23 de cazuri noi din motor acoperă auditul istoricului și explicațiile
regulilor; noul oracol Android are nouă controale pozitive/negative.

- Salvările cu istoric complet reconstruiesc ordinea, cărțile legale și
  câștigătorii. Nu acceptă redistribuirea frauduloasă a levatelor cu suma păstrată,
  o renunțare la culoarea ținută sau o decartare în locul atuului obligatoriu.
- Formatul v2 cere tot istoricul. O salvare veche cu doar ultima levată rămâne
  explicit v1 la resalvare până începe o mână nouă; nu fabricăm cărțile lipsă.
- Mâinile românești de opt cărți nu pot fi restaurate cu atu inventat.
- Expert nu mai simulează distribuții când există o singură carte legală.
  Decizia precizează această cale și nu inventează o estimare de punctaj.
- UI explică profilul ales, culoarea sau atuul obligatoriu și supratăierea
  opțională; arată progresul partidei și câștigătorii, inclusiv totaluri negative
  și egalități. Întoarcerea la meniu anulează callback-urile AI încă programate.

Controlul mutant a eliminat numai apelul de audit al istoricului într-un TMP
separat: **7 din 12 teste refuză mutantul**. Sursa normală trece toate cele 12.
Mutantul nu a fost împachetat sau instalat pe Android. Logul este
`history-mutant-rejection.log`, iar receipta este `history-mutant-control.json`.
Aceste eșecuri așteptate nu se numără printre cazurile funcționale acceptate.

Modelul independent păstrează 88 de partide complete, 40.304 mutări și 8.712
levate la 3–6 locuri. Benchmark-ul identic are 144 de perechi: Expert 14.604,
Hard 7.689; contracte exacte 2.792 față de 1.777. Îmbunătățirea căii forțate
păstrează deciziile și aceste rezultate; nu reprezintă un nou nivel de forță.
Nu este o demonstrație de optimalitate sau performanță împotriva oamenilor.

JaCoCo: GameRules 77/77 linii și 94/94 ramuri. Întreg motorul are 652/671 linii
(97,17%) și 549/607 ramuri (90,44%). Gărzile noi adaugă ramuri; numărul de cazuri,
acoperirea codului și acoperirea regulilor sunt măsurători distincte.

## Reproducere și proveniență

```powershell
C:\work\1\atf_j4\scripts\run-romanian-expert.ps1
```

Runnerul creează un staging TMP nou, încheie componentele și raportul lor,
construiește și instalează propriul APK, apoi execută partida normală și Extent.
Nu cere scrcpy. Configurație curentă: API 28, ADB remote
`tcp:192.168.56.1:5038`, serial `127.0.0.1:5555`.
Datele și instalarea originalului Contract Whist nu se modifică.

APK-ul acestei rulări este arhivat ca `whist-lab-tested.apk`, SHA-256
`e7f8ef7870b29cf7f4686405156c8e31e8c559d4597cb9f6f1e98cd5eeae64eb`.
Hash-ul APK-ului instalat a fost comparat cu buildul. `apk-provenance.json`,
`library-binaries.json` și XML-urile fixează Core C3 / Reporting J12.
Sursele sunt construite în TMP; dovezile rămân în afara repository-ului.
`source-snapshot.zip` păstrează sursele consumerului, fișierele de build și
perechile JAR/POM fizice; cache-urile, output-urile și cheile de semnare sunt excluse.
Nu s-a schimbat APK-ul în timpul partidei sau după încheierea ei.

## Limite de interpretare

O partidă Android nu enumără toate distribuțiile sau dispozitivele. Cazurile
de reguli suplimentare se testează în componente, fără interfață. Timpii zero
din ACTION/OBSERVE sunt neinstrumentați, nu latențe măsurate.
Auditul vizual/interactiv al HTML-ului nu este certificat: MCP a refuzat schema
locală `file:` și restricția nu este ocolită. PNG-urile selectate sunt inspectate
direct; verificarea automată a datelor nu dovedește lizibilitatea întregului DOM.
