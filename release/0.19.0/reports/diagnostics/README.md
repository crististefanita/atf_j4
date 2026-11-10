# Verificările pachetului Android

## Pe scurt

Aceste fișiere înregistrează operațiile efectuate direct asupra APK-ului candidat.

build.log arată rezultatul assembleRelease și lintVitalRelease. apk-identity.json consemnează application ID-ul, versiunea, SDK-urile, octeții și hash-ul. apksigner-verify.txt și signing-compatibility.json confirmă semnarea și compatibilitatea de upgrade; zipalign-check.log și zip-crc.json verifică structura APK-ului.

Fișierele sunt incluse individual în manifestul de integritate.