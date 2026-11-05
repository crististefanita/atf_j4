# Istoricul partidei și calificarea interfeței

## Responsabilitate

Motorul deține datele partidei, iar aplicația Android deține persistența și prezentarea lor. Modificările interfeței se califică pe binarul efectiv instalat, cu starea și scopul scenariului declarate.

## Diferență de contract: levatele istorice

[ScoreHistory](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/ScoreHistory.java) păstrează licitațiile și soldurile după mâini; punctele se derivă din diferența soldurilor. Levatele fiecărui loc nu sunt stocate separat în acest registru.

| Prioritate | Cerință rămasă | Criteriu de închidere |
|---|---|---|
| P1 | Istoricul complet solicitat include și levatele câștigate. | Definiți schema și migrarea, păstrați informația necunoscută în salvările vechi și verificați asocierea loc–mână–licitat–levate–puncte–total. |

Un sold sau o diferență de puncte nu justifică reconstruirea unor levate lipsă. Contractul implementat și limita migrării rămân în [modelul istoricului](../game/ATF_ScoreHistory.md).

## Persistență și coordonare

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P1 | [GameSessionStore](../../whist-lab-app/app/src/main/java/ro/whistlab/app/persistence/GameSessionStore.java): ordinea stocării față de randare și calcul nu este calificată la eșec sau întârziere. | Componente cu stocare controlată: salvare, reluare și clasament au ordine explicită, fără puncte ori rezultate duplicate. |
| P1 | Recuperarea `AtomicFile` după corupere sau întrerupere nu are acceptanță prin interfață. | Android pe APK identificat: întrerupere controlată, recuperare sau eroare explicită, datele utilizatorului păstrate; componentele serializării rămân verificare separată. |
| P2 | [MainActivity](../../whist-lab-app/app/src/main/java/ro/whistlab/app/MainActivity.java) compune navigarea, comenzile și construcția ecranelor. | Extrageri calificate prin componente: proprietar și durată de viață explicite pentru callback-uri, aceleași tranziții, geometrie și reguli. |

## Calificarea modificărilor vizuale

| Prioritate | Suprafață | Verificare necesară înainte de acceptare |
|---|---|---|
| P1 | Masă, carte și atu | Profiluri declarate, text mărit, intersecții reale, coordonate logice mapate la raster și revizie a capturii originale. |
| P1 | Istoric și reluare | Toate mâinile încheiate și toate locurile accesibile, fără acordarea repetată a punctelor ori avansarea partidei. |
| P1 | Accesibilitate și localizare | Ordine asistată, etichete ale valorilor necunoscute ori negative, primul și ultimul element derulabil în ambele limbi. |
| P2 | Nume configurat și insignă de tură | Verificați numele la lungimea maximă acceptată, în toate pozițiile și cu insigna activă; identitatea vizibilă trebuie să fie lizibilă, iar prescurtarea să aibă o regulă explicită și acces la numele complet. |

- **Regiune:** acțiunea turei are o regiune măsurată deasupra chenarului; componentele verifică dimensiunile și separarea pentru text mărit.
- **Observație:** E2E folosește același decodor pentru joc și licitare; acceptanța nativă rămâne necesară pentru glyph-uri, împărțirea reală pe rânduri și capturi în RO/EN.

- **Sanity:** observați acțiunea integrală deasupra numelui la toate locurile active, inclusiv după schimbarea turei și la revenirea din pauză.
- **Istoric:** verificați prima și ultima coloană după derulare, etichetele numerice ale cărților și continuitatea separatoarelor dintre jucători.

O schimbare de geometrie sau stil se leagă de sursele și APK-ul inspectate. Calificarea pe dispozitiv și auditul componentelor sunt înregistrări distincte, păstrate extern.

## Acceptanță nativă Android 17

Protecția ferestrei și calculul spațiului util sunt implementate; acceptanța pe API 37 rămâne distinctă de componente și de verificarea pe un alt sistem Android. Comparația între versiuni cere identitatea ambelor APK-uri și a dispozitivului.

