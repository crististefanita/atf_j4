# Whist Lab 0.1.3 — acceptanța interfeței

## Ce primește jucătorul

La 13 cărți, rangul și simbolul se citesc unul sub altul în partea expusă a fiecărei
cărți. Mâna rămâne pe un rând. La patru locuri, toate cele nouă valori legale
încap în centrul mesei. Alegerea valorii 8 păstrează poziția fiecărui scaun.
Spațiul dintre regiuni provine din dimensiunile măsurate ale copiilor Android.

[APK](../dist/WhistLab-0.1.3-debug.apk) · [Sumă SHA-256](../dist/WhistLab-0.1.3-debug.apk.sha256) · [Atlas de layout](../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md)

## Proveniența binarului

| Intrare | Valoare verificată |
|---|---|
| Identitate | `ro.whistlab.app`, versiune 0.1.3, cod 4, semnătură debug locală |
| Android | `minSdk 24`, `compileSdk 36`, `targetSdk 36`. Execuție pe emulator Android API 28 |
| SHA-256 APK | `3F1E7452C2AC4511148F32C634F41A128078B4C44874D63ACE38079EA553405A`. Exemplarul extras după instalare prin ADB are același hash |
| Bază de lucru | `ac4edf8`, cu modificările UX ale versiunii și corecția AI locală `PublicSalvage.java` |
| Motor AI compilat | SHA-256 `9636BAD44F066232FD6C0BBC47A88C37162DF18BE98C00115E99B3E3AC67BC3C`. Blob Git local `c3967b8f0b9c842e6524c501f162da9c6aa10b08`, anterior `4e2efb013e2ccaa1d0e43d9a296bfbd04b34a687` |
| Limita de reproducere | Checkout-ul commitului UX singur nu reproduce sursa AI. Pentru acest motor se aplică diferența locală din `engine-live.diff` și se verifică hash-ul de mai sus |

În calculul levatelor câștigate sigur, motorul local ia în calcul atuul mai mare
deja jucat în levata deschisă. Dacă jucătorul poate pierde legal un atu sub el,
estimarea scade cu o levată. Corecția aparține lucrării AI. Pilotul de mai jos
a rulat această versiune exactă.

## Probe de acceptanță

| Probă | Ce s-a observat |
|---|---|
| Componente aplicație | 25 teste trecute. Matricea de geometrie parcurge 6.480 combinații pentru 3–6 jucători, 1–18 opțiuni, cinci lățimi, două spațieri și dimensiuni variabile ale scaunelor și centrului |
| Componente consumer Android | 20 de teste trecute. Opt verifică publicatorul și indexul din feedback. Celelalte 12 acoperă geometria extrasă din ierarhie, auditul de pixeli, acțiuni și identificarea profilului. Negativele acoperă simbol absent, margine tăiată și identificarea ambiguă prin text. Indexul păstrează și un FAIL auditat, iar pachetul original și probele offline sunt excluse |
| Cele două defecte de display | 12 checkpoint-uri. La fiecare profil se verifică cei 13 indici, inclusiv cele patru cărți de 10, apoi cele nouă licitații și alegerea valorii 8. Proba compară dreptunghiurile efective și pixelii din ambele linii ale indicelui |
| Licitare cu 13 cărți | Flux separat pe mai multe rezoluții, inclusiv revenirea la profilul inițial. Se verifică opțiunile legale și încadrarea fețelor cărților |
| Șase locuri și opt cărți | După măsurarea regiunii centrale, cele două flancuri de sus și cele două de jos formează arcul din mock. Deschiderea și închiderea scorului păstrează licitația legală selectată și totalul curent |
| Carte jucată din mâna de 13 | Tap pe o carte legală, 12 cărți rămase și cartea în centru. Poziția scaunului sudic rămâne fixă |
| Navigare EN și RO | 24 de checkpoint-uri pe trei dimensiuni. Home, configurare, reguli și scor sunt accesibile în ambele limbi. La 320 dp, `Lic.` și `Pct.` încap în tabelul românesc |
| Pilot din meniul normal | Whist românesc pe Hard, patru jucători, șase mâini, nouă levate și 36 de mutări. Totaluri finale 25, 19, 26, 18. Partida se reia după repornirea procesului. Verdictul de joc și auditul raportului au trecut |
| Extent în Edge | Pentru display s-au expandat 28 de noduri și 38 de detalii. Cele 12 tabele de aserțiuni au ultimul rând accesibil. În pilot s-au expandat 68 de noduri și 87 de detalii. Vizualizarea PNG păstrează proporțiile și starea arborelui după închidere |

