# Testele contractelor de validare

## Pe scurt

Modulul confruntă răspunsul observat cu un contract declarat, păstrând diferențele și cauza originală. [Strategia comună](../../../docs/testing/README.md) separă politica pură de integrarea cu documentele și raportarea.

## Proprietăți verificate

| Familie | Oracol și frontieră |
|---|---|
| Egalitate și rezultat | Tipul valorii, diferența observată și verdictul politicii. |
| Contractul răspunsului | Document JSON controlat, câmpuri obligatorii și mesaje localizate. |
| Schemă | Capabilitățile declarate și refuzul regulilor neacceptate, fără a transforma lipsa suportului în succes. |
| Dovezi | Preview, redactare și păstrarea valorilor necesare explicării eșecului. |

## Execuție și limite

[Catalogul](../../../config/atf-test-suites.json) separă `ValidationUnitSuite` de `ValidationComponentSuite`. [Lansatorul comun](../../../scripts/run-component-suites.ps1) publică numai selecția de componentă; unitățile păstrează intrarea și rezultatul lor distincte.

- Folosiți [intrarea modulului](../../README.md) într-o copie temporară cu dependențele identificate.
- Probele negative modifică o singură condiție relevantă și păstrează martorul valid; un defect al fixture-ului nu demonstrează refuzul cerut.

Testele de raportare verifică datele transmise, separat de acceptanța rendererului. [Bugetul evaluării](../../../docs/feedback/ATF_TestInfrastructure.md#costul-validării-api) rămâne o calificare distinctă de corectitudinea funcțională.

[Validarea API](../../README.md) · [Strategia comună](../../../docs/testing/README.md)
