# Așteptări de acceptanță pentru rapoartele Whist Lab

Acest feedback aparține producerului de teste al jocului. Defectele
neutre ale contractului HTML, Extent sau fallback-ului aparțin
modulului comun de raportare, prin [indexul de feedback](README.md).

| Prioritate | Risc specific jocului | Probă cerută |
|---|---|---|
| P0 | Un tap este confundat cu o mutare acceptată. | Receipt-ul compară controlul selectat cu exact o schimbare semantică observată în starea Whist Lab. |
| P0 | Scorul final ascunde o mână sau un jucător atribuit greșit. | Replay-ul compară pe fiecare mână jucătorul, licitația, levatele, punctele și totalul cu oracolul independent. |
| P1 | Un sanity de ecran este interpretat ca partidă completă. | Raportul numește scenariul, mâinile efectiv jucate și limitele UI observate. |
| P1 | Captura nu aparține stării verificate a jocului. | E2E-ul leagă PNG-ul integral, acțiunea trimisă, observația după acțiune și aserțiunile aceluiași checkpoint. |

Testele curente păstrează expected/actual și verdictul real. PNG-urile sunt
capturi live; fixture-urile de componentă nu sunt prezentate ca UI executat.
Un rezultat bun la build sau la componentă nu certifică elementele vizibile,
iar un raport nou nu repară retroactiv un eșec anterior.

[Contractul consumerului](../ATF_Android_Report_Contract.md) ·
[Suitea multi-profile Whist Lab](../WHIST_RESPONSIVE_UI_SANITY.md) ·
[Feedback-ul de UX](WHIST_LAB_User_Experience_Feedback.md)