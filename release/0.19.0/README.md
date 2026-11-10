# Whist Lab 0.19.0 — aplicație Android

## Pe scurt

Această versiune păstrează pozițiile jucătorilor la masă între etapele de împărțire și licitare și aduce opțiunea Auto Play în antetul jocului.

## Descărcare și instalare

APK-ul este publicat ca asset GitHub pentru conturile autentificate care au acces la repository. Folosiți butonul de descărcare din [README-ul principal](https://github.com/crististefanita/atf_j4) sau pagina [release-ului v0.19.0](https://github.com/crististefanita/atf_j4/releases/tag/v0.19.0).

Pentru instalare pe Android cu ADB:

    adb install -r WhistLab-0.19.0.apk

## Schimbări

- Păstrează geometria centrală a mesei stabilă între împărțire, licitare și joc.
- Așază bifa Auto Play lângă acțiunea de licitare, în antetul mesei.
- Adaptează poziționarea la numărul de jucători, cărți și spațiul disponibil.
- Nu schimbă regulile, punctajul sau semantica acțiunilor.

## Calificare

- Build-ul Release și lintVitalRelease: PASS.
- APK-ul a fost aliniat și semnat cu certificatul TEST compatibil cu versiunea anterioară.
- Suitele primare de componentă sunt documentate prin rapoarte Extent și receipt-uri în [dosarul de rapoarte](reports/README.md).
- E2E nu a fost rulat pentru această livrare; calificarea nu certifică fluxuri pe dispozitiv.

## Proveniență și audit

Manifestul release-validation.json leagă APK-ul, sursa, receipt-urile, rapoartele și hash-urile fișierelor livrate. [Rapoartele de componentă](reports/component/README.md) sunt canonice; JUnit este folosit doar ca verificare independentă temporară.
