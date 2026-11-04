# Testarea locală a aplicațiilor Android

## Pe scurt

Aplicația de referință este Contract Whist (`com.qsoft.android.whist`), iar
aplicația dezvoltată aici este Whist Lab (`ro.whistlab.app`). Fiecare are propriul
profil, propriile salvări și propria acceptanță.

## Puncte de intrare

| Activitate | Document |
|---|---|
| Conectivitate, dispozitiv și runner | [Ghidul Android](ANDROID_TESTING.md) |
| Teste, suita de componente și oracole | [Modulul Android](../atf-test-android/README.md) |
| Build și persistență Whist Lab | [Aplicația](../whist-lab-app/README.md) |
| Profilurile de reguli | [Contractele jocului](CONTRACT_WHIST_RULE_RECONSTRUCTION.md) |
| Partidă românească în original | [Runnerul dedicat](ORIGINAL_ROMANIAN_HARD_LIVE.md) |
| Ultimul raport și limitele lui | [Raportarea](reporting/README.md) |

## Delimitarea dovezilor

- Componentele pot verifica regulile și replay-ul unei partide fără emulator.
- Compilarea unui flux UI nu certifică selectorii, geometria sau inputul Android.
- Un tap ADB confirmă transportul; receipt-ul confirmă cartea sau licitația
  acceptată efectiv de aplicație.
- Un eveniment raster nu identifică singur rangul, culoarea sau legalitatea
  cărții jucate.
- Capturile și rezultatele istorice sunt în [arhivă](history/README.md), legate
  de versiunea și configurația de la execuție.
