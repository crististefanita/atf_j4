# Separarea responsabilităților și guvernanța codului

## Contract urmărit

Pachetele grupează tipuri care se schimbă pentru aceeași responsabilitate. Pragul de coeziune este o alarmă structurală; o excepție justificată nu certifică arhitectura și nu autorizează creșterea.

## Datorie explicită

| Zonă | Problemă | Criteriu de închidere |
|---|---|---|
| Motor | Reguli, stare, estimare, căutare și certificate împart accesul de pachet | API-uri restrânse și pachete semantice, fără transmiterea informației private către politici. |
| Teste de domeniu | Regresiile, protocoalele și referințele înghețate au colecții ample | Separare pe proprietate verificată, cu selecții păstrate și fără creșterea costului implicit. |
| Interfață | Compoziția, geometria, localizarea și persistența împart pachetul aplicației | Limite explicite, fără modificarea regulilor și fără regresii de accesibilitate. |
| Android | Scenariile și suportul istoric au acces comun | Separare între interacțiuni, oracole, fluxuri și publicare, cu selecții echivalente. |
| Compoziția testelor | Publicatorii și contractele binare împart pachetul general | Pachete pe responsabilitate și actualizarea referințelor fără copierea infrastructurii producătorului. |

[Politica executabilă](../../config/atf-governance-policy.json) păstrează inventarele exacte pentru excepțiile existente. Adăugarea, înlocuirea sau dispariția unui tip cere reconciliere explicită; nu actualizați automat lista pentru a închide un eșec.

## Reutilizarea verificatoarelor

- Păstrăm local validatoarele de documentație, descoperirea sigură a surselor și rezolvarea adnotărilor compuse.
- Adaptarea modifică perimetrul și descoperirea proiectului, fără a importa module străine domeniului.
- Un API comun viitor trebuie să accepte explicit rădăcina, modulele, rădăcinile suplimentare și excepțiile motivate.
- Diagnosticul trebuie să includă regula, fișierul și remedierea, iar inventarul gol ori o rădăcină greșită trebuie refuzate.
- Proprietatea bibliotecilor și rutarea cerințelor comune rămân în [indexul de feedback](README.md).

## Acceptare

Orice relaxare a unei reguli are justificare semantică și control negativ. Eliminarea unei copii locale devine posibilă după existența unui API comun echivalent și verificarea lui pe perimetrul Android.
