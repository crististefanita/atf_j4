# Numele jucătorilor de la masă

## Pe scurt

`ro.cardtable:card-table-names:0.1.0` este un JAR Java 17. El furnizează politica de nume folosită de Whist și Rentz, inclusiv validarea NFC și repartizarea adversarilor pe locuri.

## Contract

- La mesele de 3–6 jucători, omul ocupă locul 0. Numele său este exclus din lista adversarilor fără diferențiere de majuscule.
- Aceeași intrare produce aceeași listă imutabilă, în ordinea locurilor. Generatorul folosește un flux separat de distribuire și de AI.
- Consumatorul își gestionează preferințele, identitatea locurilor și salvarea. Biblioteca rămâne independentă de Android și de regulile jocului.

## Verificare și publicare

J4 verifică politica și recuperarea partidei prin componentele din `game-core`. Consumatorii verifică separat numele randate și restaurarea.

```powershell
.\gradlew.bat --offline --no-daemon :card-table-names:publishReleasePublicationToIsolatedRepository -Pcardui.repository=<repository-extern>
```

[Rendererul Android comun](../card-table-ui/README.md)
