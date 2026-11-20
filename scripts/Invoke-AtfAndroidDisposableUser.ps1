#requires -Version 7.0
param(
    [Parameter(Mandatory)][ValidateSet('Prepare','Teardown','Recover')][string]$Phase,
    [Parameter(Mandatory)][string]$RunRoot,
    [Parameter(Mandatory)][string]$Serial,
    [Parameter(Mandatory)][string]$AdbExecutable,
    [Parameter(Mandatory)][string]$LeasePath,
    [Parameter(Mandatory)][string]$LeaseId,
    [string]$ServerSocket = '',
    [string]$ApkPath = '',
    [string]$ApkSha256 = '',
    [ValidateRange(1,999)][int]$ExpectedApi = 28,
    [string]$ExpectedDisplayProfile = '1080x1920@440',
    [string]$OwnershipJournal = ''
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'AtfLocalPaths.ps1')
. (Join-Path $PSScriptRoot 'AtfAndroidPreflight.ps1')
. (Join-Path $PSScriptRoot 'AtfAndroidPackageRegistration.ps1')
. (Join-Path $PSScriptRoot 'AtfAndroidUserReadiness.ps1')
. (Join-Path $PSScriptRoot 'AtfAndroidUserOwnership.ps1')
Import-Module (Join-Path $PSScriptRoot '../atf-test-android/tools/AndroidDeviceSetup.psm1') -Force
$run = [IO.Path]::GetFullPath($RunRoot)
$sourceRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$sourceRootPrefix = $sourceRoot.TrimEnd([char]'\',[char]'/') + [IO.Path]::DirectorySeparatorChar
if ($run.Equals($sourceRoot,[StringComparison]::OrdinalIgnoreCase) -or
    $run.StartsWith($sourceRootPrefix,[StringComparison]::OrdinalIgnoreCase)) {
    throw 'RunRoot must stay outside the ATF checkout; use external evidence storage.'
}
if ($Phase -eq 'Prepare' -and $ApkPath) {
    $apkFullPath = [IO.Path]::GetFullPath($ApkPath)
    if ($apkFullPath.Equals($sourceRoot,[StringComparison]::OrdinalIgnoreCase) -or
        $apkFullPath.StartsWith($sourceRootPrefix,[StringComparison]::OrdinalIgnoreCase)) {
        throw 'Candidate APK must stay outside the ATF checkout; use external release staging.'
    }
}
if (-not (Test-Path -LiteralPath $run -PathType Container)) { throw 'RunRoot must already exist outside the source tree.' }
Assert-AtfUserOwnershipPath $run
$setupFile = Join-Path $run 'setup-release.json'
$readyFile = Join-Path $run 'data-isolation-ready.json'
$cleanupFile = Join-Path $run 'data-isolation-cleanup.json'
$leaseReceipt = Join-Path $run 'device-lease.json'
$packageName = 'ro.whistlab.app'
if (-not $OwnershipJournal) {
    $evidence = Get-AtfEvidenceRoot
    $store = Join-Path $evidence '.imp/local/android-ownership'
    $OwnershipJournal = Join-Path $store ($LeaseId + '.json')
}
$OwnershipJournal=[IO.Path]::GetFullPath($OwnershipJournal)
if ($OwnershipJournal.StartsWith($sourceRootPrefix,[StringComparison]::OrdinalIgnoreCase)) { throw 'OwnershipJournal must stay outside the ATF checkout.' }
Assert-AtfUserOwnershipPath $OwnershipJournal
$null=New-Item -ItemType Directory -Path (Split-Path -Parent $OwnershipJournal) -Force

function Invoke-DeviceShell([string[]]$Arguments,[ValidateRange(1,120)][int]$TimeoutSeconds=30) {
    Get-AtfActiveDeviceLease $LeasePath $LeaseId $Serial | Out-Null
    $result = Invoke-AtfAdbCommand -AdbExecutable $AdbExecutable -Arguments (@('-s',$Serial,'shell') + $Arguments) `
        -ServerSocket $ServerSocket -TimeoutSeconds $TimeoutSeconds
    if ($result.ExitCode -ne 0) { throw "ADB shell failed ($($Arguments -join ' ')): $($result.Output)" }
    $result.Output.Trim()
}
function Get-CurrentUserId {
    $value = Invoke-DeviceShell @('am','get-current-user')
    if ($value -cnotmatch '^[0-9]{1,3}$') { throw "Unexpected active Android user ID: $value" }
    [int]$value
}
function Test-UserExists([int]$UserId) {
    $users = Invoke-DeviceShell @('pm','list','users')
    $users -match ("(?m)UserInfo\{" + [regex]::Escape([string]$UserId) + ":")
}
function Get-PackagePath([int]$UserId) {
    $value = Invoke-DeviceShell @('cmd','package','path','--user',[string]$UserId,$packageName)
    $match = [regex]::Match($value,'(?m)^package:(/\S+\.apk)\s*$')
    if (-not $match.Success) { throw "Installed APK path is not observable for Android user $UserId." }
    $match.Groups[1].Value
}
function Test-PackageInstalled([int]$UserId) {
    $value = Invoke-DeviceShell @('pm','list','packages','--user',[string]$UserId,$packageName)
    $lines = @($value -split "`r?`n" | ForEach-Object { $_.Trim() })
    if (@($lines | Where-Object { $_ -and $_ -cnotmatch '^package:[a-zA-Z0-9_.]+$' }).Count) {
        throw 'ANDROID_USER_PACKAGE_LIST_UNAVAILABLE'
    }
    $lines -ccontains "package:$packageName"
}
function Unlock-DisposableUser {
    Invoke-DeviceShell @('input','keyevent','KEYCODE_WAKEUP') | Out-Null
    Invoke-DeviceShell @('input','swipe','540','1750','540','500','300') | Out-Null
    Invoke-DeviceShell @('wm','dismiss-keyguard') | Out-Null
    $unlockDeadline = [DateTime]::UtcNow.AddSeconds(15)
    do {
        $windowPolicy = Invoke-DeviceShell @('dumpsys','window','policy')
        if ($windowPolicy -match '(?m)\bisStatusBarKeyguard=false\b') { return }
        Start-Sleep -Milliseconds 250
    } while ([DateTime]::UtcNow -lt $unlockDeadline)
    throw 'The disposable Android user remains behind the keyguard after wake, swipe and dismiss requests.'
}
function Write-JsonFile([string]$Path,[object]$Value) {
    [IO.File]::WriteAllText($Path,($Value | ConvertTo-Json -Depth 8) + "`n",[Text.UTF8Encoding]::new($false))
}

function Complete-OwnedProfile {
    $lease = Get-AtfActiveDeviceLease $LeasePath $LeaseId $Serial
    if (-not [IO.Path]::GetFullPath([string]$lease.run).Equals($run,[StringComparison]::OrdinalIgnoreCase)) {
        throw 'ANDROID_USER_OWNERSHIP_LEASE_RUN'
    }
    Complete-AtfOwnedUser -Path $OwnershipJournal -Serial $Serial -RunRoot $run -LeaseId $LeaseId -ReadUsers {
        ConvertFrom-AtfAndroidUsers (Invoke-DeviceShell @('pm','list','users'))
    } -ReadCurrent { Get-CurrentUserId } -SwitchOriginal { param($id)
        Invoke-DeviceShell @('am','switch-user',[string]$id) | Out-Null
        $deadline=[DateTime]::UtcNow.AddSeconds(30)
        while ((Get-CurrentUserId) -ne $id -and [DateTime]::UtcNow -lt $deadline) { Start-Sleep -Milliseconds 250 }
    } -RemoveUser { param($id) Invoke-DeviceShell @('pm','remove-user',[string]$id) | Out-Null
    } -ReadOriginalPackage { param($id) Test-PackageInstalled $id }
}

if ($Phase -ceq 'Recover') {
    $recovered = Complete-OwnedProfile
    Write-JsonFile (Join-Path $run ('data-isolation-recovery-' + $LeaseId + '.json')) ([ordered]@{
        status='RECOVERED';applicationVerdict='NOT_RUN';serial=$Serial;creationLeaseId=$recovered.leaseId
        recoveryLeaseId=$LeaseId;originalUserId=$recovered.originalUserId;removedUserId=$recovered.testUserId
        ownershipJournal=$OwnershipJournal;observedUtc=[DateTime]::UtcNow.ToString('o')
    })
    return
}

if ($Phase -ceq 'Prepare') {
    if (-not $ApkPath -or -not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) { throw 'Prepare requires an existing candidate APK.' }
    if ($ApkSha256 -cnotmatch '^[0-9a-f]{64}$' -or (Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash.ToLowerInvariant() -cne $ApkSha256) {
        throw 'Candidate APK bytes do not match the requested SHA-256.'
    }
    foreach ($file in @($setupFile,$readyFile,$cleanupFile,$leaseReceipt)) {
        if (Test-Path -LiteralPath $file) { throw "Refusing to overwrite an existing run receipt: $file" }
    }

    $createdUser = $null
    $ownershipCreated = $false
    $arguments = @{
        AdbExecutable=$AdbExecutable; Serial=$Serial; AdbServerSocket=$ServerSocket
        LeasePath=$LeasePath; LeaseId=$LeaseId; ExpectedApi=$ExpectedApi; ExpectedDisplayProfile=$ExpectedDisplayProfile
        Receipt=(Join-Path $run 'android-preflight.json')
    }
    $observedPreflight = & (Join-Path $PSScriptRoot 'check-android-release-device.ps1') @arguments
    $readiness = $observedPreflight.observed

    $originalUser = Get-CurrentUserId
    if (-not (Test-UserExists $originalUser)) { throw "Original Android user $originalUser is not present." }
    $originalPackageWasInstalled = Test-PackageInstalled $originalUser
    Assert-AtfAndroidPreflight $observedPreflight $LeasePath $LeaseId $Serial $ServerSocket $ExpectedApi $ExpectedDisplayProfile
    try {
        $ownership = New-AtfUserOwnership $OwnershipJournal $Serial $LeaseId $run $originalUser $originalPackageWasInstalled `
            @(ConvertFrom-AtfAndroidUsers (Invoke-DeviceShell @('pm','list','users')))
        $ownershipCreated = $true
        $name = $ownership.userName
        $created = Invoke-DeviceShell @('pm','create-user',$name)
        $idMatch = [regex]::Match($created,'(?i)Success: created user id\s+(\d+)')
        if (-not $idMatch.Success) { throw "Android did not create a disposable secondary user: $created" }
        $createdUser = [int]$idMatch.Groups[1].Value
        if ($createdUser -le 0 -or $createdUser -eq $originalUser) { throw 'Android returned a non-disposable user ID.' }
        $ownership = Set-AtfOwnedUserId $OwnershipJournal $Serial $run $createdUser
        $actual = @(ConvertFrom-AtfAndroidUsers (Invoke-DeviceShell @('pm','list','users')) | Where-Object id -EQ $createdUser)
        if ($actual.Count -ne 1 -or $actual[0].name -cne $name) { throw 'ANDROID_USER_OWNERSHIP_FOREIGN_ID' }

        # A new Play Store profile starts these background services together and can stall System UI.
        $installed = Invoke-DeviceShell @('pm','list','packages','--user',[string]$createdUser)
        $quieted = @()
        foreach ($background in @('com.google.android.gms','com.google.android.gsf','com.android.vending',
                'com.google.android.googlequicksearchbox','com.google.android.apps.docs','com.google.android.apps.maps',
                'com.google.android.calendar','com.google.android.setupwizard','com.google.android.sdksetup',
                'com.google.android.apps.photos','com.google.android.gm','com.google.android.youtube',
                'com.google.android.videos','com.google.android.apps.wallpaper')) {
            if (($installed -split "\r?\n") -ccontains "package:$background") {
                $disabled = Invoke-DeviceShell @('pm','disable-user','--user',[string]$createdUser,$background)
                if ($disabled -notmatch 'new state: disabled-user') { throw "Could not quiet $background for test user $createdUser." }
                $quieted += $background
            }
        }

        $before = Invoke-DeviceShell @('pm','list','packages','--user',[string]$createdUser,$packageName)
        if ($before -match "(?m)^package:$([regex]::Escape($packageName))$") { throw 'The new Android user already has Whist Lab installed.' }

        $matchingInstalledApk = $false
        if ($originalPackageWasInstalled) {
            $originalPackagePath = Get-PackagePath $originalUser
            $originalDigest = Invoke-DeviceShell @('sha256sum',$originalPackagePath)
            $matchingInstalledApk = $originalDigest.StartsWith("$ApkSha256 ", [StringComparison]::Ordinal)
        }
        $registration = Invoke-AtfVerifiedPackageRegistration -Install {
            if ($matchingInstalledApk) {
                $installText = Invoke-DeviceShell -Arguments @('cmd','package','install-existing','--user',[string]$createdUser,$packageName) -TimeoutSeconds 120
                if (-not (Test-PackageInstalled $createdUser)) { throw "Could not enable the matching APK: $installText" }
            } else {
                $remoteApk = '/data/local/tmp/atf-whistlab-' + $LeaseId + '.apk'
                try {
                    Get-AtfActiveDeviceLease $LeasePath $LeaseId $Serial | Out-Null
                    $push = Invoke-AtfAdbCommand -AdbExecutable $AdbExecutable -Arguments @('-s',$Serial,'push',$ApkPath,$remoteApk) `
                        -ServerSocket $ServerSocket -TimeoutSeconds 90
                    if ($push.ExitCode -ne 0) { throw "Candidate APK transfer failed: $($push.Output)" }
                    $installText = Invoke-DeviceShell -Arguments @('pm','install','-r','--user',[string]$createdUser,$remoteApk) -TimeoutSeconds 120
                    if ($installText -notmatch '(?m)^Success\s*$') { throw "Candidate APK installation failed: $installText" }
                } finally {
                    Invoke-DeviceShell @('rm','-f',$remoteApk) | Out-Null
                }
            }
            } -Observe {
            if (-not (Test-PackageInstalled $createdUser)) { throw 'ANDROID_PACKAGE_NOT_REGISTERED' }
            $path = Get-PackagePath $createdUser
            $digest = Invoke-DeviceShell @('sha256sum',$path)
            [pscustomobject]@{path=$path;sha256=($digest -split '\s+')[0]}
        } -ExpectedHash $ApkSha256
        Invoke-DeviceShell @('locksettings','set-disabled','--user',[string]$createdUser,'true') | Out-Null
        Invoke-DeviceShell @('settings','--user',[string]$createdUser,'put','secure','user_setup_complete','1') | Out-Null
        Invoke-DeviceShell @('settings','--user',[string]$createdUser,'put','system','screen_off_timeout','2147483647') | Out-Null
        Invoke-DeviceShell @('am','start-user',[string]$createdUser) | Out-Null
        $userReadiness = Wait-AtfAndroidUserReady -Read {
            Invoke-DeviceShell -Arguments @('am','get-started-user-state',[string]$createdUser) -TimeoutSeconds 10
        } -Unlock {
            $response = Invoke-DeviceShell -Arguments @('am','unlock-user',[string]$createdUser,'!','!') -TimeoutSeconds 10
            if ($response -notmatch 'Success: user unlocked') { throw "ANDROID_USER_UNLOCK: $response" }
        }
        Invoke-DeviceShell @('am','switch-user',[string]$createdUser) | Out-Null
        $deadline = [DateTime]::UtcNow.AddSeconds(30)
        while ((Get-CurrentUserId) -ne $createdUser -and [DateTime]::UtcNow -lt $deadline) { Start-Sleep -Milliseconds 250 }
        if ((Get-CurrentUserId) -ne $createdUser) { throw 'Android did not switch to the disposable secondary user.' }

        try { Unlock-DisposableUser }
        catch { throw "Android relocked the disposable user during APK installation: $($_.Exception.Message)" }

        $apkPathOnDevice = Get-PackagePath $createdUser
        $deviceHash = Invoke-DeviceShell @('sha256sum',$apkPathOnDevice)
        if ($deviceHash -notmatch '^([0-9a-f]{64})\s' -or $Matches[1] -cne $ApkSha256) {
            throw 'Installed package bytes differ from the candidate APK.'
        }
        $receipt = [ordered]@{
            schemaVersion=1;status='READY';scope='ANDROID_SECONDARY_USER';applicationId=$packageName
            originalUserId=$originalUser;testUserId=$createdUser;createdByRun=$true
            packageWasInstalledBefore=$false;cleanupRequired=$true;accountsAdded=$false
            originalPackageWasInstalledBefore=$originalPackageWasInstalled
            screenOffTimeoutMs=2147483647;screenUnlocked=$true
            backgroundPackagesDisabled=$quieted
            lockScreenDisabledForFreshUser=$true;packageRegistration=$registration
            userReadiness=$userReadiness
            userName=$name;observedApi=$readiness.api;displayProfile=$readiness.displayProfile
            installedPath=$apkPathOnDevice;installedApkSha256=$ApkSha256;observedUtc=[DateTime]::UtcNow.ToString('o')
        }
        Write-JsonFile $readyFile $receipt
        $ownership.state='READY'; Write-AtfUserOwnership $OwnershipJournal $ownership
        $receiptHash = (Get-FileHash -LiteralPath $readyFile -Algorithm SHA256).Hash.ToLowerInvariant()
        $lease = Get-AtfActiveDeviceLease $LeasePath $LeaseId $Serial
        Write-JsonFile $leaseReceipt ([ordered]@{leaseId=$LeaseId;serial=$Serial;active=$true})
        Write-JsonFile $setupFile ([ordered]@{
            schemaVersion=1;serial=$Serial;api=$readiness.api;displayProfile=$readiness.displayProfile
            requestedApkSha256=$ApkSha256;installedPath=$apkPathOnDevice;dataMode='DISPOSABLE_SECONDARY_USER'
            dataIsolationReceipt='data-isolation-ready.json';dataIsolationReceiptSha256=$receiptHash
            originalUserId=$originalUser;testUserId=$createdUser
            originalPackageWasInstalledBefore=$originalPackageWasInstalled
            backupScopeValidated=$false;restoreRequired=$false
        })
        Write-Output "PASS: signed APK installed for isolated Android user $createdUser; original user $originalUser data remains in place."
    } catch {
        $failure = $_
        if ($ownershipCreated -and (Test-Path -LiteralPath $OwnershipJournal -PathType Leaf)) {
            try {
                $ownership=Read-AtfUserOwnership $OwnershipJournal $Serial $run
                $ownership.preparationFailure=$failure.Exception.Message; Write-AtfUserOwnership $OwnershipJournal $ownership
                Complete-OwnedProfile | Out-Null
            } catch {
                $failure.Exception.Data['cleanupFailure'] = $_.Exception.Message
                $cleanupMessage=$_.Exception.Message
                try {
                    $ownership=Read-AtfUserOwnership $OwnershipJournal $Serial $run
                    $ownership.cleanupFailure=$cleanupMessage; Write-AtfUserOwnership $OwnershipJournal $ownership
                } catch { $failure.Exception.Data['ownershipJournalFailure']=$_.Exception.Message }
            }
        }
        throw $failure
    }
} else {
    if (-not (Test-Path -LiteralPath $setupFile -PathType Leaf) -or -not (Test-Path -LiteralPath $readyFile -PathType Leaf)) {
        throw 'Teardown requires the setup and isolation receipts from this run.'
    }
    if (Test-Path -LiteralPath $cleanupFile) { throw 'Refusing to overwrite the teardown receipt.' }
    $setup = Get-Content -LiteralPath $setupFile -Raw | ConvertFrom-Json
    $ready = Get-Content -LiteralPath $readyFile -Raw | ConvertFrom-Json
    $readyHash = (Get-FileHash -LiteralPath $readyFile -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($setup.dataMode -cne 'DISPOSABLE_SECONDARY_USER' -or $setup.serial -cne $Serial -or
        $setup.dataIsolationReceiptSha256 -cne $readyHash -or $ready.applicationId -cne $packageName -or
        -not $ready.createdByRun -or $ready.testUserId -le 0 -or $ready.testUserId -eq $ready.originalUserId) {
        throw 'The isolated-user receipt does not authorize this exact teardown.'
    }
    if ($setup.testUserId -ne $ready.testUserId -or $setup.originalUserId -ne $ready.originalUserId) {
        throw 'Setup and isolation receipts disagree on Android user IDs.'
    }
    if (-not ($ready.originalPackageWasInstalledBefore -is [bool]) -or
        -not ($setup.originalPackageWasInstalledBefore -is [bool]) -or
        $setup.originalPackageWasInstalledBefore -ne $ready.originalPackageWasInstalledBefore) {
        throw 'Setup and isolation receipts disagree on the original package installation state.'
    }
    $ownership = Read-AtfUserOwnership $OwnershipJournal $Serial $run
    if ($ownership.testUserId -ne $ready.testUserId -or $ownership.originalUserId -ne $ready.originalUserId) {
        throw 'ANDROID_USER_OWNERSHIP_RECEIPT_MISMATCH'
    }
    Complete-OwnedProfile | Out-Null
    $originalPackageInstalled = Test-PackageInstalled $ready.originalUserId
    if ($originalPackageInstalled -ne $ready.originalPackageWasInstalledBefore) {
        throw 'Original user package installation state changed during the disposable run.'
    }
    Write-JsonFile $cleanupFile ([ordered]@{
        schemaVersion=1;status='PASS';scope='ANDROID_SECONDARY_USER';applicationId=$packageName
        originalUserId=$ready.originalUserId;removedUserId=$ready.testUserId;removedByRun=$true
        originalPackageWasInstalledBefore=$ready.originalPackageWasInstalledBefore
        originalPackageRetained=$ready.originalPackageWasInstalledBefore;originalPackageStatePreserved=$true
        readyReceiptSha256=$readyHash;leaseId=$LeaseId;serial=$Serial
        observedUtc=[DateTime]::UtcNow.ToString('o')
    })
    Write-Output "PASS: removed disposable Android user $($ready.testUserId); retained original user $($ready.originalUserId)."
}
