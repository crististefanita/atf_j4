# Aplicația iOS Whist Lab

## Stare

Ținta nativă iOS este în calificare. Proiectul folosește SwiftUI pentru interfață
și J2ObjC pentru a transpila motorul Java existent, astfel încât regulile,
licitațiile, scorul, adversarii și validarea acțiunilor să rămână implementate o
singură dată. Adaptorul expune către Swift doar starea necesară ecranului și
acțiunile omului; mâinile adversarilor nu sunt incluse în starea publică.

Configurația declară iOS 17 ca deployment target și simulatorul iOS 27.2 ca țintă
de calificare curentă. Suportul devine calificat după un run verde al workflow-ului
`iOS simulator qualification`; verdictul de simulator trebuie atașat commitului
de release înainte ca suportul să fie declarat calificat.

## Structură

| Cale | Rol |
|---|---|
| `WhistLab/` | Interfața SwiftUI, modelul ecranului, checkpoint-ul local și puntea Objective-C. |
| `WhistLabTests/` | Smoke test pe motorul tradus, rulat în simulator. |
| `project.yml` | Configurația XcodeGen, identificatorul iOS și deployment target-ul. |
| `scripts/prepare-shared-core.sh` | Traduce sursele Java și verifică artefactele J2ObjC. |
| `../whist-lab-app/ios-bridge/` | API-ul iOS și testele JVM pentru contractul punții. |

## Dezvoltare și verificare

Build-ul iOS are nevoie de macOS, Xcode 27.2, JDK 21, Homebrew, XcodeGen și
J2ObjC 3.1. Mașina Windows de dezvoltare nu include Xcode. Workflow-ul folosește
runner-ul public `xcode-27`, construiește distribuția J2ObjC din tag-ul fixat,
rulează testele motorului și ale adaptorului, apoi build-ul și testul simulator
pe iOS 27.2. Proiectul leagă `libjre_emul` și `libiconv`, necesare runtime-ului
J2ObjC. Vezi [ghidul iOS](../docs/platform/ATF_iOSDelivery.md).

```sh
cd whist-lab-app
./gradlew :game-core:test :ios-bridge:test
```

Build-ul simulatorului nu este un IPA semnat și nu îl instalează pe un iPhone.
Publicarea prin TestFlight sau App Store necesită arhivare și semnare Apple; pașii
de distribuție iOS rămân separați de APK-ul Android.
