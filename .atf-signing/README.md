# Android test edition signing

## Overview

The public certificate Whist and the HA- 256 print identify the signatory of the test edition. Compare the print with the result `apksigner verify --print-certs` before qualifying an upgrade.

The keystore used for signing is kept locally. Folder only allows in Git documentation, certificate and fingerprint, so that a copied keystore here does not accidentally enter the index. [Android delivery guide](../docs/platform/ATF_AndroidDelivery.md)

Qualification exports only `.gitignore`, `README.md`, the public certificate `whist-android-test.pem` and its public fingerprint from `.atf-signing`. The [shared source policy](../scripts/AtfSigningSourcePolicy.ps1) excludes all other entries in that dedicated local directory before enumeration or hashing. Keep the private keystore and its protected backup outside source snapshots, reports and release assets. This boundary is not a general secret scanner for other source directories.

A local key may be present, absent or rotated without changing source identity. Changing the public certificate, public fingerprint or another source file changes that identity. The copier checks the complete manifest before writing: private signing paths, canonical aliases and indirect paths are refused. Signing still uses the local key explicitly; qualification does not delete or modify it.
