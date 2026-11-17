# Android test edition signing

## Overview

The public certificate Whist and the HA- 256 print identify the signatory of the test edition. Compare the print with the result `apksigner verify --print-certs` before qualifying an upgrade.

The keystoreul used for signing is kept locally. Folder only allows in Git documentation, certificate and fingerprint, so that a copied keystore here does not accidentally enter the index. [Android delivery guide](../docs/platform/ATF_AndroidDelivery.md)
