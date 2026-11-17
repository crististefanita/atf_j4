# ATF quality governance

## Overview

Governance converts the principles of structure into local executable checks. The suite inspects the sources, documents and contracts of the libraries consumed without device, tournament or access to external services.

## Complementary guides

- [Good documentation practices](ATF_Documentation.md): organization, drafting, legends, navigation and live revision.
- [Change of tests](ATF_TestChanges.md): independent expectations, negative control and responsibility transfer.
- [Package responsibilities](ATF_Packages.md): engine structure, application and tests, cohesion and dependency limits.

## Perimeter and responsibilities

```mermaid
flowchart LR
    POLICY[Explicit policy] -->|delimited| INVENTORY[Source inventory]
    INVENTORY -->|fuel| GUARDS[Checks]
    DOCS[Documente curente] -->|fuel| GUARDS
    GUARDS -->|locate| FINDING[Explanatory deviation]
    FINDING -->|cere| REVIEW[Correction and revision]
```

<details>
<summary>Legend: a result has a perimeter</summary>

The inventory includes the declared Maven roots and additional JVM roots in politics. Documentary validators inspect all the Readmees and Markdown files in docs, excluding the directories generated.

</details>

[Local Suite](../../atf-tests/src/test/java/atf/tests/governance/AtfQualityGovernanceSuite.java) and [politica](../../config/atf-governance-policy.json) define the enforceable contract. Adaptation keeps the reusable rules of common verifiers; assumptions about other applications modules are not imported.

| Inventar | Input |
|---|---|
| Maven | The declared root and the responsible POM are kept together, including for nested modules; the metadata are not rebuilt from the first segments of the path. |
| Other roots | The module and path are explicit and `test` is Boolean; the root remains in its module. |
| Property | A file has one person and one set of sources; overlaps are denied without any deduction that would conceal ambiguity. |

[Inventory contracts](../../atf-tests/src/test/java/atf/tests/governance/support/SourceInventoryContractTest.java) checks both the valid source and the configurations that would lose the identity or change the owner.

## JVM Inventory via CLI

Start [inventarul JVM](../../scripts/run-jvm-inventory.ps1) from the checkout that provides the tool. Parameter `-ProjectRoot` indicates the project inspected. `-ScratchRoot` indicates an existing directory outside both checkouts.

PowerShell 7, JDK 21 and local cachet Jackson are required.

```powershell
.\scripts\run-jvm-inventory.ps1 -ProjectRoot $checkout -ScratchRoot $scratch
```

| Entry or verdict | Contract |
|---|---|
| Maven | `mavenModules` coincides with reactor modules and discovered POMS. An implicit missing root can be omitted. An explicitly configured root must exist. |
| Grades and other sets | `additionalSourceRoots` declares the module, the canonical relative path and role `test`. Roots outside the module and overlaps are refused even when the directories are empty. |
| Exceptions | `reviewedPackages` requires the exact inventory, motive and action. `productionBoundaryAdapters` requires the exact production file, the prohibited reason and the permitted references that still exist. |
| Output `0` or `2` | JSON with scheme version, modules, roots, roles, number of files and packages, declared and diagnosed exceptions. `0` indicates success and `2` indicates deviations in the complete inventory. |
| Exit `1` | The entrance or the perimeter is invalid. The detail is written on the stadr. |

The Client and the Suite apply the same rules of route of the package, imports, cohesion and dependencies. [Component samples](../../atf-tests/src/test/java/atf/tests/governance/support/JvmInventoryCliComponentTest.java) checks inventory and diagnostics on a separate project. Reactor compiling, test stratification and documentary governance have their own checks.

## Executable rules

| Area | Contract | Limit |
|---|---|---|
| Reactor | Source modules coincide with policy; common libraries remain binary | We are not copying the manufacturer's implementation or regression infrastructure. |
| Packages | Declaration corresponds to path; cohesion is checked separately on module and set of sources | The number of files does not measure semantic quality alone. |
| Coeziune | Maximum eight Java files per package; current inventory does not use derogations | A build up of responsibilities requires separation without increasing the threshold. |
| Importuri | No duplicate imports; comments and strings do not become code | Lexical verification uses the common parser. |
| Dependencies | Engine does not know Android, ATF or JUnit; infrastructure follows declared directions | Test Kit holds local JUnit integration; the implementation of the console and its native samples are consumed by binary delivery. |
| Niveluri | A Maven executable class has one level; the guide table corresponds to the code | The helping classes and Gradle modules have explicit policies. |
| Documentation | Structure, prose, style, diagrams, legends, links, fragments and relocatable paths | English wording and conceptual accuracy also require editorial review. |

The policy does not allow automatic threshold increase or the generation of derogations to achieve success. Negative evidence also checks the generic mechanism of exceptions: accurate identity of files, motive and separation action; an exception which is made unnecessary is rejected. These synthetic samples shall not enter derogations in the project.

