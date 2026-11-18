# Semnarea ediției Android de test

## Pe scurt

Certificatul public Whist și amprenta sa SHA-256 identifică semnatarul ediției de test. Comparați amprenta cu `apksigner verify --print-certs` înainte de calificarea unui upgrade.

Keystore-ul privat rămâne local și ignorat de Git. [Ghidul de livrare Android](../docs/platform/ATF_AndroidDelivery.md) explică semnarea și identitatea pentru upgrade.

## Calificarea surselor

| Intrare | Regula de export |
|---|---|
| `.gitignore`, `README.md`, `whist-android-test.pem`, `whist-android-test.fingerprint` | Contract public de semnare: inventariat, amprentat și copiat. |
| Alte intrări din `.atf-signing` | Material privat local: exclus înainte de citirea conținutului sau traversarea directoarelor. |
| Surse din afara `.atf-signing` | Se aplică regulile normale pentru surse; această limită nu este un detector general de secrete. |

- [Politica comună a surselor](../scripts/AtfSigningSourcePolicy.ps1) ține cheile locale și copiile lor în afara snapshot-urilor sursă, rapoartelor și fișierelor de release.
- Cheia locală poate fi prezentă, absentă sau rotită fără să schimbe identitatea surselor. Modificarea metadatelor publice sau a altui fișier sursă schimbă acea identitate.
- Copierea verifică întregul manifest înainte de scriere și refuză căile private, aliasurile canonice și căile indirecte. Semnarea folosește cheia locală explicit; calificarea nu o șterge și nu o modifică.
