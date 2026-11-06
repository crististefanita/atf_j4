# Calificarea Whist Lab 0.10.2

## Pe scurt

Versiunea este PUBLISHED: APK-ul calificat și checksum-ul sunt în GitHub Releases; descărcarea integrală curentă a trecut prin API autentificat. [Extent — legenda istoricului](e2e/history-legend.html) este intrarea principală; [inventarul](qualification.json) fixează identitățile și limitele.

## Citirea rapoartelor

1. Deschideți contextul rulării și binarul identificat, apoi invocarea și faza.
2. Extindeți inputul ori secvența de acțiuni, observația și comparațiile până la ultimul rând.
3. Deschideți PNG-ul integral și reveniți la același checkpoint. Corpurile XML/JSON și capturile sunt în HTML.

## Componente Maven pe sursa nouă

[Agregatele suitelor primare](component/live-source/README.md) au proveniență și invocări proprii. Verifică snapshot-ul sursei, fără a recalifica APK-ul, componentele Gradle sau selecția Android de mai jos.

## Componente auditate

| Selecție | Invocări HTML/JUnit | Raport |
|---|---|---|
| Geometrie, driver și istoric | 150/150 | [Extent](component/display-extent.html) |
| ADB, procese, capturi, font și corpuri integrale | 21/21 | [Extent](component/adapter-extent.html) |
| Trace, publicare, republicare și timing | 27/27 | [Extent](component/trace-publication-extent.html) |
| Contractele predării | 37/37 | [Extent](component/framework-extent.html) |
| Istoric, renderer alternativ | 9/9, subset repetat | [Fallback](component/history-fallback.html) |

Identitățile invocărilor sunt confruntate exact, inclusiv parametrizările și clasele imbricate. Cele nouă invocări fallback se suprapun selecției display; nu măresc totalul unic. Cele două controale de integrare lansează separat cazuri PASS, FAIL, abort și lipsa aserțiunilor, în ambele renderere.

## Android și APK

| Selecție efectivă | Scop | Raport |
|---|---|---|
| Legendă: compact EN 1,0 și RO 1,5 | Text întreg, legendă fixă și deplasarea coloanelor | [Extent](e2e/history-legend.html) |
| Reluare: portret înalt, text 1,3, RO/EN | Același registru cu 24 de mâini încheiate | [Extent](e2e/history-release-sanity.html) |
| Release semnat: meniu, configurare, revenire | APK-ul instalat are hash-ul livrat | [Extent](e2e/signed-release-smoke.html) |

- Aplicație JVM: **236** teste, fără erori sau omiteri; construire Debug/Release și lint vital trecute. Replay-ul arhivei externe rămâne opt-in și nu face parte din selecție.
- Adaptor Android: **200** teste, din selecția display și reverificarea focalizată a publicării; contractele framework: **37**; Kit: **13**; controalele negative de împachetare: **83**. Rezultatele originale sunt consolidate pe selecție în `test-results`, fără reevaluare sau transformarea lor în comparații HTML necapturate.
- Android API 28: **3** teste JUnit trecute, cu identități separate pentru Debug cu fixture-uri și Release semnat. Datele originale sunt restaurate și aplicația rămâne închisă.
- APK-ul și checksum-ul sunt [lângă manifestul versiunii](../README.md). [Intrările APK-ului](apk-inputs.json) și inventarul arhivei surselor păstrează proveniența construirii în TMP.

- Guvernanță: **58** teste trecute pentru cod, documentație și livrare. Populația unică consolidată este **630**; include unit-uri, componente și E2E, din selecții compuse, fără a aduna reverificările suprapuse.

## Distribuție și README

| Selecție suplimentară | Invocări | Raport |
|---|---|---|
| Catalog, link principal și refuzul directoarelor retrase | 24 | [Extent](distribution/readme-release-policy.html) |
| Descărcare completă și răspunsuri defecte controlate | 13 | [Extent](distribution/download-components.html) |
| Identitatea candidatului față de versiunea publicată | 2 | [Extent](distribution/download-selection.html) |
| Descărcarea API a versiunii curente 0.10.2 | 1, PASS | [Extent](distribution/current-download.html) |
| Conservarea octeților în Git, inclusiv din staging fără `.git` | 2, PASS | [Extent](distribution/git-evidence-policy.html) |

Selecția finală are 47 de verificări offline trecute; cele 39 de invocări de componentă de mai sus sunt un subset. Șase controale separate ale proprietății suitelor au trecut. [Inventarul suplimentar](distribution-verification.json) separă execuțiile și identitățile; totalul istoric de 630 rămâne neschimbat.

Proba curentă confirmă 803.960 bytes, SHA-256 și toate CRC-urile. Testul JUnit are cinci comparații efective în Extent; verificarea separată a descărcării confruntă și checksum-ul primit. Referința veche a fost retrasă după calificarea ediției curente.

