# Whist Lab 0.19.1 — aplicație Android

## Pe scurt

Această ediție ajustează dinamic spațiul mesei pentru ca mâna utilizatorului să rămână vizibilă la rezoluții și configurații diferite. Locurile jucătorilor rămân stabile între licitare și joc.

## Descărcare și instalare

- [Descarcă APK-ul 0.19.1](https://github.com/crististefanita/atf_j4/releases/download/v0.19.1/WhistLab-0.19.1.apk)
- [Pagina release-ului v0.19.1](https://github.com/crististefanita/atf_j4/releases/tag/v0.19.1)

Pentru instalare pe Android cu ADB:

    adb install -r WhistLab-0.19.1.apk

## Schimbări

- Păstrează pozițiile mesei între fazele de licitare și joc.
- Adaptează spațiul inferior la înălțimea și geometria disponibile, fără să acopere cărțile utilizatorului.
- Verifică 400 de combinații de rezoluție, număr de cărți și jucători prin teste de componentă.
- Nu schimbă regulile jocului, scorul sau semantica acțiunilor.

## Calificare

- Build Release și `lintVitalRelease`: PASS.
- Opt selecții de componente: PASS, 1.157/1.157 invocări.
- E2E nu a fost rulat; raportarea nu declară aceste fluxuri ca PASS.

## Proveniență

[Raportare Extent, receipt-uri și diagnostic APK](reports/README.md) · [Catalogul release-urilor](../README.md)