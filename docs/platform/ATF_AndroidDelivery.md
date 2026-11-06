# Contractul livrării Android

Livrarea leagă configurația surselor de identitatea APK-ului și de compatibilitatea instalării. Versiunea declarată în sursă, binarul produs și aplicația instalată au identități care trebuie verificate separat.

## Responsabilități

```mermaid
flowchart LR
    SOURCE[Surse identificate] -->|construiește în staging| BUILD[Gradle Android]
    BUILD -->|produce| APK[APK semnat]
    APK -->|verifică| STATIC[Pachet și integritate]
    STATIC -->|permite instalare| DEVICE[Aplicație instalată]
    STATIC -->|publică binarul verificat| ASSET[Asset GitHub Release]
    ASSET -->|descarcă și confruntă hash| DEVICE
    DEVICE -->|verifică separat| UI[Contracte Android]
```

<details>
<summary>Legendă: build și comportament</summary>

Build-ul verifică producerea binarului, iar controalele statice verifică pachetul, SDK-urile, semnătura și alinierea. Verificarea dispozitivului se referă la hash-ul APK-ului instalat efectiv.

</details>

## Condiții de livrare

- Citiți identitatea pachetului, SDK-urile și versiunea din [configurația Gradle a aplicației](../../whist-lab-app/app/build.gradle).
- Construiți într-o copie temporară proprie, cu dependențe identificate.
- Verificați API-urile Java disponibile pe platforma Android vizată; nivelul `sourceCompatibility` nu adaugă metode absente din sistem. Configurați explicit desugaring-ul bibliotecilor de bază dacă produsul depinde de el.
- Verificați integritatea, semnătura, alinierea și compatibilitatea cheii pentru upgrade.
- Păstrați APK-ul și checksum-ul în `release/<versionName>/`, lângă manifest și README; dovezile aparțin subdirectorului `reports`. [Catalogul livrărilor](../../release/README.md) identifică versiunea curentă, iar rulările intermediare rămân externe.
- Verificați binarul instalat înainte de a atribui unui scenariu comportamentul acelei livrări.
- Distingeți build-ul local de distribuția prin magazin și de gestionarea cheilor de distribuție.

## Compatibilitate și calificare

Compatibilitatea unei versiuni Android noi se califică separat de alegerea `targetSdk`. Numărul SDK-ului din APK nu dovedește instalarea, afișarea ori stabilitatea pe acel sistem.

| Nivel | Ce verifică | Limită |
|---|---|---|
| Construire și pachet | Surse, dependențe, manifest, SDK-uri, semnătură și APK complet. | Nu execută aplicația pe fiecare sistem acceptat. |
| Componente JVM | Modelele ferestrei, geometria, salvarea și regulile, cu intrări controlate. | Nu execută callback-urile sistemului sau gesturile reale. |
| Android identificat | Instalare, restaurare, fereastră, interacțiune și stabilitate pe binarul efectiv. | Califică profilul și sistemul declarate, cu limitele înregistrate. |

## Distribuția prin GitHub

- **Descărcare:** README-ul principal oferă APK-ul candidatului local; după publicare, folosește linkul direct fixat pe versiune și nume de asset din GitHub Releases. Autentificați-vă în același browser pentru canalul personal; directorul `release` al repository-ului păstrează documentația și copia locală.
- **Stare:** README-ul principal indică numai `currentVersion`. Pentru `PREPARED`, linkul identifică APK-ul local; după publicare, identifică asset-ul GitHub al aceleiași versiuni. O ediție anterioară poate fi reper în audit, fără a deveni descărcarea curentă.

- **Copie locală:** [catalogul livrărilor](../../release/README.md) trimite la dosarul versiunii, care reunește APK-ul, checksum-ul și rapoartele; binarul și checksum-ul local sunt excluse din Git.
- **Navigare:** GitHub afișează directorul repository-ului ca fișiere și documentație; README-ul oferă o legătură către descărcare, fără redirect HTTP automat.

