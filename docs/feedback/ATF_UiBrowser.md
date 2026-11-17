# Calificarea adaptorului Web

## Pe scurt

[Documentația adaptorului](../../atf-ui-browser/README.md) explică parcursul APK, JAR și Pages. Registrul urmărește comparația pe dispozitiv și acceptanța în consumatori.

## Calificarea pe același dispozitiv

| ID | Probă rămasă | Criteriu de închidere |
|---|---|---|
| WEB-07 | O partidă românească 1–8–1 cu patru locuri, de la același checkpoint în APK și Chrome pe Android. | Compară mutările legale, actorul, ultima carte și colectarea, scorul și istoricul cu ajustări. Verifică setările, Realizările, Joacă din nou și reluarea după redeschidere în RO/EN, cu text normal și mărit. |
| WEB-11 | Mâna maximă în Chrome Android la 360 × 800 și 414 × 896, cu text mărit. | Confirmă accesul la toate cele 17 cărți, inclusiv la A, K, Q, J și `10`. Compară glifele în mână, pe masă și la colectare, inclusiv în browser fără `font-variant-emoji`. |

Folosiți același profil util și documentați densitatea, scara textului, limba, versiunea browserului și hash-ul APK-ului. Comparați geometria după zonele rezervate sistemului. Înregistrați separat rotunjirea marginilor, măsurarea fonturilor și zonele interactive. [Matricea de afișaj](../testing/ATF_Display.md)

Deciziile AI, seriile și Jocul la 0 se extind prin componente cu oracol de domeniu. Capturile și interacțiunile fizice rămân în selecția vizuală. [Strategia de testare](../testing/README.md)

## Criterii de închidere

| ID | Responsabilitate | Criteriu de închidere |
|---|---|---|
| WEB-01 | Acceptanța pe browser și în jocurile consumatoare. | În Chrome și Edge pe Windows măsoară centrarea, scara și tastatura la 1280 × 720 și 1920 × 1080 la zoom 100% și 125%, plus o fereastră îngustă. În Rentz și Hold’em validează câte un flux pe JAR-ul fixat, cu oracolul jocului și legătura către propriul dosar. |

Publicați capturile native și Web inline în Extent cu distribuirea, comenzile și comparațiile. Folosiți [dovezile de componentă și Playwright](../../atf-ui-browser/README.md#verificare) pentru a selecta tranzițiile care mai cer proba pe dispozitiv.
