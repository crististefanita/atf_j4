# Semnarea ediției Android de test

## Pe scurt

Certificatul public Whist și amprenta SHA-256 identifică semnatarul ediției de test. Comparați amprenta cu rezultatul `apksigner verify --print-certs` înainte de a califica un upgrade.

Keystore-ul utilizat la semnare se păstrează local. Folderul permite în Git doar documentația, certificatul și amprenta, pentru ca un keystore copiat aici să nu intre accidental în index. [Ghidul de livrare Android](../docs/platform/ATF_AndroidDelivery.md)
