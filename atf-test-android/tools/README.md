# Uneltele infrastructurii Android

## Overview

Tools belong to Android infrastructure. PowerShell module checks ADB transport, and Python tools consume explicitly captured data. The player checks the rules; the teaching links the results descriptor and keeps external pointeres on purpose.

## Liabilities

| Source | Contract |
|---|---|
| [AndroidDeviceSetup.psm1](AndroidDeviceSetup.psm1) | Running ADB commands, connecting the transport and observing the boot, API and actual display. [Preflight Android](../../docs/testing/ATF_ReleaseQualification.md#preflight-android) |
| [whist_ui_replay.py](whist_ui_replay.py) | Projections of public observation and independent verification of rules and score. |
| [whist_ui_handoff.py](whist_ui_handoff.py) | The overflow republishment, the integrity of the descriptor and acceptance on purpose. |
| [test_whist_ui_handoff.py](test_whist_ui_handoff.py) | Refusing corruption and keeping acceptance to failures or old republics. |

## Verification

| Surface | Profit test | Limit |
|---|---|---|
| Transport PowerShell | Parsing the module and, separately, connecting by `Connect-AtfAndroidDevice`. | Parsing does not exercise the timeout, process errors or device availability. |
| Replay and surrender | [Contractele Python](test_whist_ui_handoff.py), with directories and records controlled. | Check the captured data and republish without new Android interactions. |
| Clientul Java ADB | [AdbClientContractTest](../src/test/java/atf/android/AdbClientContractTest.java), with a controlled executor. | Qualify that client; PowerShell module has separate implementation. |

From the root of the project, with a real available Python 3 interpreter:

```powershell
python -m unittest discover -s atf-test-android/tools -p test_whist_ui_handoff.py
```

Python imports are solved between files of the same directory. The results and pointeres are external to the project; a republican republics the identity of the original execution.

The ADB connection can change the transport; it runs in the Android stream holding the reservation and configuration. [Qualification of launchers](../../docs/feedback/ATF_Governance.md#qualification-of-claims-and-launchers) keeps the conditions of the prior check and diagnosis separate from the Python samples.

[Teaching results](../../docs/reporting/ATF_Handoff.md) · [Publication audit](../../docs/reporting/ATF_Audit.md) · [Modulul Android](../README.md)
