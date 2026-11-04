# Contracte consumer pentru publicarea Android

## Responsabilitate

Consumerul deține acțiunile, observațiile și oracolul aplicației. Biblioteca comună deține tipurile neutre și prezentarea, prin responsabilitățile din [indexul de feedback](README.md).

## Cerințe permanente

| Suprafață | Contract |
|---|---|
| Atribuirea acțiunilor | Driverul Android este identificat din comanda reală; browserul de revizie are rol separat. |
| Rezultate | Dispatch-ul, observația și comparația semantică păstrează rezultate distincte. |
| Intervale | Originea monotonă și proprietarul fazei sunt capturate înaintea activității. |
| Întrerupere | Starea parțială și cauza originală rămân vizibile. |
| Date vechi | Timing-ul absent rămâne indisponibil, fără reconstrucție din sumele copiilor. |
| Republicare | Identitatea și conținutul execuției originale sunt păstrate. |
| Documentație | README-urile descriu mecanisme; indexurile de execuție sunt externe. |

## Criterii de închidere

- Refuzați lipsa scalarilor declarați, intervalele imposibile și proprietarii incompatibili.
- Păstrați pixelii capturii și identitatea checkpoint-ului prin publicarea individuală și agregată.
- Verificați aceleași contracte prin API-ul public pentru rendererul și fallback-ul consumate.
- Revizia interactivă și execuția pe dispozitiv au scopuri explicite, separate de componente.

[Timing Android](../ATF_Android_Phase_Timing.md) · [Verificarea publicării](../REPORTING_AUDIT_CONTRACT.md)
