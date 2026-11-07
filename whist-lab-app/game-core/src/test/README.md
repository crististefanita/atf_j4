# Testele motorului Whist

## Pe scurt

Testele verifică regulile, sesiunile și politicile din observația permisă actorului. [Strategia comună](../../../../docs/testing/README.md) deține nivelurile și criteriile; acest catalog delimitează colecțiile motorului și costul lor.

## Colecții și oracole

| Familie | Proprietate și limită |
|---|---|
| Reguli și scor | Cărți legale, câștigător, contracte, serii și vecinii invalizi, cu așteptări independente. |
| Sesiune și restaurare | Comenzi, avansarea unică a mâinii, istoric și refuzul stărilor inconsistente. |
| Politică AI | Observația actorului, legalitatea, necesarul și alegeri justificate în domenii controlate. |
| Joc la 0 | Scorul final în ambele variante românești, tranziția AI către zero exact și păstrarea strategiei obișnuite când locul al doilea rămâne posibil. |
| Cercetare | Inferență și continuări experimentale, separate de regresia implicită. |
| Evaluare offline | Roluri, surse și loturi identificate; progresul strategic are protocol și date rezervate proprii. |

## Alegerea execuției

[Colecțiile proiectului](../../../../docs/testing/ATF_Collections.md) aleg sarcinile din [Gradle](../../build.gradle). [Intrarea JVM](../../../algorithm-build/README.md) permite verificări fără SDK Android; folosiți copii temporare și output extern.

O regresie mică fixează regula și defectul distinct, fără a rula din nou întregul corpus de antrenare. Cercetarea, arena și evaluarea rezervată se selectează explicit și nu se adună la regresie ca teste unice noi.

## Interpretarea rezultatului

Corectitudinea unei reguli, costul unei decizii și superioritatea politicii sunt calificări diferite. [Protocolul de învățare](../../../../docs/learning/README.md) păstrează proveniența, selecția și criteriile de promovare; un rollout ori un certificat finit nu garantează câștigarea fiecărei partide.

## Evaluarea jocului la 0

Sarcina `:game-core:zeroMatchPairedTest` rulează explicit partide complete pe JVM. Perechea păstrează locurile, seria, distribuția, ordinea mâinilor și atuurile, cu regula dezactivată și activată.

| Parametru Gradle | Rol |
|---|---|
| `whist.zero.firstSeed`, `whist.zero.repeats` | Intervalul determinist de distribuții pentru fiecare combinație de variantă și număr de locuri. |
| `whist.zero.pressure` | Alege lotul cu prefix controlat, în care un loc primește licitații adverse legale înainte de ultimele cinci mâini. Loturile naturale și controlate se interpretează separat. |
| `whist.zero.output`, `whist.zero.reference` | Scrie perechile JSON într-un dosar nou și confruntă controlul cu referința înghețată, când este disponibilă. |
| `whist.auditOutput`, `whist.auditSource` | Destinația Extent și identificarea sursei de execuție. |

JSON-ul reține mutările publice, scorurile, alegerile către zero și amprentele distribuțiilor. Compararea finală separă intervalele de dezvoltare de cele rezervate.

[Motorul](../../README.md) · [Strategia comună](../../../../docs/testing/README.md) · [Proiectarea componentelor AI](../../../../docs/testing/ATF_AiComponents.md)
