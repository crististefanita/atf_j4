# Note de versiune 0.13.0

- Android: pachet nou `versionCode` 19, construit din configurația SDK 36.
- iOS: adăugată aplicație SwiftUI cu punte către motorul Java comun, checkpoint local și smoke test care licitează, joacă prima mână și restaurează checkpoint-ul.
- Adaptarea iOS nu implementează reguli sau adversari separați; traducerea J2ObjC și smoke test-ul simulatorului nu au fost încă executate. GitHub Actions a refuzat pornirea la nivel de utilizator, deci ținta rămâne necalificată.
- Calificare Android/JVM: 502 componente pe colecțiile Bidding, Commands, Display și History; 44 teste de reguli și sesiune; 3 teste pentru contractul punții iOS.
- Nu s-a rulat E2E Android sau iOS. Încercarea full `:game-core:test` a rămas necalificată din cauza epuizării memoriei în testele de învățare și a unei aserțiuni din `OfflineMockMatchTest`.