| Profil de display | Lățime logică aproximativă |
|---|---:|
| 720 × 1280 la 360 dpi | 320 dp |
| 972 × 1728 la 440 dpi | 353 dp |
| 1080 × 2448 la 440 dpi, profil ROG 7 | 393 dp |
| 1200 × 1920 la 320 dpi | 600 dp |

Lățimea disponibilă cărților exclude marginile aplicației. În capturile emulatorului,
benzile negre exterioare apar când profilul logic are alt raport de aspect decât
display-ul fizic. Auditul transformă coordonatele înainte de citirea pixelilor.
Verificările pe ROG 7 fizic, Android 16 și cu TalkBack rămân deschise.

## Interpretarea reviziei UX

| Axă | Evaluare de revizie | Limită observată |
|---|---:|---|
| Lizibilitatea mâinii în portret | 98/100 | Cei 13 indici, inclusiv toate cărțile de 10, au probe de pixeli la font standard |
| Licitare și poziția jucătorilor | 96/100 | La 320 dp și nouă licitații, flancurile de sus au foarte puțin spațiu pentru arc |
| Navigare EN/RO | 96/100 | Fluxurile obișnuite au fost parcurse în ambele limbi pe emulator |
| Scor și continuitate | 92/100 | Scorul curent și reluarea au probe reale. Istoricul consultabil pe mâini lipsește |
| Auditarea raportului | 98/100 | Fiecare dintre cele șase rapoarte acceptate a fost deschis în Edge și verificat până la ultimul rând de aserțiuni |

Scorurile sunt aprecieri ale fluxurilor observate. Pentru utilizarea cu TalkBack și pe ROG 7 fizic rămân necesare probe directe.

## Regăsirea dovezilor

Cele șapte HTML-uri, manifestele, auditurile legate de SHA-256 și trace-urile
publice disponibile au și copii imuabile în afara TMP. [Arhivă și replay offline](WHIST_UI_REPORT_HANDOFF.md)

Build-ul, capturile integrale, perechile XML/PNG, rapoartele și hash-urile sunt în
`%TEMP%\ATF_J4_UX_GEOMETRY_20261103`. Directorul TMP poate fi curățat ulterior.

| Cale sub TMP | Conținut |
|---|---|
| `build-release.log`, `build-live-resync.log`, `apk-provenance.json`, `final-build-inputs.json` | Build, reverificare după adăugarea unei sarcini de teste Gradle în live, identificarea dispozitivului și comparația celor 49 de intrări |
| `engine-live.diff` | Diferența AI aplicată la bază. SHA-256 al fișierului `438F9FC5427457DA2CCDA69E9818FBC0B025762592B9D43FE9FFC4A7408FEB9E` |
| `release/dense-layout/dense-layout.html` | Matricea, capturile și comparațiile per indice și per control |
| `release/thirteen-card-bidding-resolutions/whist-lab-thirteen-card-bidding-resolutions.html` | Licitare cu 13 cărți |
| `pilot-verified/six-hands/romanian-match-extent.html` | Pilot cu tabel de acțiuni, observații, aserțiuni și PNG per checkpoint |
| `release/browser-dense/browser-audit.json`, `pilot-verified/browser/browser-audit.json` | Verificări interactive în Edge |
| `release/menu-layout/menu-layout.html` | Home, configurare, reguli și scor în EN și RO |
| `release/table-presentation/whist-lab-table-presentation.html` | Șase jucători, licitație, scor și revenire |
| `release/thirteen-card-fan/whist-lab-thirteen-card-fan.html` | Mâna de 13 cărți și mutarea pe masă |
| `release/six-hands/romanian-match-extent.html` | Execuție cu FAIL la reluare. Aplicația afișa „Reia partida”, iar pilotul presupunea limba engleză. Corecția alege limba din dropdown înaintea jocului |
| `acceptance-result.json` | Coroborarea binarului, componentelor, celor șase fluxuri și celor șase audituri în browser |

[Ultimul Extent publicat](feedback/whist-extent/README.md) · [Registrul UX](feedback/WHIST_LAB_User_Experience_Feedback.md) · [Contractul de raportare](ATF_Android_Report_Contract.md) · [Acceptanța anterioară](WHIST_UI_012_ACCEPTANCE.md)