- **Diagnostic păstrat:** [HTTP 404 înaintea publicării](diagnostics/initial-github-404.html) rămâne FAIL; nu este rescris ca descărcare reușită. Noua execuție califică starea publicată.
- **Prepublicare:** selecțiile pentru infrastructura modificată și guvernanță au trecut. Cele două probe opt-in de învățare și deep suite au fost omise explicit; inventarul separă selecțiile și nu mărește artificial populația inițială.
- **Ținta:** README-ul principal și asset-ul identifică exclusiv 0.10.2. APK-ul păstrează cele 114 intrări identificate prin commit-ul surselor; modificările live ulterioare nu primesc automat calificarea acestui binar. Testele jocului nu au fost reluate pentru publicarea dovezilor.
- **Randarea:** grupurile de componentă și comparațiile descărcării sunt verificate detașat, cu cererile externe blocate. Aceasta este revizia HTML-ului, separată de descărcarea APK-ului prin browser.
- **Browser:** MCP raportează `Local browser use is disabled in Work settings`; lansarea Chrome prin ADB a fost refuzată de revizia automată cu `blocked by policy`. Probele API și Android de mai jos nu califică traseul vizual personal autentificat.

## Descărcare reală pe Android

- **[Extent nativ](distribution/mobile-download.html):** DownloadManager pe API 28 a primit 803.960 bytes în 888 ms; SHA-256 și CRC corespund. Cele 11 comparații native includ pachetul `ro.whistlab.app`, versiunea 0.10.2, codul 15, SDK 24/36 și certificatul exact. Alte două comparații verifică fișierul extras de pe dispozitiv.
- **Proveniență:** Android a efectuat transferul de rețea prin redirectul autorizat al asset-ului; tokenul GitHub a rămas pe calculator. Rezultatul integral și sursele probei sunt în HTML; URL-ul temporar semnat este exclus. Aceste comparații nu măresc totalul JUnit de 630.
- **[Diagnostic nativ](diagnostics/mobile-download-diagnostics.html):** pagina proiectului fără sesiune a răspuns 404; linkul canonic neautentificat a eșuat cu motivul 404 și zero bytes. Prima probă autorizată a primit APK-ul complet, dar cititorul certificatului a eșuat pe API 28; FAIL-ul inițial este păstrat.
- **Corecția probei:** solicitarea ambelor flag-uri de semnătură păstrează certificatul așteptat și cerința unui singur semnatar. Reexecuția autorizată a trecut toate cele 11 comparații; nu s-au relaxat praguri sau identități.
- **Predare:** copia real descărcată este în `/sdcard/Download/WhistLab-0.10.2.apk`. Hash-ul a fost confruntat și după dezinstalarea helperului temporar; jocul și datele lui nu au fost modificate. Rezervarea emulatorului este eliberată.
- **Randare:** cele cinci cazuri din cele două HTML-uri au comparațiile și corpurile integrale verificate detașat, inclusiv închiderea/redeschiderea și verdicturile FAIL păstrate. Aceasta nu este o nouă execuție Android.
- **Domeniu:** probă nativă de integrare, republicată offline cu timing-ul original; fără clickuri în browser, instalarea jocului sau o nouă regresie E2E. Transferul autorizat funcționează; cauza accesului 404 al frontend-ului rămâne neconfirmată.

## Revizie și retenție

- Chromium a deschis detașat raportul Extent de componentă, fallback-ul și cele trei rapoarte native, cu resursele externe blocate. Revizia este limitată la ownerii declarați; nu constituie o nouă execuție Android.
- Rapoartele Android finale sunt republicări offline ale trace-urilor calificate: corpurile mari apar în părți consecutive, în plafonul configurat. Proveniența, verdicturile și intervalele originale sunt păstrate; nu s-au reexecutat comenzi Android sau comparații.
- Intrările integrale, grupurile pliabile, ultimul rând și PNG-ul original sunt verificate. Inventarul păstrează hash-urile și confruntarea octeților înaintea eliminării copiilor finale PNG/XML/JSON.
- [Diagnosticul inițial FAIL](diagnostics/font-restoration-failure.html) păstrează eroarea restaurării unei setări de font absente. Modelul de restaurare și o execuție nativă nouă califică remedierea; încercarea inițială nu devine PASS.
- Rezultatele JUnit au un scop distinct: recitirea automată independentă a populației și verdicturilor. Metadatele și intrările surselor califică identitatea APK-ului; nu dublează corpurile auditului din HTML.

## Limite

Acceptanța nativă acoperă API 28 și profilele declarate; API 37 și regresia completă a partidelor nu au fost executate. Componentele Gradle fără adaptor păstrează verdicturi JUnit, iar auditarea lor integrală rămâne în [feedback](../../../docs/feedback/ATF_Reporting.md). Sursele modificate după snapshot nu preiau automat calificarea APK-ului.

[Catalog](../../README.md) · [Strategia comună](../../../docs/testing/README.md)
