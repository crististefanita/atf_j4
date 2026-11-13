# Dovezile Whist Lab 0.21.3

## Pe scurt

Acest dosar păstrează dovezile care califică APK-ul publicat și proveniența selecțiilor de componentă. E2E nu a fost executat deoarece sursele de produs și fluxurile observabile nu s-au schimbat.

## Suite de componentă

Fiecare selecție are un raport Extent HTML, un receipt propriu și o proveniență verificabilă. Capturile sunt inline în HTML; XML-urile JUnit au fost folosite doar ca oracle temporar și nu sunt duplicate în release.

| Selecție | Invocări | Raport |
|---|---:|---|
| atf-validation | 19 | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 259 | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 361 | [Extent canonic](component/atf-tests/component-report.html) |
| whist-lab-app-achievements | 27 | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-bidding | 253 | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 116 | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 99 | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-atf-card-table-ui | 34 | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |

Total: **1.229 de invocări unice**, toate cu rezultat PASS. [Indexul selecțiilor](component/README.md) explică rapoartele și refolosirea dovezilor neschimbate.

## Distribuție și identitate

[Verificarea descărcării autentificate](distribution-verification.json) confirmă dimensiunea, SHA-256 și CRC-ul APK-ului după publicare. Verdictul RAG pre-publicare este păstrat separat de raportul post-publicare.

## Proveniență

[Catalogul livrărilor](../../README.md) · [Manifestul versiunii](../release-validation.json) · [Strategia de testare](../../../docs/testing/README.md) · [Ghidul Android](../../../docs/platform/ATF_AndroidDelivery.md)