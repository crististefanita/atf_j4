# atf-mock — baza comună în J3

## Pe scurt

Modulul păstrează transport API determinist pentru adaptoare care au nevoie de el.
Sursele și testele au fost preluate selectiv din referința J0.
`bootstrap-provenance.json` păstrează snapshot-ul inițial; sincronizările
ulterioare sunt reconciliate separat și nu rescriu istoricul.

## Ownership și limită

Implementarea comună are owner în J0. Transportul MOCK este verificat separat;
recognizerul real și fixture-urile speech nu sunt implementate prin acest transport.
Modulul nu primește modele audio, decoder ASR sau logică de transcriere
pentru a evita ownerul Speech.

[Arhitectura J3](../docs/ATF_Speech_Architecture.md) delimitează consumerul.
[Sync-ul selectiv](../docs/ATF_Workspace_Synchronization.md) păstrează
diferențele locale și interzice mirror-ul altui deployment.

## JSON strict și potrivire numerică exactă

Catalogul și request-ul sunt documente complete. Trailing content, rădăcini
multiple și duplicate nested sunt refuzate; scalarul null este JSON valid.
Un expected absent în YAML nu echivalează cu textul JSON null, iar obiectele
sau array-urile YAML nu sunt convertite tacit în absența constrângerii.

| Potrivire | Contract verificat |
|---|---|
| precizie | JSON și scalarul numeric YAML folosesc BigDecimal, fără narrowing prin Double |
| valori distincte | vecinii peste 2^53, fracțiile lungi, exponenții extremi și underflow-ul ajung la fallback |
| nominale | formele decimal egale și zerourile rămân eligibile; integer/decimal și număr/string păstrează distincțiile |
| wire | body-ul și răspunsul configurat nu sunt rescrise |

[Contractele de componentă](src/test/java/atf/mock/json/MockNumericContractComponentTest.java)
folosesc API-ul public, expected independent și transportul mock. Ele verifică
fallback-ul, răspunsul original și încărcarea YAML, fără serviciu extern.