- **Identitate:** [registrul release-ului](../../release/README.md) deține versiunea, adresele asset-urilor și probele de integritate.
- **Canale:** descărcarea personală autentificată și accesul anonim au calificări separate; lipsa accesului anonim nu blochează acceptarea canalului personal verificat.

| Etapă | Criteriu de acceptare |
|---|---|
| Publicare | Upload-ul asset-ului are exact numărul de bytes al APK-ului semnat; checksum-ul identifică același fișier. |
| Descărcare | Asset-ul este accesibil în canalul declarat, iar copia descărcată are mărimea și SHA-256 originale; pentru folosirea personală se verifică browserul autentificat. |
| Integritate | Arhiva ZIP trece CRC pentru toate intrările și conține manifestul Android și codul aplicației; semnătura și alinierea sunt verificate separat. |
| Instalare | Verificarea folosește copia completă, cu identitatea pachetului și cheia de upgrade a release-ului. |

Un răspuns HTTP de eroare nu constituie un APK, indiferent de extensia fișierului salvat. Reușita construirii și instalării locale nu dovedește reușita descărcării publice; integritatea și proveniența copiei descărcate se verifică separat.

- Copiați fișierul local într-un nume temporar, verificați-l și publicați-l prin redenumire atomică. Descărcați din nou asset-ul GitHub și confruntați-l înainte de handoff; răspunsurile HTTP negative rămân erori, fără promovarea lor la pachet instalabil.

- Calificați separat accesul autentificat și accesul anonim. `visibility=public`, starea `uploaded` și digest-ul corect al asset-ului nu dovedesc că browserul unui vizitator poate descărca binarul. Proba păstrează codul HTTP, numărul de bytes și hash-ul copiei primite; un blocaj de vizibilitate rămâne deschis până la răspunsul public reușit.

## Procesul unei versiuni

[Calificarea pas cu pas](../testing/ATF_ReleaseQualification.md) ordonează selecțiile, verificarea HTML și dosarul candidat; acest ghid deține build-ul, identitatea și publicarea.

[Modelul GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases) leagă livrarea de un tag și permite atașarea binarelor. APK-ul se distribuie ca asset; dosarul local `release` păstrează livrarea verificată, fără să creeze o secțiune GitHub sau un redirect HTTP.

1. Identificați commit-ul surselor și versiunea aplicației; fixați copia de construire în TMP.
2. Construiți și semnați o singură dată, apoi verificați pachetul și scenariile relevante ale acelui binar.
3. Pregătiți un draft legat de tag, cu APK și checksum; confruntați identitatea asset-ului cu registrul livrării.
4. Publicați versiunea completă și marcați-o drept ultima versiune; păstrați binarul publicat stabil, iar corecțiile primesc o versiune nouă.
5. Verificați descărcarea în canalul cerut și actualizați registrul, legăturile și dosarul de dovezi.

[GitHub recomandă draftul înaintea atașării asset-urilor și publicării](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository) pentru release-urile imuabile. Procesul proiectului folosește această ordine și păstrează diferența dintre politica stabilită și activarea opțiunii native de imutabilitate.

## Release de mentenanță

Un patch apropiat de versiunea precedentă folosește verificări țintite după diferența reală de cod. Numărul versiunii nu justifică singur reducerea verificărilor; schimbarea regulilor, salvărilor, SDK-urilor ori dependențelor poate cere calificare suplimentară. Guvernanța folosește snapshot-ul de release și metadatele candidatului, păstrând lucrările paralele separate.

| Control | Minim pentru patch | Când se extinde |
|---|---|---|
| Surse și build | Commit identificat, copie în TMP, `assembleRelease` și `lintVitalRelease`. | Dependențe, platformă sau configurație de construire schimbate. |
| Comportament | Componentele/regresiile zonei modificate și cazurile negative relevante. | Reguli, scor, salvări sau politică AI schimbate; selecția urmărește contractul afectat. |
| Guvernanță | Dosare și prezentare release, plus linkuri și stil pentru documentele atinse. | Arhitectură ori convenții schimbate; se adaugă controalele ariei respective. |
| APK | Identitate, SDK-uri, CRC, semnătură, aliniere, compatibilitatea cheii de upgrade și SHA-256. | Manifest, instalare ori compatibilitate Android schimbate; se adaugă smoke nativ. |
| Interfață | Componentele geometriei; sanity scurt pentru interacțiunea schimbată, când dispozitivul este disponibil. | Un profil ori o tranziție nouă cere acceptanță distinctă; nu se repetă automat partida completă pe fiecare rezoluție. |
| Distribuție | Asset și checksum, apoi descărcare în canalul cerut, cu mărime și SHA-256 confruntate. | Schimbarea canalului ori a semnării cere reverificarea distribuției. |

