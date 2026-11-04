# Istoricul partidei și calificarea interfeței

## Responsabilitate

Motorul deține datele partidei, iar aplicația Android deține persistența și prezentarea lor. Modificările interfeței se califică pe binarul efectiv instalat, cu starea și scopul scenariului declarate.

## Diferență de contract: levatele istorice

[ScoreHistory](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/ScoreHistory.java) păstrează licitațiile și soldurile după mâini; punctele se derivă din diferența soldurilor. Levatele fiecărui loc nu sunt stocate separat în acest registru.

| Prioritate | Cerință rămasă | Criteriu de închidere |
|---|---|---|
| P1 | Istoricul complet solicitat include și levatele câștigate. | Definiți schema și migrarea, păstrați informația necunoscută în salvările vechi și verificați asocierea loc–mână–licitat–levate–puncte–total. |

Un sold sau o diferență de puncte nu justifică reconstruirea unor levate lipsă. Contractul implementat și limita migrării rămân în [modelul istoricului](../game/ATF_ScoreHistory.md).

## Calificarea modificărilor vizuale

| Prioritate | Suprafață | Verificare necesară înainte de acceptare |
|---|---|---|
| P1 | Masă, carte și atu | Profiluri declarate, text mărit, intersecții reale, coordonate logice mapate la raster și revizie a capturii originale. |
| P1 | Istoric și reluare | Toate mâinile încheiate și toate locurile accesibile, fără acordarea repetată a punctelor ori avansarea partidei. |
| P1 | Accesibilitate și localizare | Ordine asistată, etichete ale valorilor necunoscute ori negative, primul și ultimul element derulabil în ambele limbi. |

O schimbare de geometrie sau stil se leagă de sursele și APK-ul inspectate. Calificarea pe dispozitiv și auditul componentelor sunt înregistrări distincte, păstrate extern.

## Criterii de verificare

- Componente: contract exact sau ratat, scor negativ, serii configurate, toate locurile și tranzițiile istoricului.
- Persistență: licitare, levată, închiderea mâinii, sfârșitul partidei, date parțiale și date corupte.
- Interfață: rang și culoare lizibile, ținte accesibile, roluri semantice distincte și revenire cu starea păstrată.
- Raport: acțiune, observație, comparație și captură asociate aceleiași stări.

[Testarea Android](../testing/ATF_Android.md) · [Profiluri de ecran](../testing/ATF_Display.md) · [Atlasul interfeței](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md)
