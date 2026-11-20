# Audit Whist 0.22

## Pe scurt

Componentele sunt calificate pe surse identificate, cu rapoarte Extent canonice și receipt-uri originale. Selecția derivată din diff nu cere E2E pentru această ediție de mentenanță.

[Deschide raportul canonic al cadrului](component/atf-tests/component-report.html)

## Identitate

| Element | Valoare |
|---|---|
| APK | WhistLab-0.22.apk |
| versionCode | 45 |
| Sursa calificată | `df7d1954a0b4098f9678ed54acc911a57600adff` |
| Integritate | ## Distribuție

Descărcarea autentificată prin API și transferul anonim finalizat în browser corespund APK-ului calificat. Aceste verificări au rapoarte separate și nu se adaugă inventarului componentelor.

- [Extent pentru transferul APK](download-extent.html)
- [Extent pentru descărcarea din README](browser-download-extent.html)
- [Identitatea livrării](distribution-verification.json)

[Manifest](../release-validation.json) |

## Componente

| Colecție | Invocări | Verdict | Raport |
|---|---:|---|---|
| atf-validation | 19 | PASS | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | PASS | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 549 | PASS | [Extent](component/atf-tests/component-report.html) |
| whist-lab-app-achievements | 28 | PASS | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-bidding | 255 | PASS | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | PASS | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | PASS | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | PASS | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-atf-card-table-ui | 56 | PASS | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
| whist-lab-app-rules | 40 | PASS | [Extent](component/whist-lab-app-rules/component-report.html) |

Total: **1515 invocări unitare/de componentă**. Controalele suplimentare ale sursei se suprapun peste colecția primară și nu se adună la acest total.

## Selecția Android

E2E: **NOT_APPLICABLE**. Diff-ul schimbă validarea livrării, documentația și numai valorile literale ale versiunii Android; regulile, interfața și fluxurile jocului sunt păstrate.

## Proveniență și citire

- Rapoartele păstrează intrările, comparațiile și imaginile inline Base64 ale execuției originale.
- [Receipt-ul componentelor](component/source-component-run.json) și snapshot-urile originale descriu selecția și reutilizarea.
- [Execuția CI](diagnostics/cloud-qualification.json) identifică raportul proaspăt al cadrului.
- [Identitatea APK](diagnostics/apk-identity.json) și [semnarea](diagnostics/signing-compatibility.json) sunt verificate separat.
- [TWO-PART-RELEASE-AND-AUDIT](supplemental/test-change-controls/TWO-PART-RELEASE-AND-AUDIT/component-report.html)

[Manifest](../release-validation.json) · [Catalogul livrărilor](../../README.md) · [Strategia testării](../../../docs/testing/README.md)
