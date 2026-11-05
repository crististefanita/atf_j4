# Istoricul partidei și calificarea interfeței

## Responsabilitate

Motorul deține datele partidei, iar aplicația Android deține persistența și prezentarea lor. Modificările interfeței se califică pe binarul efectiv instalat, cu starea și scopul scenariului declarate.

## Diferență de contract: levatele istorice

[ScoreHistory](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/ScoreHistory.java) păstrează licitațiile și soldurile după mâini; punctele se derivă din diferența soldurilor. Levatele fiecărui loc nu sunt stocate separat în acest registru.

| Prioritate | Cerință rămasă | Criteriu de închidere |
|---|---|---|
| P1 | Istoricul complet solicitat include și levatele câștigate. | Definiți schema și migrarea, păstrați informația necunoscută în salvările vechi și verificați asocierea loc–mână–licitat–levate–puncte–total. |

Un sold sau o diferență de puncte nu justifică reconstruirea unor levate lipsă. Contractul implementat și limita migrării rămân în [modelul istoricului](../game/ATF_ScoreHistory.md).

## Calificarea modificărilor vizuale

| Prioritate | Suprafață | Verificare necesară înainte de acceptare |
|---|---|---|
| P1 | Masă, carte și atu | Profiluri declarate, text mărit, intersecții reale, coordonate logice mapate la raster și revizie a capturii originale. |
| P1 | Istoric și reluare | Toate mâinile încheiate și toate locurile accesibile, fără acordarea repetată a punctelor ori avansarea partidei. |
| P1 | Accesibilitate și localizare | Ordine asistată, etichete ale valorilor necunoscute ori negative, primul și ultimul element derulabil în ambele limbi. |
| P2 | Nume configurat și insignă de tură | Verificați numele la lungimea maximă acceptată, în toate pozițiile și cu insigna activă; identitatea vizibilă trebuie să fie lizibilă, iar prescurtarea să aibă o regulă explicită și acces la numele complet. |

O schimbare de geometrie sau stil se leagă de sursele și APK-ul inspectate. Calificarea pe dispozitiv și auditul componentelor sunt înregistrări distincte, păstrate extern.

## Acceptanță nativă Android 17

Protecția ferestrei și calculul spațiului util sunt implementate; acceptanța pe API 37 rămâne distinctă de componente și de emulatorul API 28. Versiunea 0.1.5 este reperul funcțional raportat de utilizator, cu identitatea binarului și a sistemului de confirmat la comparație.

| Prioritate | Cerință rămasă | Criteriu de închidere |
|---|---|---|
| P1 | API 37: fereastră, reluare și memorie | Pe un dispozitiv identificat: navigare prin gesturi și trei butoane, decupaj, tastatură, reluare și câteva mâini; înregistrați memoria, motivul ieșirii și hash-ul APK-ului, fără modificarea limitelor sistemului. |

API 37 nu este disponibil în mediul actual și nu se execută în această iterație, conform cerinței utilizatorului. [Contractul livrării](../platform/ATF_AndroidDelivery.md) leagă această acceptanță de documentația oficială și de diagnosticul `MemoryLimiter:AnonSwap`.

## Publicare și sumarul checkpoint-ului

| Prioritate | Lacună observată | Criteriu de închidere |
|---|---|---|
| P2 | Un sumar de aserție peste limita configurată este respins numai la publicarea finală, după execuția UI. | Validați contractul checkpoint-ului înaintea capturii următoare, prin aceeași politică efectivă; păstrați geometria completă în observații și XML, fără trunchierea dovezii. |

Starea capturii și rezultatul publicării sunt distincte: `flow.status=PASS` singur nu dovedește o publicare reușită. Handoff-ul trebuie să confrunte rezultatul JUnit, auditul HTML și proveniența replay-ului; [registrul release-ului](../../artifacts/reports/README.md) păstrează această distincție.

## Izolarea cache-ului Maven pe Windows

| Prioritate | Lacună reprodusă | Criteriu de închidere |
|---|---|---|
| P2 | Bootstrap-ul reinstalează JAR-urile ATF în cache-ul comun, iar un proces paralel care le citește poate bloca înlocuirea înaintea testelor. | Runnerul pregătește coordonate ATF private pentru execuție, verifică hash-urile și evită publicarea în cache-ul folosit de alt proces; cazul concurent rămâne regresie de infrastructură. |

