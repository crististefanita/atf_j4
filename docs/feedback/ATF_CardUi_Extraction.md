# Interfața comună pentru jocuri de cărți

## Pe scurt

Whist integrează deja modulul Android comun. Registrul urmărește verificările pe APK și în ceilalți consumatori, limita Unicode a numelui și extinderile care au nevoie de un contract comun. [Contractul bibliotecii](../../whist-lab-app/atf-card-table-ui/README.md)

## Priorități și condiții de închidere

| ID | Ce rămâne | Criteriu de închidere |
|---|---|---|
| UIX01 | Paritatea față de APK-ul reper | Fixați APK-urile, sursele și configurația. Comparați aceleași nume, seed, intrări și profil pe dispozitiv. |
| UIX02 | Geometria și desenul comun pe Android | Confruntați ancorele, rotunjirile și lizibilitatea la aceiași parametri. Identificați profilul și regiunea fiecărei capturi. |
| UIX03 | Meniu, profil, localizare și limită Unicode | [Dialogul](../../whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/profile/PlayerNameDialog.java) numără unități UTF-16, iar [politica Whist](../../whist-lab-app/card-table-names/src/main/java/ro/cardtable/names/PlayerNames.java) numără puncte de cod. Verificați tastarea și lipirea a zece litere din afara planului multilingv de bază, refuzul celei de-a unsprezecea, focusul, salvarea și resetarea. |
| UIX04 | Pozițiile și proiecțiile mesei în integrarea nativă | La 3–6 locuri, verificați dimensiunea, vizibilitatea și ordinea cărților, apoi o selecție legală aplicată o singură dată. [Matricea Whist](ATF_Android.md#acceptanța-nativă-a-proporțiilor-și-ordinii) |
| UIX05 | Cadrul de scor și colectarea levatei | Demonstrați contractul coloanelor în doi consumatori înainte de a extrage cadrul. În Whist, verificați refacerea, anularea și ultima levată. [Criteriile Android](ATF_Android.md#calificarea-modificărilor-vizuale) |
| UIX06 | Costul și acceptanța consumatorilor AAR | Comparați buildul din sursă cu AAR pe aceleași intrări. Rulați probele native pe versiunea admisă și păstrați identitatea ei. [Împachetare și consum](../../whist-lab-app/atf-card-table-ui/README.md#împachetare-și-consum) |

## Controlul comun „Automat”

- Verificați atingerea și spațiul controlului pe APK compact și cu text mărit în ambele limbi. Refacerea antetului păstrează valoarea furnizată de joc, iar o atingere produce o singură intenție. [Contractul antetului](../../whist-lab-app/atf-card-table-ui/README.md#antetul-mesei)
- Reproduceți debifarea cu o decizie AI în așteptare. Executantul controlat livrează răspunsul după debifare, iar revizia jocului confirmă că răspunsul nu produce o mutare.
- Confruntați bifarea la licitație, oprirea la scor, pauza și reluarea în Whist. Verificați separat poziționarea antetului în consumatorul care folosește AAR-ul. [Acceptanța nativă](ATF_Android.md#acceptanța-nativă-a-proporțiilor-și-ordinii)

## Adopție între jocuri

- Confirmați pentru fiecare consumator versiunea AAR, selecția nativă și verdictul pentru antet, chenare și notificări. Contractul Realizărilor păstrează catalogul, eligibilitatea și stocarea în jocul care le deține. [Suprafața comună](../../whist-lab-app/atf-card-table-ui/README.md#realizări)
- Selectați numai extremele care pot distinge o problemă de geometrie sau accesibilitate. Pentru UIX06, consemnați separat măsurarea buildului și rezultatul nativ al fiecărui consumator. [Strategia de testare](../testing/README.md)
