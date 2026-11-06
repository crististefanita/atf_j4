# Testele laboratorului independent

## Pe scurt

Laboratorul califică jucătorul independent și mecanismele de evaluare offline. [Strategia comună](../../../../docs/testing/README.md) deține nivelul verificării; integrarea cu motorul și cercetarea pe corpusuri au domenii separate.

## Frontiere și intrări

| Frontieră | Verificare responsabilă |
|---|---|
| Contractul local Java | Tipuri, stări și intrări generate legal, fără dependență de arhiva partidelor. |
| Unelte Node.js | Corpus, rezervare, selecție și compoziție, cu fixture-uri controlate lângă unealta responsabilă. |
| Integrarea cu motorul | Testele motorului compară observația și acțiunile legale prin adaptor. |
| Evaluare strategică | Protocol separat, surse înghețate și loturi rezervate; rezultatul concret rămâne extern. |

## Execuție și interpretare

[Gradle](../../build.gradle) deține sarcina `check` și intrările locale, iar [ghidul laboratorului](../../README.md) descrie precondițiile. [Uneltele](../../tools/README.md) au contracte și teste proprii; experimentele păstrate separat nu intră implicit în politica activă.

O probă locală validează contractul exercitat, fără a demonstra superioritatea jucătorului. Corpusul și evaluarea rezervată se selectează explicit, cu identitatea și expunerea datelor păstrate.

[Laboratorul AI](../../README.md) · [Strategia comună](../../../../docs/testing/README.md) · [Protocolul offline](../../../../docs/learning/README.md)
