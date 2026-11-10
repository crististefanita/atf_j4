## Descărcare

- [APK Whist Lab 0.19.1](https://github.com/crististefanita/atf_j4/releases/download/v0.19.1/WhistLab-0.19.1.apk)
- [Checksum SHA-256](https://github.com/crististefanita/atf_j4/releases/download/v0.19.1/WhistLab-0.19.1.apk.sha256)

## Îmbunătățiri față de 0.19.0

- Păstrează aceleași poziții ale jucătorilor între licitare și joc.
- Adaptează spațiul de sub jucătorul apropiat de utilizator la rezoluția și geometria mesei, pentru ca mâna de cărți să rămână vizibilă.
- Acoperă prin teste de componentă 400 de combinații de rezoluție, număr de cărți și jucători.

## Calificare

- Build-ul Release și `lintVitalRelease`: PASS.
- Opt selecții de componente: PASS; 1.157/1.157 invocări, fără FAIL/ERROR/SKIP.
- E2E nu a fost rulat; modificarea nu schimbă fluxul de utilizare.
- APK aliniat și semnat cu certificatul compatibil cu 0.19.0.

## Audit

- [Rapoarte Extent și proveniență](https://github.com/crististefanita/atf_j4/blob/master/release/0.19.1/reports/README.md)