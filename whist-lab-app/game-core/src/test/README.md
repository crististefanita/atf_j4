# Testele motorului Whist

## Pe scurt

Testele verifică regulile, sesiunile și politicile din observația permisă actorului. [Strategia comună](../../../../docs/testing/README.md) deține nivelurile și criteriile; acest catalog delimitează colecțiile motorului și costul lor.

## Colecții și oracole

| Familie | Proprietate și limită |
|---|---|
| Reguli și scor | Cărți legale, câștigător, contracte, serii și vecinii invalizi, cu așteptări independente. |
| Sesiune și restaurare | Comenzi, avansarea unică a mâinii, istoric și refuzul stărilor inconsistente. |
| Politică AI | Observația actorului, legalitatea, necesarul și alegeri justificate în domenii controlate. |
| Cercetare | Inferență și continuări experimentale, separate de regresia implicită. |
| Evaluare offline | Roluri, surse și loturi identificate; progresul strategic are protocol și date rezervate proprii. |

## Alegerea execuției

[Colecțiile proiectului](../../../../docs/testing/ATF_Collections.md) aleg sarcinile din [Gradle](../../build.gradle). [Intrarea JVM](../../../algorithm-build/README.md) permite verificări fără SDK Android; folosiți copii temporare și output extern.

O regresie mică fixează regula și defectul distinct, fără a rula din nou întregul corpus de antrenare. Cercetarea, arena și evaluarea rezervată se selectează explicit și nu se adună la regresie ca teste unice noi.

## Interpretarea rezultatului

Corectitudinea unei reguli, costul unei decizii și superioritatea politicii sunt calificări diferite. [Protocolul de învățare](../../../../docs/learning/README.md) păstrează proveniența, selecția și criteriile de promovare; un rollout ori un certificat finit nu garantează câștigarea fiecărei partide.

[Motorul](../../README.md) · [Strategia comună](../../../../docs/testing/README.md) · [Proiectarea componentelor AI](../../../../docs/testing/ATF_AiComponents.md)
