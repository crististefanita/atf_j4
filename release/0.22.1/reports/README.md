# Audit Whist 0.22.1

## Pe scurt

Planul RAG selectează dovezile componentelor pe baza diferenței de surse. Rulează suita de guvernanță afectată, reutilizează nouă grupuri de module neschimbate cu proveniența lor originală și nu selectează fluxuri E2E Android.

[Deschideți raportul Extent canonic ATF](component/atf-tests/component-report.html)

## Identitate

| Element | Valoare |
|---|---|
| APK | WhistLab-0.22.1.apk |
| versionCode | 46 |
| Sursa calificată | `a58a1233c49de858378e6c03ef92cbcc1f56fc5b` |
| APK SHA-256 | `cf3510909002e7190ad0dfab3305cfced84bd7d435b1b4190f6b7ad9e727f740` |
| Dimensiune APK | 1029553 bytes |
| Execuție nativă Android | NOT_RUN |

## Suite de componente

| Colecție | Invocări | Selecție | Raport |
|---|---:|---|---|
| atf-validation | 19 | REUSE | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | REUSE | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 555 | RUN | [Extent](component/atf-tests/component-report.html) |
| whist-lab-app-achievements | 28 | REUSE | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-bidding | 255 | REUSE | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | REUSE | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | REUSE | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | REUSE | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-atf-card-table-ui | 56 | REUSE | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
| whist-lab-app-rules | 40 | REUSE | [Extent](component/whist-lab-app-rules/component-report.html) |

Total: **1521** invocări de componentă/guvernanță; toate execuțiile selectate au trecut. Grupurile reutilizate își păstrează dovezile de execuție și amprentele sursă originale.

## Selecția Android

E2E: **NOT_RUN**. Diferența de surse recalculată conține doar modificări ale politicii de release, calificării și documentației, plus metadate de versiune; contractele jocului, ecranului, interacțiunilor și persistenței nu s-au schimbat. E2E nu este raportat ca trecut.

## Proveniență și publicare

- [Receipt de execuție și reutilizare a componentelor](component/source-component-run.json)
- [Snapshot-urile originale ale componentelor](component/snapshots/)
- [Verificările de build și identitate APK](diagnostics/)
- [Verificarea autentificată a descărcării și checksum-ului APK](distribution-verification.json)
- [Release-ul GitHub publicat și inventarul activelor](../github-release.json)

[Manifestul release-ului](../release-validation.json) · [Catalogul release-urilor](../../README.md) · [Strategia de testare](../../../docs/testing/README.md)
