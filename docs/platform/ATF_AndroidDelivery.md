# Android delivery contract

## Overview

Delivery links pinned sources to APK identity and installation compatibility. Verify the declared version, produced binary and installed application separately.

## Responsibilities

```mermaid
flowchart LR
    SOURCE[Identified sources] -->|build in staging| BUILD[Gradle Android]
    BUILD -->|produce| APK[Signed APK]
    APK -->|checks| STATIC[Package and integrity]
    STATIC -->|allows installation| DEVICE[Installed application]
    STATIC -->|publish the checked binary| ASSET[Asset GitHub Release]
    ASSET -->|downloads and verifies hash| DEVICE
    DEVICE -->|check separately| UI[Android contracts]
```

<details>
<summary>Legend: build and behavior</summary>

The build produces the binary; static checks verify the package, SDK levels, signature and alignment. Device verification refers to the SHA-256 of the APK actually installed.

</details>

## Delivery conditions

- Read package identity, SDK levels and version from the application's [Gradle configuration](../../whist-lab-app/app/build.gradle).
- Build an isolated external copy with identified dependencies.
- Check Java API availability on supported Android levels; `sourceCompatibility` does not provide missing platform methods. For newer APIs, declare `coreLibraryDesugaringEnabled` and pin `desugar_jdk_libs` in each module requiring isolated verification. Governance compares API usage with that configuration.
- Check integrity, signature, alignment and key compatibility for upgrade.
- Keep APK and checksum in `release/<versionName>/`, next to the manifest and README. Store the evidence in the `reports` subdirectory. [Release catalog](../../release/README.md) identifies the current version. Keep intermediate runs outside the checkout.
- Verify the installed binary before attributing scenario behavior to that delivery.
- Separate local builds from store distribution and distribution-key management.

## Signing and migration plan

