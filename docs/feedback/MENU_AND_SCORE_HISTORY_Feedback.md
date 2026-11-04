# Meniu și registrul complet al partidei

## Cerința curentă

Meniul și motorul păstrează numai variantele susținute de produs. Eliminarea
unei opțiuni cuprinde modelul, descrierile, fixture-urile de produs, testele și
documentația; filtrarea selectorului singură nu închide cerința.

## Criterii pentru istoric

| Situație | Rezultat cerut |
|---|---|
| O mână se încheie | Pentru fiecare loc se păstrează licitația, levatele și punctele mâinii, inclusiv seria, plus totalul rezultat. |
| Consultare în timpul jocului | Toate mâinile încheiate sunt accesibile, fără avansarea partidei. |
| Schimbarea jucătorului | Fiecare jucător poate fi urmărit pe tot istoricul, cu identitate stabilă. |
| Reluare | Înregistrările se păstrează prin salvare și reîncărcare; punctele nu se acordă din nou. |
| Salvare veche | Datele inexistente sunt marcate explicit ca necunoscute; un sold nu permite inventarea unei licitații. |
| Corupere | Dimensiunile, domeniile și coerența cu scorul final sunt validate înaintea reluării. |
| Partidă nouă | Registrul pornește gol și nu moștenește datele partidei precedente. |

## Verificarea în paralel

Motorul păstrează licitațiile pe locuri și soldurile după fiecare mână. Testele de partidă completă compară licitația și punctele mâinii cu oracolul, apoi compară licitația și totalul după restaurare.

Contractele componentelor, persistenței și interfeței se verifică separat, cu configurații și limite explicite.

- Componente: contracte exacte și ratate, scor negativ, serie +5 și +10,
  penalizare de serie, toate locurile și toate mâinile unui joc complet.
- Persistență: salvare în licitare, levată, scor și după ultima mână; migrare
  cu informații parțiale; payload invalid și reîncărcări repetate.
- UI: antete clare, licitație distinctă de levate și puncte, rânduri derulabile,
  toate valorile accesibile la font mărit și cu TalkBack.
- Raport: inventar al testelor, versiunea și sursele efective, comparații
  concrete și capturi numai pentru acțiunile Android executate.

[Istoricul scorului](../WHIST_SCORE_HISTORY.md) descrie API-ul integrat.
[Feedback UX](WHIST_LAB_User_Experience_Feedback.md) urmărește acceptanța vizuală;
[Contractul predării rezultatelor](../WHIST_UI_REPORT_HANDOFF.md) separă ultima încercare de acceptanță.
