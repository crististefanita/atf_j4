# Console - diagnostic for execution

## Overview

`atf-console` design ATF events in stdout/stderr: lifecycle, durates and configurable details. It is useful for tracking a Maven or IntelliJ run; HTML artifact is produced by [Reporting](README.Reporting.md).

## Minimal example

Fragment in a method; policy is explicit and the stream remains in the adaptor:

```java
import atf.console.AtfConsoleReporter;
import atf.console.ConsoleSettings;

var console = AtfConsoleReporter.configuredStdout(
        new ConsoleSettings(true, false));
console.info("The scenario catalog was loaded");
```

`configuredStdout()` read `atf.report.config` → `ATF_REPORT_CONFIG` → `atf.yaml/yml`; options are:

```yaml
reporting:
  console:
    enabled: true
    detailsEnabled: false
```

## Integration and options

| Intent | Choice |
|---|---|
| Independent message of details | `info(...)` |
| Typed events with lifecycle and duration | `publish(AtfConsoleEvent)` by port of Core `AtfConsoleSink` |
| Full console off | `enabled: false` or `ConsoleSettings.disabled()` |
| End of Console Report Path | Builder with `reportPathEnabled: true` and event `REPORT_PUBLISHED` |
| JUnit policy per class | JUnit Console Adapter and Test Kit annotations; declare Kit explicit |

The path requires eighty-in, active console and event with confirmed artifact; `detailsEnabled` controls only details.

Console depends on Core; Kit is optional for JUnit link. The projection remains diagnostic: the comparison and the verdict belong to the test.

## Verification of use

| Action | Observed result |
|---|---|
| Perform the example with explicit policy. | The message provided appears in stdout, including with the details disabled. |
| Disable console via `ConsoleSettings.disabled()`. | A new `info(...)` call does not produce a message in the stream. |
| Activate only the path display. | The path occurs only after the confirmed publication event; the option does not create the report. |

The compilation of the fragment qualifies API signature on the selected JAR. The control of the stream qualifies the message projection; the console does not change the test result or application rules.

---

The Console pair is an external binary fixed in the POM and the home registry. This guide verifies only the output contract consumed by the project; attach the version, hash and reproduction to [Feedback local](../docs/feedback/README.md).