Selectarea unui repository propriu este deja parte a contractului de lucru. Valoarea implicită a runnerului și pregătirea lui trebuie aliniate cu acest contract; un eșec de bootstrap nu este un verdict asupra interfeței.

## Criterii de verificare

- Componente: contract exact sau ratat, scor negativ, serii configurate, toate locurile și tranzițiile istoricului.
- Persistență: licitare, levată, închiderea mâinii, sfârșitul partidei, date parțiale și date corupte.
- Interfață: rang și culoare lizibile, ținte accesibile, roluri semantice distincte și revenire cu starea păstrată.
- Raport: acțiune, observație, comparație și captură asociate aceleiași stări.

## Acoperire E2E minimă pentru profil, istoric și clasamente

Componentele JVM verifică mecanismele, iar [scenariile Android](../../atf-test-android/src/test/java/atf/android/e2e/records/WhistRecordsE2eTest.java) verifică interacțiunea pe un APK identificat, sub rezervarea exclusivă a dispozitivului. Starea „automatizat” descrie existența verificării; verdictul unei versiuni aparține execuției externe identificate. Fixture-urile și replay-urile de diagnostic nu trebuie să alimenteze clasamentele utilizatorului.

| Stare | Scenariu minim | Criteriu de acceptare |
|---|---|---|
| AUTOMATIZAT | Configurare și resetare nume | Setare din meniul inițial, nume accesibil pe masă, relansare, resetare din meniul partidei; păstrarea cărților și scorurilor. Lizibilitatea numelui și a insignei păstrează criteriul vizual separat. |
| AUTOMATIZAT | Nume AI și reluare | Două partide noi au nume scurte alese dinamic; reluarea aceleiași partide păstrează numele și nu produce dubluri la masă. |
| AUTOMATIZAT | Tabel comun al istoricului | Toate mâinile și toate locurile accesibile, antete aliniate în timpul derulării orizontale, celule complete la finalul derulării; verificare în RO/EN și pe un profil compact. |
| AUTOMATIZAT | Serii simetrice | Selectare ±5 și ±10; închidere controlată a unei serii reușite și ratate, apoi relansare fără acordarea repetată a punctelor. |
| AUTOMATIZAT | Top local după încheiere | Exemplele sunt marcate, rezultatele omului și AI sunt clasate corect, Top 10/20 păstrează aceleași date, iar relansarea rezultatului final nu dublează intrările. Ultimul rând este verificat integral, nu doar prin existența nodului. |
| PARȚIAL | Izolare și salvare Android | Schimbarea configurației și excluderea fixture-urilor sunt automatizate; recuperarea `AtomicFile` după corupere sau întrerupere rămâne de calificat prin interfață. |

Publicarea E2E include poze inline base64 și legătura dintre configurație, acțiune, comparație și rezultat. Acest catalog nu înlocuiește rezultatul execuției pe dispozitiv.

## Distribuție GitHub: accesul anonim

| Prioritate | Blocaj observat | Criteriu de închidere |
|---|---|---|
| P1 extern | Profilul, repository-ul și asset-ul returnează 404 anonim; API-ul autentificat declară repository public și release publicat, iar asset-ul complet trece hash-ul și CRC. | Confirmați vizibilitatea contului cu GitHub Support; URL-ul public al asset-ului trebuie să răspundă 200 și să producă exact mărimea și SHA-256 ale binarului verificat. |

[Proba actuală](../../artifacts/reports/WhistLab-0.1.9/github-download-verification.json) separă upload-ul, descărcarea autentificată și accesul anonim. Cauza exactă nu este confirmată de API; simptomul este compatibil cu ascunderea conținutului la nivel de cont, mecanism descris de [GitHub](https://github.blog/news-insights/policy-news-and-insights/2022-transparency-report/). Nu modificați APK-ul pentru a compensa un răspuns HTTP negativ.

Date utile pentru suport: repository `crististefanita/atf_j4`, release `v0.1.9`, ID `435027600`, asset `712919432`; asset-ul are 771.391 bytes și digest identic cu [registrul release-ului](../../artifacts/reports/README.md). Verificarea autentificată este PASS; distribuția publică rămâne necalificată.

[Testarea Android](../testing/ATF_Android.md) · [Profiluri de ecran](../testing/ATF_Display.md) · [Atlasul interfeței](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md)
