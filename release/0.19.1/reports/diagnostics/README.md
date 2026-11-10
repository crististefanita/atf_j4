# Verificările pachetului Android

## Pe scurt

Fișierele înregistrează operațiile executate direct asupra APK-ului candidat.

`build.log` arată `assembleRelease` și `lintVitalRelease`. `apk-identity.json` consemnează application ID-ul, versiunea, SDK-urile, octeții și hash-ul. `apksigner-verify.txt` și `signing-compatibility.json` confirmă semnarea și compatibilitatea de upgrade; `zipalign-check.log` și `zip-crc.json` verifică structura APK-ului.