Qualify signing-key changes against the last APK available to users. Record package, `versionCode`, `minSdk`, APK SHA-256 and certificate SHA-256 from `apksigner verify --verbose --print-certs`. If the signer is `Android Debug`, recover the exact old key under controlled access before planning rotation. [Android app signing](https://developer.android.com/studio/publish/app-signing)

1. Prepare a delivery key with protected storage and a test copy. Keep the key, passwords and local configuration outside Git and reports. The build process receives an explicit signature configuration and stops if the identity cannot be verified. Publish only the fingerprints of the certificates and the verification data.
2. If the exact old key remains safe to use, build a candidate with the same `applicationId`, a higher `versionCode` and verifiable signing lineage. Set the rotation threshold explicitly: `apksigner` defaults to the new key from API 33; rotation from API 28 needs separate configuration and qualification. Below the threshold, updates still depend on the old signer; API 24–27 cannot use v3 rotation. [apksigner options](https://developer.android.com/tools/apksigner)
3. If the old key is missing or is no longer safe, stop publishing as a transparent upgrade for the affected installations. A migration to new installation requires a route for transferring saved data available for the installed version, fully checked prior to distribution. Treat any decision to change the package or the minimum Android level separately.

| Android profile | Required verification before publication |
|---|---|
| API 24–27 | Upgrade from the published APK with the compatible signatory. Check first installation and data retention. |
| API 28–32 | Upgrade and clean installation. Check the actual signatory for the selected rotation threshold. |
| API 33 and later | Upgrade and clean installation. Check the new signatory and the rotation history. |

- For each profile, create a saved match and an ongoing hand in the published version.
- Install the candidate over it, resume the match and compare preferences, score, history and next legal action.
- Record the previous version, API level, certificate fingerprints and installed APK hash.
- A missing required profile or lost saved data blocks public migration. Qualify the exact distributed binary.

## Compatibility and qualification

Qualify compatibility with a new Android version separately from choosing `targetSdk`. The APK's SDK value does not prove installation, display or stability on that system.

| Level | What it checks | Limit |
|---|---|---|
| Build and package | Sources, dependencies, manifest, SDKs, signature and complete APK. | Does not execute the app on every supported system. |
| JVM components | Window models, geometry, persistence and rules, with controlled inputs. | Do not execute system callbacks or actual gestures. |
| Identified Android device | Installation, restoration, window behavior, interaction and stability of the actual binary. | Qualifies only the declared system and profile, with recorded limits. |

## Distribution through GitHub

- **Download:** the main README advertises one APK. `currentVersion` selects the published edition; `candidateVersion` selects an announced test candidate.
- **Branch candidate:** `release/candidate.json` declares `PREPARED`, `artifactSource: MAIN_BRANCH` and anonymous access. Its directory contains the guide, APK and checksum; the direct GitHub link reads the binary from `main`.
- **Publication:** after qualification, the README links to the GitHub Release asset or verified public copy. The [release catalog](../../release/README.md) retains published editions and links to candidates separately.
- **Navigation:** GitHub renders repository files and documentation; the README provides a download link rather than an automatic HTTP redirect.

- **Identity:** the [release register](../../release/README.md) owns version, asset URLs and integrity evidence.
- **Channels:** authenticated downloads and anonymous access are qualified separately. Failed anonymous access does not invalidate an explicitly declared authenticated channel.

| Step | Acceptance criterion |
|---|---|
| Publication | The asset upload has the exact number of bytes of the signed APK; the checksums identify the same file. |
| Download | The asset is accessible through the declared channel, and the downloaded copy has the original size and SHA-256; for personal use check the authenticated browser. |
| Integrity | The ZIP archive passes CRC for all entries and contains the Android manifest and application code; signature and alignment are checked separately. |
| Installation | The check uses the complete copy, with the package identity and the release upgrade key. |

An HTTP error response is not an APK, regardless of the saved extension. Successful local build and installation do not prove public download; verify the received copy's integrity and provenance separately.

- Download to a temporary file and rename it within the same directory only after verification. On failure, remove only that transfer's temporary file and preserve the original cause. Download and compare the GitHub asset before handoff.

- Qualify authenticated and anonymous access separately. `visibility=public`, `uploaded` and a matching asset digest do not prove browser download. Retain HTTP status, byte count and received SHA-256; keep access failures open until the required channel succeeds.

## Separating access from Android transfer

- **Navigation.** The README opens the current GitHub release. Under **Assets**, select the APK, which is the same versioned file as the direct link.
- **Access:** HTTP 404 on a repository or asset is a separate access failure. Compare authenticated and anonymous requests; never claim successful Android transfer from failed HTTP access.
- **Native transfer:** an Android Download Manager test records system, access mode, status and reason, received bytes, SHA-256, CRC and PackageManager identity. Android must perform the network transfer; pushing a desktop-downloaded copy is not equivalent.
- **Credentials:** keep API tokens on the computer. A temporary authorized asset redirect can support the native test without a global device token; never retain its signed URL or query in docs or reports.
- **Signature:** on API 28, read archive signing information through both supported paths. Reject absent or unexpected signers and compare the exact certificate. Successful transfer cannot override signature-verifier failure.
- **Audit:** retain the transfer result JSON and test sources inline in Extent for the captured execution. Keep failed attempts separately as diagnostics; republishing evidence does not create a new execution or JUnit invocation.
- **Limit:** native transfer qualifies the file received on Android. The README → authentication → Assets → saved-file browser route requires a separate check. Remove the temporary test app and release the lease afterward.

## Process of a version

[Qualification guide](../testing/ATF_ReleaseQualification.md) orders test selections, HTML review and candidate preparation. This guide owns build, identity and publication.

[GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases) binds delivery to a tag and attached binaries. The APK is an asset; local `release/` retains the verified dossier without creating a GitHub release or HTTP redirect.

1. Identify source commit and application version. Push to the release branch and verify after `git fetch` that `git merge-base --is-ancestor <commit> origin/<branch>` succeeds. The tag must resolve to that exact commit.
2. Pin the external build copy, then build and sign once. Check package identity and relevant scenarios against that same binary.
3. Prepare a draft for the tag with APK and checksum; compare asset identity with the delivery register.
4. Publish the full version and mark it as the latest version; keep the binary published stable and corrections receive a new version.
5. Verify download through the declared channel and update the register, links and evidence dossier.

[GitHub recommends creating a draft before attaching assets and publishing](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository) immutable releases. This process follows that order; distinguish the project policy from GitHub's native immutability setting.

## Maintenance releases

A maintenance release uses checks selected from the actual code diff. Version numbering alone never justifies fewer checks; rule, save, SDK or dependency changes can require additional qualification. Governance reads release snapshots and candidate metadata while keeping parallel work separate.

| Control | Minimum for patch | When it expands |
|---|---|---|
| Sources and build | Identified commit, external copy, `assembleRelease` and `lintVitalRelease`. | Changed dependencies, platform or build configuration. |
| Behavior | Components and regressions for the changed area, including relevant rejection cases. | Changed rules, scoring, persistence or AI policy; selection follows the affected contract. |
| Governance | Release layout and presentation, plus links and style for affected documents. | Architecture or contract changes add their owning controls. |
| APK | Identity, SDKs, CRC, signature, alignment, upgrade key compatibility and SHA-256. | Manifest, install or compatibility Android changed; add native smoke. |
| Interface | Geometry components and focused native sanity for the changed interaction when required. | New profiles or transitions require distinct acceptance; do not repeat a full match at every resolution automatically. |
| Distribution | Asset and checksum, followed by a download through the declared channel with matching size and SHA-256. | Channel or signing changes require renewed distribution checks. |

Selected tests must pass; a relevant failure blocks publication. Record missing native checks and reasons in the technical dossier, without inheriting prior APK qualification or adding diagnostic noise to the release page.

### Hand over the patch

1. Update `versionName` and increment `versionCode` in Gradle; pin the built commit. Compare relevant parallel source changes before publication.
2. Run the selected checks in external staging; prepare commands, JUnit results and logs for `release/<versionName>/reports/`, with a short reading guide. Extent reports derive from actual executions; label historical references explicitly.
3. Place the audit copy of the signed APK `WhistLab-<versionName>.apk` and checksum in `release/<versionName>/`; keep the current and immediately preceding edition. Archive older editions outside the active catalogue only after verifying their identities and hashes. The README links to the GitHub release asset. Use the retained copy to verify integrity.
4. Update the new version's manifest and README, `release/index.json`, catalog and bullet-point notes against the previous version. Change generic guides only when their process or contract changes.
5. Run governance checks before publishing the tag and verified assets. Download the full APK from the advertised channel. Update the README links after verifying the version, size, SHA-256 and CRC. A versioned link identifies the qualified APK, while `releases/latest` does not pin its identity.

Keep draft metadata separately in `github-release-draft.json`; `github-release.json` describes actual publication. Draft URLs may contain `untagged`, so recheck published note links against canonical asset URLs after publication. Supply the tag and source commit explicitly and compare the response with APK identity.

Before creating a draft, the publication loader checks the new notes: one opening summary of at most 400 Unicode characters and separate change bullets of at most 300 characters each. Invalid notes stop before GitHub writes; published historical notes remain immutable. The [publication components](../../atf-tests/tools/publish-github-release.test.mjs) cover both languages and a hash-valid but editorially invalid candidate.

## Source and evidence commits

- **Sources:** the tag pins the qualified source commit. Compare all APK inputs, including new files; later documentation or evidence changes do not move the tag.
- **Evidence:** verify the actual download after publication and commit that evidence separately. Release notes pin this commit in `evidenceUrl`; metadata retains `sourceCommit` independently.
- **Linking:** The evidence commit exists before its hash is added to the release notes. A final commit records the link and the GitHub response. It does not attempt to refer to its own hash.
- **Audit identity:** [Release presentation governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java) reads the index, manifest and direct report at the indicated commit. It compares the SHA-256 with the canonical report declared in the delivered dossier. If a rerun changes the canonical report, pin the new evidence commit in the release notes.
- **Closure:** After the final push, read the README on GitHub again and compare the exact URL with the downloaded asset. A local check alone cannot establish that the online document has been updated.

## Resumable publishing on GitHub

[Publication script](../../atf-tests/tools/publish-github-release.mjs) takes a candidate directory and its `READY_FOR_HUMAN_REVIEW` verdict.

- The manifest declares `PREPARED`, repository, access mode, canonical URL and hashes for the APK, checksum and notes.
- Push sources to `main` and the version tag to the qualified commit before invoking publication.
- Use `GH_TOKEN` or `GITHUB_TOKEN` in the process environment. Keep the publication run directory outside the project and candidate.

```powershell
node .\atf-tests\tools\publish-github-release.mjs publish $ProjectRoot $CandidateDir $VerdictFile $PublicationRun
```

Resume with the same qualified manifest, verdict and original run directory. Retain them until publication and retention checks finish.

- The `github-publication.json` journal binds commit, manifest hash, asset hashes and numeric draft/asset IDs. Persist intent before calls with potentially ambiguous outcomes.
- Atomic writes and `.lock` prevent concurrent publication in the same run directory. After a forced stop, check the recorded PID before removing a stale lock.

| Situation | Resume behavior |
|---|---|
| GitHub returns 422 during creation | Reread authenticated inventory. If no matching draft exists, correct the cause before retrying. |
| Connection lost during creation | Search every page by tag and manifest marker. If still uncertain, inspect authenticated inventory and resume once the draft is observable. After saving IDs, use those exact IDs. |
| Upload interrupted or returns 422 | Reread the asset, download and compare size and SHA-256. Different bytes stop publication. Remove an empty `starter` asset only by its recorded ID in the owned draft when the journal identifies that interrupted upload. |
| Promotion response lost | Reread the same release ID and verify published assets. Do not repeat confirmed promotion. |

Preserve the `atf-publication` HTML marker when adding the commit-pinned audit link. After promotion, push the updated catalog, manifests and README to `main`. [Rotation preflight](ATF_ReleaseRetention.md#download-verification-before-rotation) compares them against actual downloads.

## Folder of a version

[Release catalog](../../release/README.md) retains the current and immediately previous editions in `release/<versionName>/`. Each version's presentation and metadata link to `reports/`. [Retention policy](ATF_ReleaseRetention.md) defines replacement, archiving and explicitly requested GitHub reset.

| Entry point | Responsibility |
|---|---|
| `release/index.json` | Schema 2 declares `currentVersion` and at most two `versions`, current first; primary links identify that version. |
| `release/README.md` | Stable catalog and entry point to active delivery. |
| `release/<versionName>/README.md` | Presents the edition, identified APK, state of handoff and links to metadata and reports. |
| `release/<versionName>/reports/README.md` | Explains selected tests, reports, results and qualification limits. |
| Manifest and directory metadata | Bind `versionName`, tag, assets, checksums and notes; relative paths start at the version directory. |
| Reports, captures, results and logs in `reports/` | Retain identified executions, negative diagnostics and labeled copies; local links start at that subdirectory. |
| [Test strategy](../testing/README.md) | Owns shared selection principles independently of release versions. |

- Qualify the new external candidate before retiring editions older than the immediate predecessor. Update catalog and index after identity verification; canonical `release/` retains both editions and their APKs.
- Move reports and supporting files together, compare hashes and repair active references. Preserve the meaning of captured absolute paths and historical commit URLs.
- `artifacts/` and `dist/` are retired root directories. After preserving unique data, remove them, including empty directories; governance rejects their return.
- Start release descriptions with a short summary and separate change bullets. Review them before sealing the manifest.
- GitHub Release notes link to the version README at an identified commit. Navigation updates neither move the tag nor replace the binary asset.

[Release-layout governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactLayoutGovernanceTest.java) rejects unregistered files, excess active versions, missing metadata and divergent identities. Integrity and link checks complete this structural contract.

### Main link and download test

| Situation | Presentation and verification |
|---|---|
| Local candidate | Exact `release/<versionName>/` directory link, `PREPARED` state and local APK; no claim of GitHub publication. |
| Asset published | Direct link `releases/download/<tag>/<apkName>`. The label, tag and file identify the same version. |
| Authenticated GitHub asset | `PUBLISHED` with `accessMode=AUTHENTICATED`; `authenticatedDownload` proves an account session through a RAG-approved channel. The API records HTTP 200; Browser MCP records completed download. Both verify URL, bytes, SHA-256 and CRC without retaining credentials. |
| Verified public copy | `PUBLISHED_SITE` with `accessMode=ANONYMOUS`; `publicDownload` verifies page, APK and checksum over HTTPS without credentials. |
| Complete copy | Successful response, exact size and SHA-256, valid ZIP CRC, Android manifest and application code. Never rename an error response to `.apk`. |
| Verified channel | Each receipt qualifies only its declared access mode. Authenticated success does not prove anonymous access; a public copy does not prove GitHub account permissions. |
| Unique identity | `currentVersion`, manifest, label, APK name and URL identify the same edition. Evidence of any previous version remains only audit references. |

Download under a temporary name and assign the final extension only after verification. On transition to `PUBLISHED`, replace the local link with the same version's distribution URL.

### Anonymous access and public route

1. After publishing, open the delivered README in a signed-out browser. Follow its APK link and download the APK and checksum without cookies, an Authorization header or a token. Compare the size and SHA-256 with `release-validation.json` and the published checksum.
2. On HTTP 404, check repository, README, release page and asset separately. Compare anonymous and owner-session responses. If the profile or other public repositories are also inaccessible, retain URLs, UTC time, HTTP status and request IDs for [GitHub Support](https://docs.github.com/en/support/contacting-github-support/creating-a-support-ticket). One anonymous 404 does not establish the cause.
3. If GitHub remains inaccessible without signing in, publish the signed APK and checksum on a public HTTPS host controlled by the project. The stable APK URL ends in `/v<versionName>/WhistLab-<versionName>.apk` and contains no query string or fragment. If the GitHub README is also inaccessible, publish an identified copy on the public site and verify its link. Republish the copy when the download version or destination changes.
4. Record the declared mode in `release/<versionName>/reports/distribution-verification.json`: `authenticatedDownload` for authenticated GitHub downloads or `publicDownload` for an anonymous site. The authenticated channel and its proof must match `config/release-platform-rag-policy.json`. The API records `httpStatus=200`, and Browser MCP records `downloadCompleted=true` after the download completes. Compare the resulting file. Do not store cookies or authorization headers. Retain only verifiable metadata. Update the main link, regenerate the dossier integrity record and run governance. After the push, read the README again and download from that exact link again using the same access mode.

Run from the project root and supply the direct URL copied from the published README.

```powershell
$version = (Get-Content release/index.json -Raw | ConvertFrom-Json).currentVersion
$expected = (Get-Content "release/$version/release-validation.json" -Raw | ConvertFrom-Json).release
$downloadUrl = Read-Host 'URL-ul direct din README'
$payload = [IO.Path]::GetTempFileName()
try {
    $status = curl.exe -q --fail --location --silent --show-error --max-time 60 `
        --proto '=https' --proto-redir '=https' --output $payload --write-out '%{http_code}' -- $downloadUrl
    if ($LASTEXITCODE -ne 0 -or $status -ne '200') { throw "Download returned HTTP $status" }
    $bytes = (Get-Item -LiteralPath $payload).Length
    $sha256 = (Get-FileHash -LiteralPath $payload -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($bytes -ne $expected.apkBytes -or $sha256 -ne $expected.apkSha256) {
        throw 'Downloaded APK differs from the manifest'
    }
    [pscustomobject]@{ HttpStatus = [int]$status; Bytes = $bytes; Sha256 = $sha256 }
} finally {
    Remove-Item -LiteralPath $payload
}
```

### Automatic distribution checks

| Control | Purpose and limit |
|---|---|
| [Link governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactLayoutGovernanceTest.java) | Same version in catalog, label and APK; rejects ambiguous `latest`, Raw/blob links, competing downloads, old README APKs and retired directories. |
| [Download components](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/download/ApkDownloadComponentTest.java) | Real loopback HTTP: complete download, 404, text masquerading as APK, truncation, excess bytes, wrong SHA-256 or CRC, divergent metadata and credentials not forwarded across origins. ZIP fixtures do not qualify Android installation. |
| [Identity selection](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/download/DownloadSelectionComponentTest.java) | Prior-version success does not qualify the current candidate's download. Byte counts are read exactly without lossy conversion. |
| [Live acceptance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/download/PublishedApkDownloadAcceptanceTest.java) | Explicit GitHub download with version, bytes, SHA-256, CRC and Extent verdict. Targets only `currentVersion`; missing publication yields HTTP 404 and FAIL without falling back to an older APK. |

With available dependencies in an external copy, run components without GitHub access:

```powershell
mvn -B -o -pl atf-tests -am '-Dtest=ReleaseArtifactLayoutGovernanceTest,ApkDownloadComponentTest,DownloadSelectionComponentTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

After publication, check the current APK via separate network sample:

```powershell
mvn -B -o -pl atf-tests -am '-Dtest=PublishedApkDownloadAcceptanceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Datf.release.download.enabled=true' '-Datf.release.download.scope=current' test
```

- **Channel:** read tokens from `GH_TOKEN` or `GITHUB_TOKEN`, never arguments, YAML or reports. The test neither publishes nor installs the app.
- **Scope:** only `atf.release.download.scope=current` is accepted. Identity and size come from the current manifest, including `PREPARED`; the test checks actual publication.
- **Report:** `atf.release.download.report-directory` requires an absolute path and defaults to TMP. Publication failures remain FAIL without inheriting an older subset's PASS.
- **Offline suite:** normal governance excludes `release-download-live`; explicitly select network acceptance without treating its omission as a pass.

- **Inventory: ** structured index, folders and catalogue form the same population checked.
- **Qualification:** structure checks and [release-note asset binding](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java) are separate controls. Their success does not prove channel access, full Git provenance or report autonomy.

### Integrity of files and results

[Integrity governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java) reads the APK and active dossier without modifying them. It compares canonical path, size, SHA-256 and checksum, reads every ZIP entry for CRC, and rejects duplicate names or unsafe paths.

| Verified link | Contract |
|---|---|
| Manifest–payload | `integrity.files` lists every payload file, size and SHA-256, excluding the manifest itself. Added, removed or modified files invalidate the observation. |
| Manifest–JUnit | Each group declares exactly its retained XMLs. Recount cases, require unique class/name identity, and compare XML attributes with declared totals. |
| Manifest–audited HTML | Audited groups declare canonical HTML and an execution receipt. Recount invocations, comparisons and verdicts under their owner; do not retain parallel XML reporting for the same selection. The schema distinguishes older, unmigrated results. |
| Qualified result | Actual cases and zero failures, errors or omissions are required; an empty container does not invent tests. A zero-failure header does not hide an item `failure`, `error` or `skipped`. |
| Manifest–inputs | The source-input file is hash-inventoried; declared fingerprint and commit agree, and paths and hashes are valid and unique. |

Record hashes explicitly after finalizing payload files:

```powershell
node atf-tests/tools/record-release-integrity.mjs
```

- **Recording:** the tool selects the indexed version, verifies inventory stability and updates `integrity`. `observedUtc` records byte observation, not a new test execution, verdict or publication state.
- **Git:** the [`text` attribute](https://git-scm.com/docs/gitattributes) is disabled by `.gitattributes` throughout `release`. Governance checks the effective attribute of each payload, including local overrides; before push, Git object bytes must match the manifest.
- **Verification:** run governance after recording. Rehashing cannot make failed JUnit results or a corrupt APK valid.
- **Limits:** CRC and SHA-256 prove local integrity separately from APK signing, binary-manifest identity, reproducible build and published-commit identity. This inventory does not by itself compare the full external archive or live-source population.
- **Catalog:** validate section boundaries before resolving the current entry; diagnose missing or reversed headings. A version in the general table does not replace its link in the current section.

## Presentation of a version

The release page addresses users. Keep three bullet-based sections and link to the audit dossier:

| Section | Content |
|---|---|
| Download | Browser login, APK direct link and version checksum. |
| Improvements compared to the previous version | Confirmed changes for the published binary with the version of comparison identified. |
| Documentation | The link to reports and provenance in an identified commit. |

- **Opening summary:** new publications begin with one paragraph of at most 400 Unicode characters. Keep each confirmed change in a separate bullet of at most 300 characters; validate this layout before creating a GitHub draft.
- **Comparison:** when differences from the previous version are unverified, use **Overview** and describe only known capabilities. Do not invent history; collect the next release's changes during development.
- **Audit:** counts, technical verdicts, untested-device limits and distribution diagnostics belong in the evidence dossier. Link to them without overloading product presentation.

[Presentation governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java) compares notes with assets and catalog: bullets, APK destination, checksum, audit link and declared comparison or explicit overview. Negative cases cover Raw links, wrong versions, missing comparison baseline and test diagnostics in product notes.

## New Android window and versions

[Android documentation](https://developer.android.com/develop/ui/views/layout/edge-to-edge) describes edge-to-edge behavior for apps targeting SDK 35 or later on Android 15 and newer. [WindowContent](../../whist-lab-app/app/src/main/java/ro/whistlab/app/window/WindowContent.java) separates inset protection from window measurement:

| Code | Implemented behaviour |
|---|---|
| `protect`, API 35+ | Adds system bars, cutouts and IME to original padding and consumes the insets. Stored base padding prevents accumulation on repeated application. |
| `widthDp` / `heightDp`, API 30+ | Measures the current window excluding bars and cutouts regardless of visibility. IME is not subtracted here; root protection has its own path. |
| Previous systems | Measurement uses platform configuration; the protection listener is not installed by this class before API 35. |

[Window components](../../whist-lab-app/app/src/test/java/ro/whistlab/app/window/WindowPaddingComponentTest.java) verify arithmetic and layout budgets. [Native acceptance](../feedback/ATF_Android.md#native-acceptance-on-android-17) checks the actual viewport, gestures, three-button navigation, clipping and keyboard; the JVM model cannot establish those results.

## Memory and Diagnosis on Android 17

[Android 17 / API 37](https://developer.android.com/about/versions/17/behavior-changes-all) applies memory limits even to apps targeting older versions. Identify limiter involvement through `ApplicationExitInfo`, `REASON_OTHER` and a description containing `MemoryLimiter:AnonSwap`.

- Identify the APK, system, table profile and application status of the same execution.
- Record baseline memory and its evolution during resume and across hand sizes; AI search cost and responsiveness require their own measurements.
- Correlate exit reasons with Android runtime logs, process identity and execution interval. An isolated code or message is insufficient.
- Preserve system acceptance limits and explicitly record incomplete diagnosis. JVM calculations do not qualify Android process memory.

## Data restoration

1. Export existing application saves before intervention.
2. Check the integrity of the export prior to installation, reset or cleaning.
3. Treat reading error as error without interpreting it as data absence.
4. Verify restoration and schema compatibility before removing the backup.
5. State limits for missing migration information explicitly.

[Application build](../../whist-lab-app/README.md) · [Android testing](../testing/ATF_Android.md) · [Score history](../game/ATF_ScoreHistory.md)
