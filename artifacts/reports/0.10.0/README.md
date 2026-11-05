# Verificarea Whist Lab 0.10.0

## Pe scurt

**PREPARED — candidat local, fără commit, push sau publicare.** APK-ul este calificat pe sursele de lucru identificate prin hash. Notele publice rămân un template; legătura documentației se fixează la commit-ul efectiv înaintea publicării.

## APK și surse

APK-ul calificat aparține snapshot-ului arhivat. Codul live include și integrare UI ulterioară, exclusă explicit în `source-freeze.json`; calificarea nu se transferă acelui cod.

- [APK local](../../../dist/WhistLab-0.10.0.apk) · [SHA-256](../../../dist/WhistLab-0.10.0.apk.sha256).
- Versiune `0.10.0`, cod Android `13`, minimum API 24, target API 36; certificat de actualizare păstrat.
- Mărime: `783480` bytes; SHA-256: `99b9c39389fbd6be6db162db0c0a22279cbb8d697d74473a198304604e821a80`.
- [Intrările APK-ului](apk-inputs.json), [identitățile blob Git](apk-git-binding.json) și [proveniența surselor](source-provenance.json) disting snapshot-ul necomis de commit-ul de bază. Blob-urile permit confruntarea ulterioară după normalizarea Git a sfârșiturilor de linie; nu reprezintă commit-uri. [Manifestul complet](release-validation.json) leagă rezultatele de acest binar.

## Dovezi selectate

- Teste fără erori sau omiteri: **app: 147, core: 50, adapter: 134, governance: 29, native: 1, history: 1**. XML-urile originale sunt în `test-results`; jurnalele sunt în `logs`.
- [Extent — masa compactă](e2e/compact-api28/responsive-ui-sanity.html): 13 cărți, levată completă și 12 cărți rămase, nouă opțiuni de licitare și selecția fără deplasarea chenarelor.
- [Extent — istoricul](e2e/history-api28/final-extent-report.html): reluarea aceluiași registru de 24 de mâini, RO/EN, text 1,3; precondiția este o partidă jucată de motorul JVM. Matricea cu toate derulările a rulat pe candidatul anterior și are identitate separată în [registrul încercărilor](native-attempts.json).
- [Verificarea structurii HTML](report-structure-verification.json) confruntă payload-urile PNG originale. Capturile și ierarhiile nu au fost rescrise.
- Chromium automatizat a deschis HTML-urile detașate, assertions până la ultimul rând și câte un PNG integral, cu revenire la același owner: [masa](browser/compact/browser-audit.json) și [istoricul](browser/history/browser-audit.json). Proba acoperă ownerul selectat din fiecare raport.
- E2E folosește APK-ul Debug identificat în fiecare dosar; [smoke-ul Release](e2e/history-api28/release-smoke.json) verifică separat APK-ul semnat. [Restaurarea datelor](e2e/history-api28/app-data-restored.json) confruntă cele trei fișiere originale prin hash.

## Domeniu și limite

- [Snapshot-ul calificat](source-freeze.json) păstrează sursele exacte în arhiva externă, cu CRC și hash-uri verificate. Clasele noi de navigare/animații apărute ulterior în live sunt inventariate separat și nu intră în APK-ul pregătit. Publicarea se leagă de snapshot; includerea dezvoltării ulterioare cere un nou candidat.


- [Revizia vizuală](visual-review.json) păstrează o observație deschisă: legenda istoricului este tăiată în dreapta la text 1,3. [Feedback-ul aplicației](../../../docs/feedback/ATF_Android.md#legenda-istoricului-la-text-mărit) definește remedierea și criteriul vizual; comparațiile funcționale trecute nu închid această lacună.
- Android nativ: API 28. Selecția verifică afișajul și istoricul; nu reprezintă o regresie completă a tuturor variantelor, versiunilor Android sau politicilor AI.
- Browserul MCP a expirat la deschiderea raportului. Verificarea statică și o eventuală probă Chromium automatizată au identități separate; nu reprezintă o interacțiune MCP trecută.
- [Încercările native](native-attempts.json) separă matricea anterioară de sanity-ul binarului final. După sincronizarea live, selecția scurtă verifică reluarea și registrul complet; nu atribuie noului APK derulările executate pe candidatul anterior.

## Predarea release-ului

- [Notele schematic pregătite](release-notes.template.md) conțin numai descărcarea, îmbunătățirile și referința către dovezi; nu conțin detalii despre teste.
- Operatorul confruntă sursele cu `apk-inputs.json`, identifică commit-ul efectiv și înlocuiește numai placeholder-ul `{evidenceCommit}`. Publicarea și verificarea descărcării sunt etape ulterioare.
- [Planul de retragere](retirement-plan.json) inventariază release-urile vechi citite prin ID; lista GitHub a fost goală și nu demonstrează absența altor ediții. Nicio ștergere la distanță nu a fost efectuată.

[Catalogul rapoartelor](../README.md) · [Strategia comună](../../../docs/testing/README.md) · [Procesul și retenția](../../../docs/platform/ATF_ReleaseRetention.md)

[Verificarea finală a predării](handoff-verification.json) identifică selecția trecută și limitele pachetului local.
