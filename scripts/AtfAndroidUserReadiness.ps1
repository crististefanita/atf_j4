#requires -Version 7.0

function Wait-AtfAndroidUserReady([scriptblock]$Read, [scriptblock]$Unlock,
        [scriptblock]$Pause = { Start-Sleep -Milliseconds 250 },
        [scriptblock]$Now = { [DateTimeOffset]::UtcNow }, [ValidateRange(1,120)][int]$TimeoutSeconds = 90) {
    $deadline = (& $Now).AddSeconds($TimeoutSeconds)
    $observations = 0; $unlockRequested = $false
    while ($true) {
        $state = (& $Read).Trim(); $observations++
        if ((& $Now) -ge $deadline) { throw "ANDROID_USER_BOOT_TIMEOUT: $state" }
        if ($state -ceq 'RUNNING_UNLOCKED') {
            return [pscustomobject]@{state=$state;observations=$observations;unlockRequested=$unlockRequested}
        }
        if ($state -cnotin @('BOOTING','RUNNING_LOCKED','RUNNING_UNLOCKING')) { throw "ANDROID_USER_STATE: $state" }
        if ($state -ceq 'RUNNING_LOCKED' -and !$unlockRequested) { & $Unlock | Out-Null; $unlockRequested = $true }
        & $Pause
    }
}
