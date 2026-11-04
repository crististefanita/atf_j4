# Cerințe pentru dovezile Android și de componentă

Cititorul trebuie să poată reconstitui din HTML de ce a trecut sau a eșuat o verificare. Prioritățile de mai jos sunt criterii de acceptare la schimbarea unui producător de dovezi.

| Prioritate | Defect de prevenit | Probă de închidere |
|---|---|---|
| P0 | Un test eșuat devine verde la publicare. | Un negativ deliberat păstrează verdictul, comparația divergentă și identitatea invocării în raportul final. |
| P0 | Un tap reușit este prezentat ca mutare acceptată. | Receipt-ul compară cartea sau licitația selectată cu exact o tranziție semantică observată. |
| P0 | Un scor final corect ascunde o mână atribuită locului greșit. | Pentru fiecare mână se compară locul, licitația, levatele, punctajul și totalul cu replay-ul independent. |
| P1 | Comprimarea ascunde pierderi sau trunchiază o dovadă. | Intrarea păstrează toate eșecurile și invocări întregi. Inventarul delimitează populația și selecția. |
| P1 | Captura nu identifică starea și acțiunea relevante. | Observația leagă captura integrală de checkpoint și păstrează octeții PNG în HTML. |
| P1 | O execuție de layout este citită ca partidă completă. | Pointerul include scenariul, versiunea aplicației, mâinile încheiate și limitele vizuale. |
| P1 | O republicare apare ca o nouă execuție de joc. | Se păstrează distinct timpii capturării, compoziției și auditului browser. |

[Responsabilități](README.md) · [Politica de detaliu](../ATF_Game_Report_Presentation.md) · [Rapoarte și limite](../reporting/README.md) · [Dovezi istorice](../history/README.md)
