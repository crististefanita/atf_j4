# Semnarea ediției Android de test

## Pe scurt

Certificatul public Whist și amprenta SHA-256 identifică semnatarul ediției de test. Comparați amprenta cu rezultatul `apksigner verify --print-certs` înainte de a califica un upgrade.

Keystore-ul utilizat la semnare se păstrează local. Folderul permite în Git doar documentația, certificatul și amprenta, pentru ca un keystore copiat aici să nu intre accidental în index. [Ghidul de livrare Android](../docs/platform/ATF_AndroidDelivery.md)

Calificarea exportă numai `.gitignore`, `README.md`, certificatul public `whist-android-test.pem` și amprenta sa publică din `.atf-signing`. [Politica comună a surselor](../scripts/AtfSigningSourcePolicy.ps1) exclude toate celelalte intrări din acel director local dedicat înainte de enumerare sau calcularea hash-urilor. Păstrați keystore-ul privat și copia sa protejată în afara snapshot-urilor sursă, rapoartelor și fișierelor de release. Această limită nu este un detector general de secrete pentru alte directoare sursă.

Cheia locală poate fi prezentă, absentă sau rotită fără să schimbe identitatea surselor. Schimbarea certificatului public, a amprentei publice sau a altui fișier sursă schimbă acea identitate. Copierea verifică manifestul complet înainte de scriere: căile private de semnare, aliasurile canonice și căile indirecte sunt refuzate. Semnarea folosește în continuare cheia locală explicit; calificarea nu o șterge și nu o modifică.
