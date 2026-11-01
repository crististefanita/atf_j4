# J4 acceptance and architecture catalog

This module is owned by J4. It will hold cross-module architecture checks,
acceptance suites, and the J4 report composition. Consumer-specific suites live
in their `atf-test-*` modules. Common Core/Reporting contract tests stay in J0.

The Android device suite is opt-in and lives in `atf-test-android`; the default
build must not modify or interact with a connected device.
