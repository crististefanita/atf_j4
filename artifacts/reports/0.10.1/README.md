# Verificarea Whist Lab 0.10.1

## Pe scurt

**PREPARED — candidat local, fără commit, push sau publicare.** Corecția legendei și selecția de componente sunt calificate pe snapshot-ul identificat prin hash.

## APK și surse

- [APK local](../../../dist/WhistLab-0.10.1.apk) · [SHA-256](../../../dist/WhistLab-0.10.1.apk.sha256).
- Versiune `0.10.1`, cod Android `14`, minimum API 24, target API 36; certificat de actualizare păstrat.
- Mărime: `799864` bytes; SHA-256: `6c32f6a253cd5db7bbf0752c467dcf36fee4ed25e0cc5087525e9ae45bbd62aa`.
- [Intrările APK-ului](apk-inputs.json) și [snapshot-ul arhivat](source-freeze.json) fixează sursele efective. [Sincronizarea live](live-sync-final.json) identifică integrarea preluată; dezvoltarea ulterioară nu primește automat această calificare.

## Dovezi selectate

- Teste fără erori sau omiteri: **aplicație: 203, adaptor: 159, guvernanță: 29, nativ: 1**. XML-urile originale sunt în `test-results`; jurnalele sunt în `logs`.
- [Extent — legenda istoricului](e2e/history-legend-api28/final-extent-report.html): profil compact EN normal și RO la text 1,5, înainte și după derularea coloanelor. Precondiția are 24 de mâini jucate de motorul JVM; nu reprezintă o partidă completă jucată prin UI.
- [Revizia vizuală](visual-review.json) confirmă explicația integrală, aliniată și fixă; [verificarea structurii](report-structure-verification.json) confruntă toate payload-urile PNG originale.
- [Chromium offline](browser/legend/browser-audit.json) deschide HTML-ul singur, assertions până la ultimul rând și PNG-ul integral 1080×1920, cu revenire la același owner. Proba interactivă acoperă primul owner declarat; captura RO mărită are și revizie vizuală separată.
- [Identitatea Debug observată](e2e/history-legend-api28/apk-provenance-e2e-debug.json) este distinctă de [smoke-ul Release semnat](e2e/history-legend-api28/release-smoke.json). [Restaurarea](e2e/history-legend-api28/app-data-restored.json) confruntă cele trei fișiere originale după smoke; aplicația rămâne închisă.

## Eficiență și limite

- Componentele califică politica verticală, configurațiile invalide, duplicatele, observațiile XML defecte, recitirea limitată a controalelor și identitatea APK-ului. Aceste cazuri intră în suita offline permanentă; fontul, derularea și PNG-ul păstrează proba nativă.
- [Registrul încercărilor](native-attempts.json) separă matricea anterioară de cinci cazuri de cele două cazuri ale binarului final. Nu se transferă verdicte între APK-uri.
- Acceptanța nativă este pe API 28. API 37, sublinierile raster, importul vechi, toate locurile/rândurile și integrarea nativă a animației păstrează criteriile din [feedback](../../../docs/feedback/ATF_Android.md).
- HTML-ul include capturile și hash-ul instalat; autonomia completă a XML/JSON și raportarea componentelor în HTML rămân în [feedback-ul adaptorului](../../../docs/feedback/ATF_Reporting.md). Nu se pretinde acceptanță prin browserul MCP.

## Predarea release-ului

- [Manifestul complet](release-validation.json) leagă binarul de rezultate. [Notele pregătite](release-notes.template.md) conțin numai descărcarea, îmbunătățirile și referința către dovezi.
- Operatorul confruntă sursele, fixează commit-ul efectiv și înlocuiește `{evidenceCommit}` înaintea publicării. Starea locală nu confirmă un asset GitHub disponibil.
- [Retenția locală](retention.json) identifică arhiva verificată a ediției precedente; [verificarea finală](handoff-verification.json) confruntă pachetul activ. Nu s-au făcut mutații GitHub.

[Catalogul rapoartelor](../README.md) · [Strategia comună](../../../docs/testing/README.md) · [Procesul și retenția](../../../docs/platform/ATF_ReleaseRetention.md)
