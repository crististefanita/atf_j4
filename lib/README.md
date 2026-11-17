# ATF libraries

## Overview

Java 21 and Maven; five JARs thin with transitives from POM. The guidelines shall be autonomous from the manufacturer's sources.

| Ghid | Artefact, grup `atf.api` | Usage |
|---|---|---|
| [Core](README.Core.md) | `atf-core` | HTTP, configuration, JSON and lifecycle. |
| [Reporting](README.Reporting.md) | `atf-reporting` | Extent/fallback and auditorialevidence. |
| [Console](README.Console.md) | `atf-console` | Diagnostic Maven and IntelliJ. |
| [Mock](README.Mock.md) | `atf-mock` | HTTP offline components and contracts. |
| [Test Kit](README.TestKit.md) | `atf-test-kit` · audit and lifecycle JUnneuter | Classification, comparisons and test support. |

## Integrare

1. In a TMP copy of an ATF consumer, `mvn -B -ntp -N validate` installs the pairs of lib in the selected Maven repositoryul.
2. Declare the dependency in the caller module; the coordinates and the version are in the next tree of the JARI.
3. IntelliJ uses the same JDK and repository Maven; reload the root POM.

<details>
<summary>New consumer without ATF reactor</summary>

Copy pairs JAR/POM in lib; again, in TMP:

```powershell
Get-ChildItem lib\*.pom | ForEach-Object {
    mvn org.apache.maven.plugins:maven-install-plugin:3.2.0:install-file "-Dfile=$([IO.Path]::ChangeExtension($_.FullName,'jar'))" "-DpomFile=$($_.FullName)" -DgeneratePom=false
    if ($LASTEXITCODE) { throw "Instalare eșuată: $($_.Name)" }
}
```

You declare, for example, `atf.api:atf-reporting:<versiunea din POM>` in dependencies. Use `compile` scope for support in src/main and `test` only in src/test; Maven solves transitions.

</details>

## Identity and verification

- One pair JAR/POM per library, index and five guides, straight into the lib.
- In ATF, pom.xml selects versions; libraryprovenance.json keeps hashes and origins.
- Governance and acceptance run in TMP; each consumer checks his own profile and classspot.

## Choice of minimum verification

| Library | Start point | Contractul observabil |
|---|---|---|
| Core | [Exemplul minim](README.Core.md#exemplu-minim) | Construction of the URL and maintenance of JSON accuracy. |
| Reporting | [Exemplul minim](README.Reporting.md#exemplu-minim) | Publication completed, followed by the spread of the failed comparison. |
| Console | [Exemplul minim](README.Console.md#exemplu-minim) | Visible message or console disabled, according to explicit policy. |
| Mock | [Exemplul minim](README.Mock.md#exemplu-minim) | The configuration and application produce the answer or the declared fallback. |
| Test Kit | [Exemplul minim](README.TestKit.md#exemplu-minim) | Comparison assessed once, PASS/FAIL and the JUnit case kept. |

Check first the compilation on physical pairs JAR/POM, then the behavior of the fragment. Application contracts, real transport and interactive presentation shall qualify separately in the consumer.

## Maintenance and feedback

Libraries are consumed as versed pairs JAR/POM; their source code is not part of this project. Keep exact coordinates and hash of the pair, then qualify the contract by compiling examples and local integration tests. For an observed limitation, attach the pair, hash, reproduction and expected / observed values in [project feedback](../docs/feedback/README.md).
