# Uneltele infrastructurii Android

## Pe scurt

Uneltele aparțin infrastructurii Android. Modulul PowerShell verifică transportul ADB, iar uneltele Python consumă date capturate explicit. Replay-ul verifică regulile; predarea leagă descriptorul de rezultate și menține pointerele externe pe scop.

## Responsabilități

| Sursă | Contract |
|---|---|
| [AndroidDeviceSetup.psm1](AndroidDeviceSetup.psm1) | Conectarea ADB și verificarea disponibilității transportului, apelate de lansatoarele comune. |
| [whist_ui_replay.py](whist_ui_replay.py) | Proiecția observației publice și verificarea independentă a regulilor și scorului. |
| [whist_ui_handoff.py](whist_ui_handoff.py) | Republicarea offline, integritatea descriptorului și acceptarea pe scop. |
| [test_whist_ui_handoff.py](test_whist_ui_handoff.py) | Refuzul coruperii și păstrarea acceptanței la eșecuri ori republicări vechi. |

## Verificare

Din rădăcina proiectului, cu un interpretor Python 3 real disponibil:

```powershell
python -m unittest discover -s atf-test-android/tools -p test_whist_ui_handoff.py
```

Importurile Python se rezolvă între fișierele aceluiași director. Rezultatele și pointerele sunt externe proiectului; o republicare păstrează identitatea execuției originale.

[Predarea rezultatelor](../../docs/reporting/ATF_Handoff.md) · [Auditul publicării](../../docs/reporting/ATF_Audit.md) · [Modulul Android](../README.md)