Selecțiile executate trebuie să treacă; un eșec relevant blochează publicarea. O verificare nativă omisă se consemnează în dosarul tehnic împreună cu motivul, fără a prelua calificarea APK-ului anterior și fără a încărca pagina Release.

### Predarea patch-ului

1. Actualizați `versionName` și incrementați `versionCode` în Gradle; fixați commit-ul construit. Sursele relevante schimbate în paralel se confruntă înainte de publicare.
2. Rulați selecția de mai sus în TMP; pregătiți comenzile, rezultatele JUnit și jurnalele pentru `release/<versionName>/reports/`, cu un README scurt al verificării. Rapoartele Extent provin numai din execuțiile efective ale versiunii; un reper anterior este etichetat explicit.
3. Puneți APK-ul semnat `WhistLab-<versionName>.apk` și checksum-ul în `release/<versionName>/`; păstrați numai ultima pereche locală, după validarea înlocuitorului. Binarul se distribuie ca asset GitHub Release, nu ca fișier Git.
4. Actualizați manifestul și README-ul noii versiuni, `release/index.json`, catalogul și notele în bullets față de versiunea precedentă. Ghidurile generice se modifică numai dacă s-a schimbat procesul ori contractul.
5. Confruntați guvernanța înainte de publicare, publicați tag-ul și asset-urile verificate, apoi descărcați asset-ul complet în canalul declarat. Actualizați dovada descărcării și linkul direct din README numai după confruntarea versiunii curente, mărimii, SHA-256 și CRC; portalul `releases/latest` nu fixează identitatea APK-ului.

Metadatele draftului se păstrează separat în `github-release-draft.json`; `github-release.json` descrie publicarea efectivă. GitHub poate folosi adrese temporare `untagged` în draft, astfel că testul notelor legate de asset-urile publicate se execută din nou după publicare, pe adresele canonice observate. La publicare, trimiteți explicit tag-ul și commit-ul surselor și confruntați răspunsul cu identitatea APK-ului.

## Dosarul unei versiuni

[Catalogul livrărilor](../../release/README.md) păstrează numai ultima versiune calificată, în `release/<versionName>/`. Prezentarea versiunii și metadatele ei trimit la dovezile din `reports/`. [Politica de retenție](ATF_ReleaseRetention.md) definește arhivarea precedentului, înlocuirea și resetarea GitHub cerută explicit.

| Intrare | Responsabilitate |
|---|---|
| `release/index.json` | Schema 2 declară `currentVersion` și exact o versiune activă în `versions`; toate legăturile principale identifică acea versiune. |
| `release/README.md` | Catalogul stabil al livrării active și punctul de intrare în dosarul ei. |
| `release/<versionName>/README.md` | Prezintă ediția, APK-ul identificat, starea predării și legăturile către metadate și rapoarte. |
| `release/<versionName>/reports/README.md` | Prezintă selecțiile executate, rapoartele, rezultatele și limitele calificării. |
| Manifest și metadate în dosar | Leagă `versionName`, tag-ul, asset-urile, checksum-ul și notele; căile relative pornesc din dosarul versiunii. |
| Rapoarte, capturi, rezultate și jurnale în `reports/` | Păstrează execuția identificată, inclusiv diagnosticele negative și replay-urile etichetate; legăturile lor locale pornesc din acel subdirector. |
| [Strategia testării](../testing/README.md) | Deține selecția și principiile comune, independent de versiune. |

