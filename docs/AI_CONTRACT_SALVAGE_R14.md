# Hard — final certificat fără sacrificarea punctelor proprii

Corecție de funcționalitate separată de modelele R13 respinse. Doar offline, fără UI, emulator, APK nou sau modificări Reporting. [Manifest YAML](../config/whist-contract-salvage.yaml).

## Contraexemplul

Fără atu, cu două cărți rămase. Avem7S/7H și am ratat contractul0, cu4 levate deja câștigate. Istoricul public complet arată că adversarul1 are AS/8S, adversarul2 AH/8H, iar adversarul3 AC/8C. Cărțile nu sunt citite din mâinile ascunse: aceasta este unica distribuție compatibilă cu cele24 de cărți jucate și lipsurile de culoare demonstrate.

Adversarul1 mai cere2 levate; adversarul2 mai cere1; adversarul3 este deja peste contract. Cel mai mare scor total este al adversarului3, egal în ambele variante. Vechiul tie-break după scorul celui mai puternic rival nu distinge cele două deschideri.

| Deschidere | Cine ia ultimele două levate | Adversari ratați | Penalizarea noastră |
|---|---|---:|---:|
|7S, motorul acceptat R11|Adversarul1 își face2|2|−4|
|7H, corecția certificată|Adversarul2 ia2 în loc de1|3|−4|

Ambele trasee sunt verificate separat pas cu pas pe posesie, legalitate, câștigător și contracte. Motorul nou verifică și toate celelalte răspunsuri legale; acesta nu este doar un replay cu adversari cooperanți.

## Condiții stricte de activare

Garda lucrează numai pentru Whist românesc cu4 jucători și2 cărți proprii. Necesarul este bid−won. Oportunitățile includ levata incompletă. Atuurile proprii sigur majore furnizează o limită inferioară de câștig: cu AS întors și KS/QS proprii, cel puțin2 levate sunt inevitabile. Un contract1 este astfel imposibil chiar dacă limita numerică1 din2 părea permisivă.

Contractul propriu trebuie să fie deja imposibil. Pentru fiecare distribuție public compatibilă și fiecare răspuns legal, alternativa trebuie să aibă minimum de puncte proprii cel puțin egal cu maximum posibil pe propunerea veche. Similar, minimum de adversari ratați trebuie să fie cel puțin egal cu maximum de adversari ratați pe propunerea veche. Cel puțin o îmbunătățire trebuie să fie strictă. Aceste condiții sunt puternice și pot refuza multe alternative utile dar nedemonstrate.

Sunt păstrate cărțile neîmpărțite, cartea întoarsă, toate cărțile publice și lipsurile de culoare. O enumerare incompletă sau peste40000 de noduri păstrează decizia anterioară și certified=false. Egalitatea mediilor nu este o dovadă: cazul AS/AD cu medie−2,6 are intervale suprapuse și nu primește certificat.

Licitarea, samplingul și continuarea Monte Carlo acceptate rămân identice. Nu sunt activate modelele de presiune sau ponderile experimentale. Contractele proprii recuperabile nu sunt sacrificate pentru a arde adversarii.

## Protocol și limitări

Cele6 componente ale corecției trec înainte de înghețare. Gate-ul de funcționalitate cere și regresia completă, apoi non-regresie pe puncte, victorii ponderate și contracte exacte2–8 în fiecare familie de adversari pe113000–113007. Acest lot este separat de evaluarea de forță R13. Un rezultat zero demonstrează numai lipsa unei regresii în eșantion; nu este numit creștere generală a forței de joc. Se declară separat câte trasee de partidă aleatorie s-au schimbat efectiv.

Rezultatele native și finale sunt completate după închiderea execuției.

## Acceptanță închisă și activare

**492 de componente de domeniu trec**, cu toate aserțiunile vechi ale motorului acceptat păstrate. Cele6 teste noi protejează garda, replayul independent, contractul recuperabil, bugetul incomplet, diferența dintre medie și dominanță și atuul întors.

Pe cele64 de comparații pereche /128 de partide noi, din8 clustere de seed-uri, toate deltele de puncte, victorii și exacte2–8 sunt zero în fiecare familie. Cele64 de urme de domeniu sunt identice; lotul aleatoriu nu a exercitat o schimbare de decizie. Cazul explicit construit și verificat independent exercită ramura nouă. Prin urmare, acceptăm o **corecție demonstrată de funcționalitate**, fără a pretinde un progres statistic general al forței de joc.

Garda este activată în Hard. Normal este neschimbat. Modelele de presiune rămân numai în cele21 de componente research opt-in. Nu s-au modificat cele două teste istorice ajustate doar pentru candidatul R13 respins. Source freeze, corpusul complet și FEATURE_ACCEPTANCE.json sunt externe în certified-salvage. Nu s-a construit sau instalat un APK și nu s-a executat interfața.

Controlul quick executat după activare trece168 de cazuri, subset al regresiei492. Sursa tuturor celor70 de fișiere de politică din stagingul quick coincide exact cu source freeze-ul acceptat. Cercetarea21 trece separat și nu intră în APK sau în regresia implicită.

## Raport și dovezi finale

[Extent J18 final](C:/work/whist_back/ai-training/20261103-r13-pressure/reports/final-extent-report.html):27 capturi, 1239 comparații, 4492251 bytes. Inventarul declară384 capturi sursă și512 partide reluate independent la pregătirea inputului. Capturile selectate sunt întregi. FAIL-urile originale ale ponderilor respinse rămân vizibile separat de PASS-ul funcționalității certificate. Auditul structural trece; verificarea interactivă a HTML-ului nu a fost rulată. J18 fizic este consumat fără modificare de bibliotecă. Cele640 de partide noi din cele4 loturi rămân externe, cu inventar SHA-256; colecția activă100 nu este înlocuită de ele.