## Test declarations

[Parserul sintactic](../../atf-tests/src/test/java/atf/tests/governance/source/JavaTestDeclarations.java) uses active JDK in memory without class generation or annotation processing. [Regulile nivelurilor](../../atf-tests/src/test/java/atf/tests/governance/source/TestLayerRules.java) consumes this structure and keeps separate the solution of compound annotations.

An inspection shall hold and close its own JDK file manager, reused between the sources of the same inspection. Each source has a new pregnancy and diagnosis; a syntax error does not contaminate the following source, and the closed context cannot be used any longer.

| Border | Contract |
|---|---|
| Declaration | The annotation position to modifiers, qualified form and spacing shall not change the owner. |
| Declared execution | Type methods and containers `@Nested` identify execution; an inner class used as fixture does not automatically label the outside. |
| Graf annotations | Explicit or wildcard imports, actual declarations and bounded cycles; ambiguities are denied. |
| Surse invalide | A syntax error produces a diagnostic with line and column without accepted empty inventory. |
| Limit | The inspection shall not load the types of the application, shall not execute JUnit or substitute for complete compilation or resolution of the legacy. |

[Componentele sintaxei](../../atf-tests/src/test/java/atf/tests/governance/source/TestLayerSyntaxTest.java) verifies valid refusals and neighbours, including Java examples in strings that should not be treated as declarations.

## Isolated rolling

For a file or full documentary inventory, use [Documentation Client](ATF_Documentation.md#documentation-governance-cli). It tests the same editorial rules as the suite.

To verify the published evidence, use a checkout Git with the commits items indicated in the release notes. The suite reads those local objects, and a missing object causes error.

From the root of the project:

```powershell
.\scripts\run-atf-governance.ps1
```

The launcher compiles and checks the same temporary copy. [Preflight-ul de calificare](../testing/ATF_ReleaseQualification.md#snapshot-and-preflight-before-maven) confronts the artifacts and locks the sources and release folder separately. The changes in checkout or copy invalidate the result.

The Maven running warehouse is the same as the temporary copy. The existing cachet can initialize dependencies through `-MavenCache`; installing libraries in `lib` does not replace files used by other processes.

For a working copy already prepared, with the required libraries and modules:

```powershell
mvn -B '-Dmaven.repo.local=<depozit-Maven-propriu>' -pl atf-tests -am '-Dtest=AtfQualityGovernanceSuite' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

`atf.governance.root` select the copy checked. `atf.governance.gitRoot` indicate the checkout used for [test diff. of test](ATF_TestChanges.md#checking-the-git-diff) and for historical objects of published evidence.

## Evidence and limits

- The native result keeps the class, rule and localised deviation.
- Negative evidence checks for an injection defect and acceptance of the healthy document next door.
- The inventory of sources does not ignore a valid package called `target` inside a compiled root.
- [Root hygiene](../../atf-tests/src/test/java/atf/tests/governance/source/workspace/WorkspaceHygieneGovernanceTest.java) refuses unrecognized directories, flattened local temporary paths and lost compiled files. Checking is only readers; cleaning requires inventory and resolved targets in the designated perimeter.
- [Single Strategy](../../atf-tests/src/test/java/atf/tests/governance/documentation/strategy/TestingStrategyGovernanceTest.java) keeps a regulatory owner, generic guides without results and an explanatory catalogue for each of the declared test roots.
- Documentary verification does not certify the game algorithm, Android rendering or compatibility of all libraries.
- Binary qualification remains the distinct contract of [integration of libraries](../platform/ATF_Integration.md).
- Semantic deficiencies and separation debt remain visible in [feedback](../feedback/ATF_Governance.md).
- [Retention of the evidence of the release](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) refuses identical PNGs already incorporated. The presence of bytes does not certify the viewer, the autonomy of Android bodies or the integrity of APK; they have separate controls.
- [Delivery integrity](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java) checks the APK, checksum, CRC, file hash inventory and recorded JUnit cases. Negative fixtures also alter files and recompute their checksums to test validation of their contents. [Contract and limits](../platform/ATF_AndroidDelivery.md#integrity-of-files-and-results) explains the separate checks for file integrity, test execution, signing and provenance.

[Test architecture](../testing/ATF_Architecture.md) · [Documentation](ATF_Documentation.md) · [Change of tests](ATF_TestChanges.md)

## Semantic revision

Structural checks must be completed by checks on factual claims that have been enforceable. Semantic revision remains mandatory for rules, compatibility and limits described in several summaries.

- [Package responsibilities](ATF_Packages.md) calls for the status, dependencies and invariance of each mechanism; moving a file alone does not demonstrate cohesion.
- [Good documentation practices](ATF_Documentation.md) holds the editorial grid and the separation of the external result from the contract.
- [Governance feedback](../feedback/ATF_Governance.md#qualification-of-claims-and-launchers) keeps the still necessary factual control and required level prior to closure.
