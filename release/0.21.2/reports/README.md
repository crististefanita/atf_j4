# Dovezile Whist Lab 0.21.2

## Pe scurt

Acest dosar păstrează verificările legate de APK-ul publicat și proveniența selecțiilor de componentă. Nu s-a executat un flux Android E2E în această calificare.

## Suite de componentă

Fiecare selecție are un raport Extent HTML, receipt-ul său și o proveniență verificabilă. Capturile sunt inline în HTML; XML-urile JUnit au fost folosite doar în staging și nu sunt duplicate în dosarul final.

| Selecție | Invocări | Raport |
|---|---:|---|
| atf-validation | 19 | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 259 | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 348 | [Extent canonic](component/atf-tests/component-report.html) |
| whist-lab-app-achievements | 27 | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-bidding | 253 | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 116 | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 99 | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-atf-card-table-ui | 34 | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |

Total: **1.216 de invocări unice**, toate cu rezultat PASS. [Indexul selecțiilor](component/README.md) explică structura și proveniența fiecărui raport.

## Descărcare și identitate

[Receipt-ul Browser MCP](distribution-verification.json) confirmă finalizarea descărcării autentificate, dimensiunea, SHA-256 și CRC-ul APK-ului. [Verdictul RAG înainte de publicare](rag-prepublication.json) păstrează porțile și citările candidatului.

## Proveniență

[Catalogul livrărilor](../../README.md) · [Manifestul versiunii](../release-validation.json) · [Strategia de testare](../../../docs/testing/README.md) · [Ghidul de livrare Android](../../../docs/platform/ATF_AndroidDelivery.md)
