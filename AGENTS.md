# ATF workspace rules

- Console and Mock are J0-owned source/native-test libraries alongside Core/Reporting. J1–J4 keep four flat JAR/POM pairs; retain local adapters/configuration and validate each consumer independently. Read `docs/ATF_Platform_Libraries.md` before publication.

- Core, Reporting, Console and Mock implementation/native tests stay in J0; retain four flat JAR/POM pairs and local Android acceptance. See `docs/ATF_Platform_Libraries.md`.
