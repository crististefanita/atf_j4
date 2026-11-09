# Catalogul livrărilor

## Pe scurt

Artefactele active însoțesc un singur APK. Build-urile și încercările intermediare rămân în TMP sau în arhiva externă.

## Versiunea curentă

[0.17.1 — build, verificări și proveniență](0.17.1/README.md) este ediția curentă. Dosarul păstrează identitatea APK-ului și dovezile legate de versiunea calificată.

## Versiuni disponibile

| Versiune | Stare | Dovezi |
|---|---|---|
| 0.17.1 | Android TEST; pregătit local | [Dosarul și raportarea](0.17.1/README.md) |

## Cum publicăm o versiune

1. Pregătiți candidatul, construiți APK-ul semnat și verificați identitatea lui înainte de publicare. [Calificarea release-ului](../docs/testing/ATF_ReleaseQualification.md)
2. Alegeți canalul publicat și înregistrați-l explicit: `PUBLISHED` cu `AUTHENTICATED` pentru asset GitHub accesibil conturilor autorizate sau `PUBLISHED_SITE` cu `ANONYMOUS` pentru Site public. Nu declarați un canal pe baza celuilalt.
3. Descărcați APK-ul în modul declarat de manifest: cu un cont autorizat pentru asset-ul GitHub sau fără autentificare pentru copia Site publică. Confruntați HTTP, numărul de octeți, SHA-256 și CRC cu manifestul versiunii. [Ghidul livrării Android](../docs/platform/ATF_AndroidDelivery.md) · [Verificarea accesului](../docs/platform/ATF_AndroidDelivery.md#accesul-anonim-și-ruta-publică)

## Citire rapidă

1. Deschideți dosarul versiunii curente și verificați scopul, APK-ul și sursele identificate.
2. Urmați raportul HTML principal; README-ul dosarului explică selecțiile și controalele efectuate asupra raportului.
3. Consultați inventarul pentru rezultatele native încă nemigrate și diagnosticele cu scop separat. Un total JUnit nu reconstruiește comparațiile necapturate.

Catalogul arată ultima calificare păstrată. [Pregătirea următorului candidat](../docs/feedback/README.md#registre-active) și codul live au identități proprii; dezvoltarea ulterioară nu moștenește verdictul acestui APK.

[Strategia comună](../docs/testing/README.md) · [Retenție și publicare](../docs/platform/ATF_ReleaseRetention.md)
