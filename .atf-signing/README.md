# Android test edition signing

## Overview

The public Whist certificate and its SHA-256 fingerprint identify the test-edition signer. Compare the fingerprint with `apksigner verify --print-certs` before qualifying an upgrade.

The private keystore stays local and ignored by Git. The [Android delivery guide](../docs/platform/ATF_AndroidDelivery.md) explains signing and upgrade identity.

## Source qualification

| Input | Export rule |
|---|---|
| `.gitignore`, `README.md`, `whist-android-test.pem`, `whist-android-test.fingerprint` | Public signing contract: inventoried, hashed and copied. |
| Other entries under `.atf-signing` | Local private material: excluded before reading content or traversing directories. |
| Sources outside `.atf-signing` | Normal source rules still apply; this boundary is not a general secret scanner. |

- The [shared source policy](../scripts/AtfSigningSourcePolicy.ps1) keeps local keys and backups out of source snapshots, reports and release assets.
- A local key may be present, absent or rotated without changing source identity. Changing public metadata or another source file changes that identity.
- Copying checks the entire manifest before writing and refuses private paths, canonical aliases and indirect paths. Signing uses the local key explicitly; qualification does not delete or modify it.
