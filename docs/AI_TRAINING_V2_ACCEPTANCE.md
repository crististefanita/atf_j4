# Runda V2 — reguli, Expert și partida Android completă

Rezultat acceptat la 2 noiembrie 2026. Dovezile sunt în
`C:\work\whist_back\ai-training\20261102-143525\`.

- 324 de teste game-core: PASS, fără erori sau skip.
- Coverage whole-core: 771/792 linii (97,35%), 849/927 ramuri (91,59%).
- Evaluare finală clasică: Expert nou 10.380 puncte, referință 10.003;
  win share 38,5 față de 30, pe 128 de comparații pereche.
- Evaluare cu seria +10: 11.074 față de 10.216 puncte;
  win share 38,5 față de 29,5, pe alte 128 de comparații pereche.
- Aceste rezultate finite nu demonstrează joc perfect. Locurile rotite pe
  același seed nu sunt distribuții independente.

## Partida normală în Whist Lab Android

Expert, patru jucători, programul românesc complet: 24 de mâini, 94 de levate,
376 de cărți. Plannerul independent vede doar mâna proprie și informația publică.
Scor final `[63,90,110,74]`: Miles câștigă. Toți cei trei adversari au depășit
plannerul în această partidă. Nu este o afirmație despre fiecare partidă posibilă.

Auditul separat a reconstruit 260 de snapshot-uri și toate mutările/scorurile.
Back/Resume și repornirea procesului au păstrat checkpoint-ul.

- Mâna 12: Miles, licitat 2, câștigat 2, bază 7, serie +10, total 59, contor resetat.
- Mâna 18: You, licitat 2, câștigat 1, bază −1, serie −5, total 37, contor resetat.
- Meniul a fost verificat pentru bonus 5, bonus 10 și serii oprite.
- Instructions din aplicația originală au fost citite complet și păstrate ca text
  și capturi. Acest run nu pretinde o partidă nouă completă în aplicația originală.

## Raport nativ Extent

`whist-ai-extent-report.html`: 349 de cazuri acceptate, 30.028 de comparații
(14.267 UI și 15.761 de componentă), 352 de checkpoint-uri cu capturi.
Toate PNG-urile au fost verificate după hash în HTML și sunt inline Base64.
Deduplicarea nativă produce 236 PNG data URI, cu 234 de conținuturi distincte.

Raportul istoric folosește Core C3 / Reporting J13, versiunile înghețate în
acel staging. J4 consumă acum J15 în root; nu relabelăm raportul deja executat.
Problema capturilor pentru pași observaționali fără aserțiuni a fost rezolvată
în consumator cu API-ul public `uiValidationAsMedia`; feedback-ul exact este
în folderul de feedback J0. Nu s-a modificat implementarea bibliotecii.

`ai-report-verification.properties` este PASS; HTML-ul are 62.021.773 de bytes,
SHA-256 `9830a0a57f92b55d6de537553f840b93c9d946cbb0ba9d0500c036530bc656b1`.
Run-ul păstrează APK-ul testat, 290 de surse cu hash, arhiva sursă, versiunile
bibliotecilor și experimentele respinse.

Partida poate fi înțeleasă și revalidată fără UI în
`C:\work\whist_back\training-corpus\games\development\android-round-v2-21049369348797.json`.
Vezi [corpusul offline](AI_CORPUS.md) pentru format și comenzi.
