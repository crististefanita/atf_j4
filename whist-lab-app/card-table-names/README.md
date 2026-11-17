# Name of the players at the table

## Overview

`ro.cardtable:card-table-names:0.1.0` is a JAR Java 17., which provides neutral name policy, including the validation of NFC and the distribution of opponents in place.

## Contract

- At the tables of 3–6 players, man occupies 0. place his name is excluded from the list of opponents without distinction from the capitals.
- The same entry produces the same immutable list in order of seats. The generator shall use a separate distribution and AI flow.
- The consumer manages his preferences, identity of his places and salvation. The library remains independent of Android and the rules of the game.

## Verification and publication

Component tests check the policy and recovery of the game by `game-core`. Consumers shall verify separately the names of the names and the restoration.

```powershell
.\gradlew.bat --offline --no-daemon :card-table-names:publishReleasePublicationToIsolatedRepository -Pcardui.repository=<repository-extern>
```

[Rendererul Android comun](../atf-card-table-ui/README.md)
