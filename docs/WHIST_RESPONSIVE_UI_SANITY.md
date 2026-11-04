# Suita sanity Whist Lab pentru mai multe ecrane

## Pe scurt

Suita pornește din meniul normal, configurează Whist românesc și joacă un număr
limitat de mâini reale pe fiecare profil. Implicit joacă primele două mâini
reale, fiecare cu o carte, deci două levate per profil. Primele patru mâini ale
programului românesc au câte o carte; a cincea are două. Valoarea maximă 5
permite verificarea primului joc real cu două cărți fără a porni partida
completă.

Verifică apoi ecranul reluat, mâna deterministă cu 13 cărți și
masa de licitare cu valorile 0–8.

## Pornire

Rulați din rădăcina proiectului, cu emulatorul conectat și Whist Lab instalat.

```powershell
.\scripts\run-whist-responsive-sanity.ps1
```

Profilurile implicite sunt 720×1280 la 360 dpi, 1080×1920 la 440 dpi,
1080×2448 la 440 dpi și 1200×1920 la 320 dpi. Ultimul profil verifică
o suprafață de tabletă.

Runnerul primește o listă configurabilă și 1–5 mâini:

```powershell
.\scripts\run-whist-responsive-sanity.ps1 -Profiles '720x1280@360,1080x2448@440' -HandsPerProfile 5
```

Fiecare profil are formatul `LĂȚIMExÎNĂLȚIME@DPI`. Profilurile trebuie să
fie portret, distincte și să ofere cel puțin 320 dp pe lățime. Parametrii
nevalizi sunt respinși înainte de prima comandă către emulator.

`-HandsPerProfile 5` ajunge la prima mână reală de două cărți. Pentru verificarea
rapidă a meniului, licitării, jocului și istoricului, valoarea implicită rămâne
2. Programul verifică mărimea fiecărei mâini folosind numărul afișat și
secvența reală 1–8–1.

## Ce verifică

- Meniul și configurarea încap în primul viewport, fără derulare.
- Mâna reală este centrată jos, într-un singur rând, în containerul complet
  al mesei. Butoanele de licitare rămân în zona centrală și nu acoperă locurile
  jucătorilor.
- Runnerul joacă fiecare carte legală până la scorul mâinii. După mâini,
  procesul aplicației este repornit și istoricul rămâne accesibil.
- Profilurile extreme verifică separat indicii fiecărei cărți din mâna de 13
  și cele nouă opțiuni de licitare. Aceste două ecrane folosesc fixture-uri
  deterministe.
- Fiecare captură păstrează întregul ecran fizic raportat de Android. O
  aserțiune separată confirmă că rezoluția logică activă este profilul cerut;
  capturile brute nu sunt redimensionate. Pe emulatorul folosit aici, suprafața
  fizică rămâne 1080×1920. Profilul logic 1080×2448 produce benzi laterale când
  Android încadrează suprafața logică pe ecranul fizic; este o limită a simulării
  `wm size`, nu dovadă de randare nativă pe un panou 1080×2448. Raportul Extent
  păstrează acțiunile, aserțiunile și capturile în checkpoint-ul corespunzător.

Un singur scenariu Extent păstrează toate profilurile, acțiunile și capturile
în ordinea execuției.

Build-ul Maven și sursele copiate de runner rămân în
`%TEMP%`. Raportul, logurile și fingerprint-ul APK-ului instalat se păstrează
în `ui-reports/whist-lab/runs` din depozitul extern de dovezi.

Runnerul verifică lease-ul comun Android și refuză să controleze un emulator
ocupat. Citește rezoluția și densitatea curente și le restaurează în `finally`,
inclusiv la un eșec de test.

PASS se aplică profilurilor și numărului de mâini consemnate în metadate.

Verificați raportul și auditul vizual înainte de a publica linkul ca ultimă acceptanță.

## Configurație Maven directă

Pentru depanare, testul acceptă aceleași proprietăți:

- `atf.android.sanity.profiles`
- `atf.android.sanity.handsPerProfile`
- `atf.android.artifacts`
- `atf.android.serial`
- `atf.android.api`

Componentele ResponsiveDisplayProfileComponentTest și AndroidUiDriverComponentTest
verifică profilurile, numărul de mâini configurat și rezolvarea butoanelor de bid
cu etichete duplicate înainte ca runnerul să atingă dispozitivul.
