# Whist Lab — feedback de utilizare

Registrul urmărește ce poate vedea, înțelege și atinge jucătorul. Acceptanța
0.1.3 păstrează identificarea APK-ului, profilele și rapoartele. Revizia
anterioară rămâne disponibilă în Git la `ac4edf8`, în acest fișier.
[Ultimul raport Extent Whist Lab](whist-extent/README.md) · [Acceptanță 0.1.3](../WHIST_UI_013_ACCEPTANCE.md) · [Istoric 0.1.2](../WHIST_UI_012_ACCEPTANCE.md)

## Ce a schimbat verificarea pe dispozitiv

| Observație | Corecție și probă de păstrat |
|---|---|
| La 13 cărți, vecina ascundea simbolul unor cărți de 10. Numărarea nodurilor XML dădea PASS. | Indice vertical, lățime măsurată, reper central și colț inversat. Fixture-ul conține toate cele patru cărți de 10. Pentru fiecare indice se verifică banda expusă, pixelii din ambele linii și marginea liberă. |
| La patru locuri și nouă opțiuni, valorile 5–8 intrau peste Ava și Grace. | Scaunele și centrul au un singur proprietar de layout. Verificarea compară toate perechile de dreptunghiuri și măsoară țintele de 48 dp. Selectarea valorii 8 trebuie să păstreze scaunele fixe. |
| La 320 dp pe ecranul scund, toate rangurile erau lizibile, dar fundul cărților dispărea sub navigare. | Ferestrele scunde folosesc spații mai mici și rezervă ridicarea completă numai în joc. Aserțiunea verifică întreaga înălțime de 98 dp în ierarhia Android, iar captura păstrează marginea de jos pentru inspecție. |
| Un scor 0 și un buton 0 au același text în ierarhia Android. | Driverul identifică butonul prin rol și valoare. Componenta negativă respinge un text de scor folosit drept buton. |
| Selectorul de limbă nu sugera deschiderea, iar opțiunile românești împingeau ieșirea sub ecran. | Chevron pe spinnerul nativ, serie și valoare pe un singur rând, program complet la cerere. Verificarea pornește din Home și cere întreaga țintă Back în primul viewport. |
| Explicațiile împingeau rezultatele sub primul ecran de scor. | Tabelul apare înaintea detaliilor de calcul. Continuarea mâinii rămâne acțiunea principală. |

## Istoricul scorului în 0.1.5

Foaia consultativă și ecranul de rezultat oferă un istoric per jucător. Cea mai recentă mână apare prima, cu licitația, punctele și totalul după acea mână. La migrare apare soldul cunoscut pentru perioada fără detaliu. [Contract, pilot și reluare offline](../WHIST_SCORE_HISTORY.md)

## Priorități deschise

La șase jucători, prima revizie așeza flancurile de sus la 8% din înălțimea mesei.

Proba pe masă normală a descoperit diferența față de mock. Geometria folosește acum spațiul disponibil până la regiunea centrală și păstrează distanța dintre scaune și țintele de licitare. O componentă fixează cazul măsurat pe emulator.

În captura românească de scor apărea antetul englezesc `Bid`; `Mână` nu
explica unitatea. Antetele scurte sunt acum `Lic.` și `Pct.` și descrierea
accesibilă numește punctele din mâna curentă. Verificarea din meniu cere
etichetele în limba selectată.

| Prioritate | Problemă pentru jucător | Criteriu de închidere |
|---|---|---|
| P2 | Istoricul celor patru locuri este verificat pe emulator după repornire. Rămân de evaluat selecția de pe masa activă, textul mărit și focusul asistat. | Dintr-o licitație sau cu o carte ridicată, deschideți istoricul, consultați ultimul jucător, reveniți și verificați aceeași selecție. La șase locuri, text 150%, verificați primul și ultimul rând, butonul Închide și citirea unei valori negative cu TalkBack. |
| P2 | La ultima licitație lipsește uneori o valoare fără motiv vizibil. Un jucător nou poate interpreta lipsa ca defect al grilei. | Explicați contextual regula numai când se exclude o valoare. Proba trebuie să distingă valoarea exclusă, zero legal și un total legal mai mare decât numărul cărților. |
| P2 | Pe emulator, pică fără carte întoarsă folosește glifa Android închisă pe fond verde. | Verificați contrastul simbolului pe telefon și la lumină puternică. Comparați un fundal deschis pentru simbol cu varianta actuală, păstrând distincția față de cartea întoarsă și spațiul față de titlu. |
| P2 | Abrevierile scorului la lățime mică necesită confirmare cu jucători. | Cereți identificarea punctelor ultimei mâini și a totalului la patru și șase locuri, inclusiv la text mărit. Măsurați dacă încape cuvântul întreg înainte de extinderea antetului. |
| P2 | La font mare sau la 14–17 cărți, vederea parțială trebuie să comunice că mâna derulează. Contrastul descrierilor accesibile trebuie verificat și cu utilizare reală. | La 130% și 150%, ajungeți la prima și ultima carte prin gest și focus, cu identitatea întreagă. Parcurgeți licitare, carte legală și scor cu TalkBack. |
| P2 | Profilul logic ROG 7 diferă de telefonul real prin bare, gesturi și densitate. | Măsurați pe telefonul fizic zona sigură, cartea selectată și revenirea din scor. Păstrați valoarea reală a densității și captura originală. |
| P3 | La 320 dp, cu patru jucători și nouă licitații, cele trei scaune adverse se apropie vizual de un singur rând. | Comparați cu o variantă cu scaune compacte pe durata licitării. Acceptați schimbarea numai dacă toate valorile rămân lizibile, țintele păstrează 48 dp și omul rămâne fix la selecție. |
| P3 | Un E2E care compară numai bounds poate rata contrastul slab, schimbarea ordinii desenării sau un caracter greșit. | Păstrați captură și XML din aceeași stare. Pentru un nou defect vizual adăugați un negativ care îl reproduce și o verificare de pixeli focalizată. |

## Cum citim probele

Pentru meniul simplificat, chenarul de atu, mâna centrată și istoricul bid–puncte–total consultați [capturile și bilanțul 0.1.5](../WHIST_UI_015_ACCEPTANCE.md).

Pentru handoff către componente și AI, acceptanța de șase mâini se poate relua
offline din trace-ul public. Arhiva distinge încercarea eșuată, ultimul rezultat
acceptat în fiecare scope și hash-urile măsurate ulterior. [Stare și probe UI-HANDOFF](../WHIST_UI_REPORT_HANDOFF.md)

- Fiecare profil verifică separat lățimea logică și densitatea efectivă. Captura
  fizică a emulatorului poate avea benzi negre în exteriorul aplicației.
- Matricea geometrică de componentă verifică dreptunghiurile pentru 3–6 locuri.
  Datele de display folosesc o distribuție deterministă. Pilotul normal pornește
  din meniu și produce o distribuție nouă.
- Păstrăm scorurile UX ca evaluări de revizie, asociate ecranelor parcurse.
  Accesibilitatea asistată și acceptanța fizică au probe proprii.
- Capturile, XML-urile și rapoartele rămân în TMP. Pachetul live conține
  codul, documentația, ultimul APK acceptat și suma lui SHA-256.

[Atlasul layout-ului](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md) · [Testele Android](../../atf-test-android/README.md)