- La o versiune nouă, calificați candidatul extern și arhivați dosarul anterior înaintea înlocuirii. Actualizați indexul și catalogul după verificarea identității; păstrați numai APK-ul local curent în dosarul versiunii din `release`.
- La reorganizare, mutați împreună rapoartele și fișierele asociate, confruntați hash-urile și refaceți referințele active. Căile absolute capturate și URL-urile istorice fixate pe commit își păstrează sensul original.
- `artifacts/` și `dist/` sunt directoare retrase din rădăcina proiectului. După arhivarea și confruntarea datelor unice, eliminați directoarele vechi, inclusiv cele goale; guvernanța refuză reapariția lor.
- Notele din GitHub Release trimit la README-ul versiunii într-un commit identificat; schimbarea navigării nu mută tag-ul și nu înlocuiește asset-ul binar.

[Guvernanța dosarelor](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactLayoutGovernanceTest.java) respinge populația neînregistrată, acumularea versiunilor active, metadatele lipsă și identitățile divergente. Verificarea integrității fișierelor și a linkurilor completează acest contract structural.

### Linkul principal și proba descărcării

| Situație | Prezentare și verificare |
|---|---|
| Candidat local | Link la dosarul exact din `release/<versionName>/`, stare `PREPARED` și APK local; fără afirmație de publicare GitHub. |
| Asset publicat | Link direct `releases/download/<tag>/<apkName>`; eticheta vizibilă, tag-ul și fișierul identifică aceeași versiune. |
| Copie completă | Răspuns reușit, mărime și SHA-256 identice cu asset-ul și binarul declarat; ZIP cu CRC valid, manifest Android și cod. Un răspuns de eroare nu se redenumește `.apk`. |
| Canal verificat | API autentificat și browser sunt probe distincte. Reușita API nu marchează browserul drept verificat și nu închide blocajul accesului anonim. |
| Identitate unică | `currentVersion`, manifestul, eticheta, numele APK-ului și URL-ul identifică aceeași ediție. Dovezile unei versiuni anterioare rămân numai referințe de audit. |

Fișierul se descarcă într-un nume temporar și primește extensia finală numai după verificări. README-ul principal păstrează exclusiv candidatul curent; după `PUBLISHED`, linkul local se înlocuiește cu asset-ul aceleiași versiuni. Lipsa publicării nu permite revenirea la APK-ul anterior.

### Verificări automate ale distribuției

| Control | Scop și limită |
|---|---|
| [Guvernanța linkurilor](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactLayoutGovernanceTest.java) | Aceeași versiune în catalog, etichetă și APK, local sau publicat; refuză `latest` ambiguu, Raw/blob, descărcări concurente, orice APK vechi în README și directoarele retrase. |
| [Componentele descărcării](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/download/ApkDownloadComponentTest.java) | HTTP loopback real: răspuns complet, 404, text salvat ca APK, trunchiere, exces, SHA greșit, CRC invalid, metadate divergente și credențială nepropagată către altă origine. Fixture-ul ZIP nu califică instalarea Android. |
| [Selecția identității](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/download/DownloadSelectionComponentTest.java) | Un rezultat pozitiv al versiunii online anterioare nu califică descărcarea candidatului curent. Numărul de bytes este citit exact, fără conversie numerică permisivă. |
| [Acceptanța live](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/download/PublishedApkDownloadAcceptanceTest.java) | Descărcare GitHub explicită, cu versiune, bytes, SHA-256, CRC și verdict în Extent. Ținta este exclusiv `currentVersion`; publicarea absentă produce HTTP 404 și FAIL, fără accesarea unui APK anterior. |

Dintr-o copie TMP cu dependențele disponibile, componentele rulează fără acces GitHub:

