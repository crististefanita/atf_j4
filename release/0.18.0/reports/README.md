# Dovezile Whist Lab 0.18.0

## Pe scurt

Acest index descrie rapoartele canonice și proveniența dovezilor versiunii.

## Suita de componente

Opt suite Extent au raportat 1.150 de teste promovate, fără eșecuri, erori sau teste omise.
Receipt-urile leagă fiecare raport de aceeași amprentă a sursei folosită pentru build.

## Android E2E

Testul pe APK-ul semnat a trecut 1/1. Verifică identitatea APK-ului și fluxul catalogului local de realizări. Raportul Extent păstrează capturile PNG inline Base64, iar receipt-ul leagă raportul de commit, sursă și hash-ul APK-ului.

## Restaurarea mediului

Datele private existente ale emulatorului au fost salvate și restaurate după verificare; inventarul fișierelor și preferințelor a corespuns. Emulatorul a fost eliberat. Arhivele temporare private nu fac parte din payload.

## Descărcarea din GitHub

Asset-ul APK a fost descărcat cu un cont GitHub autentificat prin acceptance-ul de distribuție. Receipt-ul confirmă HTTP 200, 955.769 bytes, SHA-256 egal cu manifestul și CRC ZIP valid. [Raportul Extent](download-extent.html) păstrează comparațiile; checksum-ul atașat release-ului se verifică separat.

## Organizarea dovezilor

Rapoartele HTML sunt dovezile canonice. Fiecare suită are un singur raport și receipt.

- XML-ul JUnit, capturile PNG și rezultatele intermediare nu sunt duplicate aici.
- Raportul E2E este separat în `e2e/signed-release-achievements/`.
- [Receipt-ul de distribuție](distribution-verification.json) și [metadatele release-ului GitHub](../github-release.json) leagă publicarea de tag și commitul calificat.

[README-ul release-ului](../../README.md) · [Strategia comună de testare](../../../docs/testing/README.md)