| Prioritate | Cerință rămasă | Criteriu de închidere |
|---|---|---|
| P1 | API 37: fereastră, reluare și memorie | Pe un dispozitiv identificat: navigare prin gesturi și trei butoane, decupaj, tastatură, reluare și câteva mâini; înregistrați memoria, motivul ieșirii și hash-ul APK-ului, fără modificarea limitelor sistemului. |

[Contractul livrării](../platform/ATF_AndroidDelivery.md) distinge padding-ul cu IME de măsurarea ferestrei fără IME și definește restaurarea și diagnosticul de memorie. Componentele verifică modelul, iar criteriul nativ rămâne deschis până la execuția callback-urilor și a scenariului pe sistemul țintă.

## Memoria adaptorului

Limita de memorie a adaptorului rămâne distinctă de geometria interfeței.

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | [CommandProcess](../../atf-test-android/src/main/java/atf/android/process/CommandProcess.java) citește fluxurile integral, fără bugete separate pentru text și binar. | Componente cu ieșire controlată: depășire explicită a fiecărui buget, memorie delimitată și PNG incomplet refuzat înaintea publicării; termenul și cauza inițială se păstrează. |

## Distribuție GitHub: accesul anonim

| Prioritate | Blocaj observat | Criteriu de închidere |
|---|---|---|
| P1 extern | API-ul Search autentificat confirmă `User flagged as spammy.`; profilul, repository-ul public și asset-ul returnează 404 anonim. | Solicitați revizuirea marcajului contului prin formularul oficial GitHub; după ridicare, URL-ul public al asset-ului trebuie să răspundă 200 și să producă exact mărimea și SHA-256 ale binarului verificat. |

[Registrul distribuției](../../artifacts/README.md) păstrează probele și răspunsul explicit al API-ului Search. Marcajul contului este confirmat; motivul declanșării lui nu este expus și necesită revizuire GitHub. Setarea repository-ului este deja publică, iar release-ul este publicat.

Identitatea repository-ului, a release-ului și a asset-ului, mărimea și SHA-256 aparțin înregistrării distribuției și cererii de suport, fără duplicarea lor în ghid. Distribuția publică rămâne necalificată; modificarea APK-ului nu compensează un răspuns HTTP negativ.

## Automatizarea publicării

| Prioritate | Lacună rămasă | Criteriu de închidere |
|---|---|---|
| P2 | [Procesul versiunii](../platform/ATF_AndroidDelivery.md#procesul-unei-versiuni) este documentat, iar operațiile de publicare folosesc încă unelte temporare. | Un lansator versionat confruntă tag-ul, APK-ul, checksum-ul și notele înainte de publicare; reluarea unui draft verifică asset-urile existente, iar o versiune publicată nu este înlocuită implicit. Actualizează atomic dosarul versiunii, indexul și catalogul, cu linkul Release către auditul fixat pe commit. |

[Guvernanța prezentării](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java) verifică structura și identitatea notelor; sensul schimbărilor se confruntă cu sursele release-ului la revizie. Notele publice trimit către dosarul tehnic, care păstrează rezultatele și limitele verificării.

## Contractele responsabile

- [Scenariile de profil, istoric, clasamente și serii](../testing/ATF_Android.md#profil-istoric-clasamente-și-serii) dețin mecanismele automatizate și precondițiile; existența scenariului nu califică o versiune.
- [Componentele display](../testing/ATF_DisplayComponents.md) dețin mock-urile și izolarea cache-ului; fonturile și interacțiunea nativă au verificare distinctă.
- [Calificarea publicării](ATF_Reporting.md) păstrează lacuna validării timpurii a checkpoint-ului; [predarea rezultatelor](../reporting/ATF_Handoff.md) deține distincția dintre verdictul funcțional și publicare.

[Testarea Android](../testing/ATF_Android.md) · [Profiluri de ecran](../testing/ATF_Display.md) · [Atlasul interfeței](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md)