```powershell
mvn -B -o -pl atf-tests -am '-Dtest=ReleaseArtifactLayoutGovernanceTest,ApkDownloadComponentTest,DownloadSelectionComponentTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

După publicare, verificați APK-ul curent prin proba separată de rețea:

```powershell
mvn -B -o -pl atf-tests -am '-Dtest=PublishedApkDownloadAcceptanceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Datf.release.download.enabled=true' '-Datf.release.download.scope=current' test
```

- **Canal:** tokenul se citește din `GH_TOKEN` sau `GITHUB_TOKEN`; nu se transmite prin argumente, YAML ori rapoarte. Testul nu publică și nu instalează aplicația.
- **Scope:** numai `atf.release.download.scope=current` este acceptat. Identitatea și mărimea așteptată provin din manifestul curent, inclusiv când acesta este încă `PREPARED`; testul confruntă publicarea reală.
- **Raport:** `atf.release.download.report-directory` cere o cale absolută; implicit folosește TMP. Erorile și refuzul publicării lipsă rămân FAIL, fără PASS dintr-un subset mai vechi.
- **Suită offline:** guvernanța obișnuită exclude tag-ul `release-download-live`; acceptanța de rețea se selectează explicit, fără a transforma omisiunea ei într-un rezultat pozitiv.

- **Inventar:** indexul structurat, dosarele și catalogul formează aceeași populație verificată.
- **Calificare:** validarea structurală și [legarea notelor de asset-ul publicat](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java) sunt controale distincte; trecerea lor nu dovedește accesul anonim, proveniența Git completă sau autonomia raportului.

### Integritatea fișierelor și rezultatelor

[Guvernanța integrității](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java) citește APK-ul și dosarul activ fără a le modifica. Confruntă numele, calea canonică din dosarul versiunii, mărimea, SHA-256 și fișierul checksum; citește toate intrările ZIP pentru CRC și refuză numele duplicate ori căile necanonice.

| Legătură verificată | Contract |
|---|---|
| Manifest–payload | Registrul `integrity.files` enumeră exact toate fișierele dosarului, cu mărime și SHA-256; manifestul însuși este exclus pentru a evita autoreferința. Un fișier adăugat, șters sau modificat invalidează observația. |
| Manifest–JUnit | Fiecare grup declară exact XML-urile păstrate. Cazurile sunt renumărate, identitatea clasă–invocare este unică, iar atributele XML și totalurile declarate trebuie să coincidă. |
| Rezultat calificat | Sunt necesare cazuri efective și zero eșecuri, erori ori omisiuni; un container gol nu inventează teste. Un antet cu zero eșecuri nu ascunde un element `failure`, `error` sau `skipped`. |
| Manifest–intrări | Fișierul intrărilor sursă este inclus în registrul hash-urilor; amprenta și commit-ul declarate concordă, iar căile și hash-urile intrărilor sunt valide și fără duplicate. |

Înregistrarea hash-urilor este un pas de pregătire explicit, după fixarea fișierelor:

```powershell
node atf-tests/tools/record-release-integrity.mjs
```

- **Înregistrare:** unealta selectează versiunea din index, verifică stabilitatea inventarului și actualizează secțiunea `integrity`. Momentul `observedUtc` identifică observarea octeților, fără modificarea verdictului, datei execuției sau stării publicării.
- **Verificare:** după înregistrare se execută guvernanța; recalcularea hash-urilor nu face valid un XML cu eșecuri sau un APK corupt.
- **Limite:** CRC-ul și SHA-256 califică integritatea locală, separat de semnătura APK-ului, manifestul Android binar, reproducerea build-ului și identitatea commit-ului publicat. Lista intrărilor nu confruntă încă arhiva externă completă ori populația surselor live.
- **Catalog:** delimitarea secțiunilor se validează înaintea intrării curente, cu diagnostic pentru titluri lipsă sau inversate. Prezența unei versiuni în tabelul general nu substituie legătura din secțiunea curentă.

## Prezentarea unei versiuni

Pagina Release este destinată utilizatorului. Păstrează trei secțiuni cu bullets și trimite către dosarul dedicat pentru audit:

| Secțiune | Conținut |
|---|---|
| Descărcare | Autentificare în browser, linkul direct al APK-ului și checksum-ul versiunii. |
| Îmbunătățiri față de versiunea precedentă | Schimbări confirmate pentru binarul publicat, cu versiunea de comparație identificată. |
| Documentație | Legătura către rapoarte și proveniență, într-un commit identificat. |

- **Comparație:** dacă diferențele față de versiunea precedentă nu sunt confirmate, secțiunea se numește **Pe scurt** și descrie doar capabilități cunoscute ale versiunii. Nu transformați rezumatul în istoric inventat; următoarea livrare își adună schimbările pe parcursul dezvoltării.
- **Audit:** numerele testelor, verdicturile tehnice, mesajele despre dispozitive neverificate și diagnosticul distribuției aparțin dosarului de dovezi. Linkul din Release păstrează accesul la ele fără a încărca prezentarea produsului.

[Guvernanța prezentării](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java) confruntă snapshot-ul notelor cu asset-ul și registrul: bullets, destinația APK-ului, checksum, referință de audit și comparație declarată ori rezumat explicit. Probele negative acoperă legături Raw, versiuni greșite, lipsa bazei de comparație și detalii de testare introduse în prezentare.

## Fereastra și versiunile Android noi

[Documentația Android](https://developer.android.com/develop/ui/views/layout/edge-to-edge) descrie afișarea edge-to-edge pentru aplicațiile cu `targetSdk` de cel puțin 35 pe Android 15 sau mai nou. [WindowContent](../../whist-lab-app/app/src/main/java/ro/whistlab/app/window/WindowContent.java) separă protecția vederii de măsurarea ferestrei:

| Cale din cod | Comportament implementat |
|---|---|
| `protect`, API 35+ | Adaugă barele, decupajul și IME la padding-ul inițial și consumă insets-urile. Baza se păstrează, fără acumulare la reaplicare. |
| `widthDp` / `heightDp`, API 30+ | Folosește fereastra curentă și exclude barele și decupajul, independent de vizibilitatea lor. IME nu este scăzut în această măsurare; protecția rădăcinii are cale proprie. |
| Sisteme anterioare | Măsurarea folosește configurația platformei; listenerul de protecție nu este instalat de această clasă înainte de API 35. |

[Componentele ferestrei](../../whist-lab-app/app/src/test/java/ro/whistlab/app/window/WindowPaddingComponentTest.java) verifică aritmetica și bugetele modelului. [Acceptanța nativă](../feedback/ATF_Android.md#acceptanță-nativă-android-17) confruntă viewport-ul real, gesturile, navigarea cu trei butoane, decupajul și tastatura; ea nu rezultă din modelul JVM.

## Memorie și diagnostic pe Android 17

[Android 17 / API 37](https://developer.android.com/about/versions/17/behavior-changes-all) aplică limite de memorie inclusiv aplicațiilor care țintesc versiuni anterioare. Documentația identifică intervenția limiterului prin `ApplicationExitInfo`, motivul `REASON_OTHER` și descrierea care conține `MemoryLimiter:AnonSwap`.

- Identificați APK-ul, sistemul, profilul mesei și starea aplicației din aceeași execuție.
- Înregistrați memoria de referință și evoluția ei la reluare și la mărimi diferite de mână; costul căutării AI și reactivitatea au măsurători proprii.
- Confruntați motivul ieșirii cu jurnalul AndroidRuntime; codul sau mesajul izolat nu substituie corelarea procesului și intervalului.
- Păstrați limitele sistemului pentru acceptanță și consemnați explicit un diagnostic incomplet. Corectitudinea calculelor JVM nu califică memoria procesului Android.

## Restaurarea datelor

1. Exportați salvările înaintea intervenției asupra unei aplicații cu date existente.
2. Verificați integritatea exportului înaintea instalării, resetării sau curățării.
3. Tratați eroarea de citire drept eroare, fără a o interpreta ca absență a datelor.
4. Verificați restaurarea și schema înainte de a elimina copia de siguranță.
5. Păstrați explicit limitele migrării informației lipsă.

[Build-ul aplicației](../../whist-lab-app/README.md) · [Testarea Android](../testing/ATF_Android.md) · [Istoricul scorului](../game/ATF_ScoreHistory.md)
