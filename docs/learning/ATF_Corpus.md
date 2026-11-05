# Contractul corpusului de partide

Corpusul este o intrare externă pentru replay, selecție și analiză offline. Proiectul deține schema și validatoarele; fișierele partidelor, manifestele concrete și registrele loturilor rămân în depozitul extern configurat.

## Modelul datelor

| Entitate | Contract |
|---|---|
| Partidă | Identitate, partiție, profil de reguli, politici și ordinea mâinilor. |
| Mână | Dealer, distribuție, atu și carte întoarsă, licitații ordonate, levate și închiderea scorului. |
| Observație | Mâna actorului și prefixul public disponibil la decizie. |
| Manifest | Fișiere selectate, hash-uri, identitatea părintelui și scopul selecției. |
| Registru de lot | Separarea dezvoltării de evaluare și evidența intervalelor deja consumate. |

```mermaid
flowchart LR
    FILE[Partidă externă] -->|alimentează| ORACLE[Validator independent]
    ORACLE -->|admite în partiție| MANIFEST[Manifest verificat]
    MANIFEST -->|selectează explicit| REPLAY[Replay și analiză]
    REPLAY -->|proiectează| OBS[Observații pentru diagnostic]
```

<details>
<summary>Legendă: accesul la date</summary>

Validatorul poate verifica proprietatea tuturor cărților după încheierea jocului. Politica primește proiecția permisă la decizie, iar analiza nu transformă mâinile adverse reale în intrări ale acelei politici.

</details>

## Partiții și integritate

- Development permite analiza și ajustarea; selecția activă citește numai manifestul declarat.
- Heldout este rezervat evaluării candidatului înghețat și se înregistrează ca expus la deschidere.
- Reference păstrează intrări de compatibilitate; ele nu justifică o afirmație despre puterea actuală a unei politici.
- Repetările aceleiași distribuții sau urme nu se numără drept observații independente.
- În schema partidei, seed-ul este șir zecimal exact; parserul nu îl convertește printr-un număr JavaScript care ar pierde precizia tipului Java long.
- Identitatea, partiția, căile și SHA-256 se verifică înainte de utilizare.

## Utilizare

[Configurația corpusului](../../config/learning-corpus.json) indică arhiva și pointerul activ. [Validatorul](../../whist-lab-app/ai-lab/tools/corpus/whist-corpus.mjs) verifică regulile și scorul, iar [selectorul activ](../../whist-lab-app/ai-lab/tools/corpus/whist-active-corpus.mjs) controlează selecția.

```powershell
$gamePath = Read-Host 'Fișierul JSON extern pentru replay'
node whist-lab-app/ai-lab/tools/corpus/whist-corpus-cli.mjs replay $gamePath
node whist-lab-app/ai-lab/tools/corpus/whist-active-corpus.mjs verify-current
```

[Criteriile selecției](ATF_Selection.md) și [protocolul evaluării](README.md) explică folosirea corpusului pentru îmbunătățirea politicilor.
