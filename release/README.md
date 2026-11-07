# Catalogul livrărilor

## Pe scurt

Artefactele active însoțesc un singur APK. Build-urile și încercările intermediare rămân în TMP sau în arhiva externă.

## Versiunea curentă

[0.13.0 — build, verificări și proveniență](0.13.0/README.md) este publicată cu tag-ul `v0.13.0`. Descărcarea anonimă GitHub a răspuns 404; linkul Site public este verificat anonim și este ruta recomandată pentru APK.

## Versiuni disponibile

| Versiune | Stare | Dovezi |
|---|---|---|
| 0.13.0 | Android publicat; ținta iOS necalificată pe simulator | [Dosarul și raportarea](0.13.0/README.md) |
| 0.12.0 | PUBLISHED | [Raport Extent, rezultate și identitate](0.12.0/README.md) |

## Cum publicăm o versiune

1. Pregătiți candidatul, construiți APK-ul semnat și verificați identitatea lui înainte de publicare. [Calificarea release-ului](../docs/testing/ATF_ReleaseQualification.md)
2. Asociați tag-ul commitului sursă. Atașați APK-ul și checksum-ul la draft, verificați-le și publicați release-ul. [Ghidul livrării Android](../docs/platform/ATF_AndroidDelivery.md)
3. Deschideți README-ul publicat fără cont și descărcați APK-ul din linkul său. Confruntați HTTP, mărimea și SHA-256 cu dosarul versiunii. Dacă GitHub răspunde cu 404, urmați pașii de diagnostic și verificați separat ruta publică. [Verificarea accesului anonim](../docs/platform/ATF_AndroidDelivery.md#accesul-anonim-și-ruta-publică)

## Citire rapidă

1. Deschideți dosarul versiunii curente și verificați scopul, APK-ul și sursele identificate.
2. Urmați raportul HTML principal; README-ul dosarului explică selecțiile și controalele efectuate asupra raportului.
3. Consultați inventarul pentru rezultatele native încă nemigrate și diagnosticele cu scop separat. Un total JUnit nu reconstruiește comparațiile necapturate.

Catalogul arată ultima calificare păstrată. [Pregătirea următorului candidat](../docs/feedback/README.md#registre-active) și codul live au identități proprii; dezvoltarea ulterioară nu moștenește verdictul acestui APK.

[Strategia comună](../docs/testing/README.md) · [Retenție și publicare](../docs/platform/ATF_ReleaseRetention.md)
