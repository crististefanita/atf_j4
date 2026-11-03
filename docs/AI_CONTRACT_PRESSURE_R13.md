# Hard — necesar, informație publică și contractele mesei

Această iterație este exclusiv offline. Interfața, Android și biblioteca Reporting nu sunt modificate. Protocolul și toate partidele, inclusiv pierderile, sunt externe: C:/work/whist_back/ai-training/20261103-r13-pressure.

## Două diferențe distincte

**Necesarul propriu** este licitația minus levatele deja câștigate. **Oportunitățile rămase** includ levata curentă chiar dacă unele cărți sunt deja pe masă: (toate cărțile rămase în mâini + cărțile de pe masă) / numărul de jucători. Nu numărăm de două ori levata incompletă.

**Diferența mesei** este suma necesarelor semnate ale tuturor jucătorilor minus oportunitățile rămase. O valoare pozitivă înseamnă competiție pentru mai multe levate decât există. O valoare negativă înseamnă levate pe care masa încearcă să le evite. Separat, diferența dintre necesarul propriu și câștigătorii plauzibili ai mâinii arată rezerva sau lipsa de control; estimarea nu este o dovadă.

| Situație | Direcție strategică | Limită |
|---|---|---|
| Mai trebuie toate levatele | Ia levata curentă dacă este posibil; nu păstra o carte mare care nu mai poate ajuta ulterior | Respectă întâi culoarea/atuul obligatoriu |
| Mai trebuie una din trei și un atu sigur major poate fi păstrat | Nu consuma contractul într-o levată suplimentară riscantă; protejează rezerva | Contează legalitatea păstrării și celelalte cărți forțate |
| Contractul este deja realizat | Evită levate noi; scapă de cărți cu risc viitor mare când poți pierde sigur acum | Cea mai mică carte nu este automat cel mai sigur discard |
| Necesar negativ sau mai mare decât oportunitățile | Contractul este irecuperabil | Nu îl eticheta recuperabil printr-o estimare optimistă |
| Contract propriu irecuperabil, penalizare proprie egală | Preferă mai mulți adversari ratați; apoi limitează scorul celui mai puternic rival | Nu sacrifica un contract propriu încă recuperabil |

## Memoria publică și cartea întoarsă

Cărțile proprii, istoricul tuturor jocurilor din mâna curentă, levata curentă și cartea întoarsă sunt indisponibile pentru mâinile adverse. Cărțile publice sunt actualizate după fiecare acțiune simulată. Un atu întors nu este atribuit fictiv unui adversar. Lipsa demonstrată de culoare este o constrângere; în Whist românesc, un discard fără culoare și fără atu dovedește și lipsa atuului. Licitația este informație comportamentală, nu dovadă de lipsă sau prezență a unei cărți.

Un K de atu se promovează după ce A este public indisponibil; o Q necesită eliminarea ambelor ranguri superioare. Un as lateral poate încă fi tăiat. Numărul cărților necunoscute nu este identic cu numărul cărților efectiv împărțite: în mâinile scurte există și cărți neîmpărțite.

## Ponderi și statutul lor

[Manifestul YAML](../config/whist-contract-pressure.yaml) leagă regulile, contextul, constantele și testele. Este un manifest verificabil al codului, nu un interpretor de strategie: editarea YAML singură nu schimbă motorul. Ponderile sunt propuneri de ranking, nu probabilități calibrate. Influența diferenței mesei crește în interiorul mâinii pe măsură ce scad oportunitățile; nu este confundată cu numărul de mâini deja terminate în partidă.

Modelul adversarului separă un câștig voluntar de unul forțat: un adversar care și-a făcut contractul încearcă să piardă, dar poate fi obligat să câștige dacă toate răspunsurile legale sunt prea mari. Produsul marginalelor pe jucători ignoră corelațiile între mâini. Nu este prezentat drept probabilitate exactă comună. Cărțile private reale ale adversarilor nu sunt intrare pentru decizie.

## Acceptanță

Protocol schema12, doar Hard/expert, adversari Expert R11 și independent R11, patru rotații. Development: seed13200–13207. Evaluare nouă rezervată înainte de citire:112000–112015. Acceptarea cere componentele și criteriul nemodificat de non-regresie pe familii plus câștig strict de contracte exacte la2–8. Citirea evaluării nu autorizează reajustarea pragurilor sau ponderilor. Rezultatele sunt completate după execuție; până atunci candidatul nu este activat.

## Rezultate închise: ponderile rămân inactive

| Variantă | Lot | Puncte / victorii ponderate / exacte2–8 | Decizie |
|---|---|---|---|
| Presiune în toate continuările |64 perechi /128 partide development|−48 /−5,5 /+1|Respins|
| Mix conservator numai la jocul cărților |64 perechi /128 partide development|−92 /−3,5 /−4|Respins|
| Același mix înghețat |128 perechi /256 partide noi|−178 /+3,5 /−16|Respins|

Creșterea numărului de victorii în ultimul lot nu ascunde cele16 contracte exacte pierdute și cele178 de puncte pierdute. Nu s-au ajustat ponderile după evaluare. Lotul112000–112015 este consumat și nu intră în învățare. Prima variantă a avut495 teste cu8 diferențe față de contractele înghețate. Mixul a avut inițial496/2; originalele sunt păstrate. Corectarea a două aserțiuni de identitate a unui estimator în teste, fără schimbări de producție, și patru controale suplimentare duc calificarea candidatului la500/0. Aceasta nu îl promovează.

Cele14 componente ale modelului respins sunt acum numai în colecția opt-in research, alături de cele7 contraexemple R12:21 în total. Catalogul YAML declară influență runtime0. Nu a fost modificat motorul validat pentru a integra aceste ponderi. Două teste istorice corectate numai în stagingul candidatului nu sunt impuse regresiei active.

Colecția nouă de învățare păstrează12 poziții publice, într-un fișier de50.458 bytes, selectate determinist după regim, semnul diferenței mesei și prezența unui atu major. Acțiunea observată și rezultatul ulterior sunt separate; nu devin etichete de mutare optimă. Setul activ anterior de100 de partide rămâne neschimbat; nu se adaugă toate rulările în regresia zilnică.

Corecția exactă de final este un experiment de funcționalitate separat, descris în [salvarea certificată R14](AI_CONTRACT_SALVAGE_R14.md). Ea păstrează toate ponderile de mai sus inactive și nu relaxează gate-ul de forță R13.

## Raport și dovezi finale

[Extent J18 final](C:/work/whist_back/ai-training/20261103-r13-pressure/reports/final-extent-report.html):27 capturi, 1239 comparații, 4492251 bytes. Inventarul declară384 capturi sursă și512 partide reluate independent la pregătirea inputului. Capturile selectate sunt întregi. FAIL-urile originale ale ponderilor respinse rămân vizibile separat de PASS-ul funcționalității certificate. Auditul structural trece; verificarea interactivă a HTML-ului nu a fost rulată. J18 fizic este consumat fără modificare de bibliotecă. Cele640 de partide noi din cele4 loturi rămân externe, cu inventar SHA-256; colecția activă100 nu este înlocuită de ele.
