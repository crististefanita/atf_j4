# Guvernanță documentară pentru consumeri

## Problemă

Suita de guvernanță este cuplată la descoperirea reactorului și la țintele adnotate ale proprietarului. O comandă care transmite doar argLine nu dovedește rădăcina scanată: configurația Surefire poate înlocui argumentele JVM.

## Cerință

Expuneți validatorii existenți printr-un API sau CLI cu rădăcina consumerului și lista căilor relative furnizate explicit. Raportați rădăcina efectivă, inventarul documentelor, identitatea validatorilor și fiecare abatere cu fișier, linie, regulă și remediere.

## Criterii de acceptare

- Structură, linkuri/fragmente, proză, stil, diagrame și căi dinamice se aplică aceleiași populații declarate.
- Căile în afara rădăcinii și documentele lipsă sunt refuzate.
- Un control negativ în rădăcina consumerului dovedește că validatorul nu a scanat din greșeală checkout-ul proprietarului.
- Căile cu spații și caractere românești sunt suportate.
- Rezultatul nu depinde de modulele Maven specifice reactorului proprietarului.
- Validatorul nu rescrie documentele consumerului.

## Guvernanța codului

Același API trebuie să poată primi modulele declarate, rădăcinile JVM suplimentare și excepțiile motivate de coeziune. Descoperirea generică nu trebuie să presupună modulele unei aplicații particulare sau să refuze un profil Android valid.

Contractul trebuie să păstreze rezolvarea adnotărilor compuse, ignorarea comentariilor și a exemplelor Java, precum și respingerea unei rădăcini care iese din perimetrul declarat. Excepțiile de pachet verifică lista exactă de fișiere și refuză atât creșterea, cât și înlocuirea ori învechirea listei.

## Integrare temporară

Validatoarele independente sunt adaptate în suita locală, cu descoperirea explicită a proiectului și controale negative. Această copie de test trebuie înlocuită de un contract public împachetat după demonstrarea echivalenței; sursele Core și Reporting rămân la proprietarul lor.

Validarea editorială rămâne separată de verificarea mecanică și se conduce după [principiile documentației](../DOCUMENTATION_GOVERNANCE.md).
