# Compatibilitate și livrare iOS

## Platforme și pachete

APK-ul Whist Lab este un pachet Android și nu se instalează pe iPhone sau iPad.
Proiectul iOS este o aplicație separată cu interfață SwiftUI și folosește motorul
Java comun prin traducerea J2ObjC. Motorul original continuă să fie sursa regulilor
de joc, AI, licitare, scor și salvări; adaptorul iOS nu implementează o a doua
variantă a acestor reguli.

Deployment target-ul este iOS 17.0. Calificarea curentă folosește iOS 27.2 și
Xcode 27.2, cele mai noi versiuni stabile găsite la 7 noiembrie 2026; iOS 27.3
este încă beta. Versiunile Apple se schimbă; actualizați simultan runner-ul,
runtime-ul simulatorului și această pagină după verificarea documentației oficiale.
[Versiunile Apple](https://developer.apple.com/news/releases/) ·
[Matricea Xcode și SDK](https://developer.apple.com/xcode/system-requirements/)

## Calificarea pe GitHub Actions

Un runner macOS găzduit permite build și teste fără Mac local.

- Pentru repository-uri publice, runner-ele standard sunt gratuite și nelimitate.
- Pentru repository-uri private, consumul folosește minutele incluse, apoi se taxează la tariful planului. Verificați vizibilitatea și facturarea înainte de a estima costul.
- Imaginea `xcode-27` este public preview și nu are garanțiile unui runner stabil.
- La 7 noiembrie 2026, imaginea include Xcode 27.2 și simulatorul iOS 27.2. Workflow-ul fixează versiunea în loc să aleagă `latest`.
[Runner-e și condiții GitHub](https://docs.github.com/en/actions/reference/runners/github-hosted-runners) ·
[Imaginea curentă macOS Xcode 27](https://github.com/actions/runner-images/blob/main/images/macos/xcode-27-arm64-Readme.md) ·
[Preview Xcode 27](https://github.blog/changelog/2026-09-10-xcode-27-runner-image-now-runs-on-macos-27/)

Workflow-ul [iOS](../../.github/workflows/ios.yml) verifică pe același commit:

1. testele JVM ale motorului și adaptorului nativ;
2. traducerea surselor Java cu J2ObjC;
3. build-ul aplicației pentru simulator;
4. smoke test-ul Swift în simulatorul iOS 27.2.

J2ObjC 3.1 este fixat în workflow și construit din sursă, deoarece distribuția
precompilată nu mai este publicată de proiect. J2ObjC este beta și traduce logica,
nu interfața; SwiftUI rămâne stratul UI nativ. Build-ul din runner califică
compatibilitatea compilării și a testului automat pentru iOS 27.2, nu fiecare
versiune intermediară și nici experiența pe dispozitive fizice.
[J2ObjC — proiect și limite](https://github.com/google/j2objc) ·
[Construirea J2ObjC](https://developers.google.com/j2objc/guides/building-j2objc)

## Pașii de livrare

1. Rulați workflow-ul `iOS simulator qualification` pe commitul sursă al release-ului.
2. Verificați rezultatul testului și păstrați `xcresult`-ul atașat run-ului.
3. Pentru TestFlight sau App Store, arhivați cu Xcode, semnați cu identitatea
   organizației și validați instalarea pe dispozitive iPhone și iPad.
4. Publicați separat aplicația iOS prin fluxul Apple. APK-ul și hash-ul Android nu
   servesc drept asset ori dovadă de instalare iOS.

Runner-ul simulator construiește aplicația fără semnare Apple. Crearea unui IPA
distribuibil și încărcarea lui în TestFlight/App Store cer un Apple Developer
Team, certificate, profil de provisioning și pași de release Apple; aceste
acreditări nu sunt stocate în repository sau în workflow-ul public.

[Livrarea Android](ATF_AndroidDelivery.md) · [Proiectul iOS](../../ios/README.md)
