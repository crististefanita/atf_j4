# atf-console — baza comună în J3

## Pe scurt

Modulul păstrează proiecția consolei peste evenimentele neutre.
Sursele și testele au fost preluate selectiv din referința J0.
`bootstrap-provenance.json` păstrează snapshot-ul inițial; sincronizările
ulterioare sunt reconciliate separat și nu rescriu istoricul.

## Ownership și limită

Implementarea comună are owner în J0. Proiecția Console este verificată separat. Comanda
Speech emite propriile rezultate și căi UTF-8 prin stdout/stderr.
Modulul nu primește modele audio, decoder ASR sau logică de transcriere
pentru a evita ownerul Speech.

[Arhitectura J3](../docs/ATF_Speech_Architecture.md) delimitează consumerul.
[Sync-ul selectiv](../docs/ATF_Workspace_Synchronization.md) păstrează
diferențele locale și interzice mirror-ul altui deployment.
