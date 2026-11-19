#requires -Version 7.0

function Assert-AtfUserOwnershipPath([string]$Path) {
    $current=[IO.Path]::GetFullPath($Path)
    while ($current) {
        if (([IO.File]::Exists($current) -or [IO.Directory]::Exists($current)) -and
            ([IO.File]::GetAttributes($current) -band [IO.FileAttributes]::ReparsePoint)) { throw 'ANDROID_USER_OWNERSHIP_INDIRECT' }
        $parent=[IO.Directory]::GetParent($current);$current=if($parent){$parent.FullName}else{''}
    }
}

function Write-AtfUserOwnership([string]$Path,$Record,[switch]$Create) {
    Assert-AtfUserOwnershipPath $Path
    $temporary = $Path + '.' + [guid]::NewGuid().ToString('N') + '.pending'
    $bytes = [Text.UTF8Encoding]::new($false).GetBytes(($Record | ConvertTo-Json -Depth 8) + "`n")
    $stream = [IO.File]::Open($temporary,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
    try { $stream.Write($bytes,0,$bytes.Length); $stream.Flush($true) } finally { $stream.Dispose() }
    try { [IO.File]::Move($temporary,$Path,(-not $Create)) }
    finally { if ([IO.File]::Exists($temporary)) { [IO.File]::Delete($temporary) } }
}

function Read-AtfUserOwnership([string]$Path,[string]$Serial,[string]$RunRoot) {
    Assert-AtfUserOwnershipPath $Path
    $record = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
    if ($record.schemaVersion -ne 1 -or $record.serial -cne $Serial -or
        $record.leaseId -cnotmatch '^[a-f0-9]{32}$' -or $record.userName -cnotmatch '^ATFWhist-[a-f0-9]{32}$' -or
        $record.originalUserId -isnot [long] -or $record.originalUserId -lt 0 -or
        $record.originalPackageWasInstalledBefore -isnot [bool] -or
        $record.state -cnotin @('CREATING','CREATED','READY','REMOVING','REMOVED') -or
        -not [IO.Path]::GetFullPath($RunRoot).Equals([string]$record.runRoot,[StringComparison]::OrdinalIgnoreCase) -or
        $record.beforeUserIds -isnot [array] -or $record.beforeUserIds -notcontains $record.originalUserId -or
        @($record.beforeUserIds | Where-Object { $_ -isnot [long] -or $_ -lt 0 }).Count -or
        ($null -ne $record.testUserId -and ($record.testUserId -isnot [long] -or $record.testUserId -le 0 -or
            $record.beforeUserIds -contains $record.testUserId)) -or
        ($null -eq $record.testUserId -and $record.state -cne 'CREATING')) { throw 'ANDROID_USER_OWNERSHIP_INVALID' }
    return $record
}

function ConvertFrom-AtfAndroidUsers([string]$Text) {
    if ($Text -notmatch '(?m)^\s*Users:') { throw 'ANDROID_USER_LIST_UNAVAILABLE' }
    $rows = @([regex]::Matches($Text,'(?m)^\s*UserInfo\{([0-9]+):(.+):[0-9a-fA-F]+\}[^\r\n]*\r?$') | ForEach-Object {
        [pscustomobject]@{id=[long]$_.Groups[1].Value;name=$_.Groups[2].Value}
    })
    if (-not $rows.Count -or $rows.Count -ne [regex]::Matches($Text,'UserInfo\{').Count -or
        @($rows.id | Select-Object -Unique).Count -ne $rows.Count) { throw 'ANDROID_USER_LIST_UNAVAILABLE' }
    return $rows
}

function New-AtfUserOwnership([string]$Path,[string]$Serial,[string]$LeaseId,[string]$RunRoot,
    [long]$OriginalUserId,[bool]$OriginalPackageInstalled,[object[]]$Users) {
    if ($LeaseId -cnotmatch '^[a-f0-9]{32}$' -or $Users.id -notcontains $OriginalUserId) { throw 'ANDROID_USER_OWNERSHIP_CONTEXT' }
    $record = [ordered]@{schemaVersion=1;state='CREATING';serial=$Serial;leaseId=$LeaseId
        runRoot=[IO.Path]::GetFullPath($RunRoot);userName=('ATFWhist-' + [guid]::NewGuid().ToString('N'))
        originalUserId=$OriginalUserId;originalPackageWasInstalledBefore=$OriginalPackageInstalled
        beforeUserIds=@($Users.id);testUserId=$null;preparationFailure=$null;cleanupFailure=$null}
    Write-AtfUserOwnership $Path $record -Create
    return [pscustomobject]$record
}

function Set-AtfOwnedUserId([string]$Path,[string]$Serial,[string]$RunRoot,[long]$UserId) {
    $record = Read-AtfUserOwnership $Path $Serial $RunRoot
    if ($record.state -cne 'CREATING' -or $UserId -le 0 -or $record.beforeUserIds -contains $UserId) {
        throw 'ANDROID_USER_OWNERSHIP_FOREIGN_ID'
    }
    $record.testUserId=$UserId; $record.state='CREATED'
    Write-AtfUserOwnership $Path $record
    return $record
}

function Complete-AtfOwnedUserCore([string]$Path,[string]$Serial,[string]$RunRoot,[string]$LeaseId,
    [scriptblock]$ReadUsers,[scriptblock]$ReadCurrent,[scriptblock]$SwitchOriginal,
    [scriptblock]$RemoveUser,[scriptblock]$ReadOriginalPackage) {
    $record = Read-AtfUserOwnership $Path $Serial $RunRoot
    if ($LeaseId -cnotmatch '^[a-f0-9]{32}$') { throw 'ANDROID_USER_OWNERSHIP_LEASE' }
    $users = @(& $ReadUsers)
    if ($users.id -notcontains $record.originalUserId) { throw 'ANDROID_USER_ORIGINAL_MISSING' }
    if ($null -eq $record.testUserId) {
        $matches = @($users | Where-Object name -CEQ $record.userName)
        if ($matches.Count -gt 1) { throw 'ANDROID_USER_OWNERSHIP_AMBIGUOUS' }
        if (-not $matches.Count) { throw 'ANDROID_USER_CREATION_UNCONFIRMED' }
        if ($matches.Count) { $record = Set-AtfOwnedUserId $Path $Serial $RunRoot $matches[0].id }
    }
    $owned = @($users | Where-Object id -EQ $record.testUserId)
    if ($owned.Count -and ($owned.Count -ne 1 -or $owned[0].name -cne $record.userName -or
            $record.beforeUserIds -contains $owned[0].id -or $record.state -ceq 'REMOVED')) {
        throw 'ANDROID_USER_OWNERSHIP_FOREIGN_ID'
    }
    if (-not $owned.Count -and $null -ne $record.testUserId -and $record.state -cnotin @('REMOVING','REMOVED')) {
        throw 'ANDROID_USER_DISAPPEARED'
    }
    if ((& $ReadOriginalPackage $record.originalUserId) -cne $record.originalPackageWasInstalledBefore) {
        throw 'ANDROID_USER_ORIGINAL_PACKAGE_CHANGED'
    }
    if ($owned.Count) {
        if ((& $ReadCurrent) -ne $record.originalUserId) { & $SwitchOriginal $record.originalUserId }
        if ((& $ReadCurrent) -ne $record.originalUserId) { throw 'ANDROID_USER_ORIGINAL_NOT_ACTIVE' }
        $record.state='REMOVING'; $record | Add-Member -NotePropertyName cleanupLeaseId -NotePropertyValue $LeaseId -Force
        Write-AtfUserOwnership $Path $record
        & $RemoveUser $record.testUserId
        if (@(& $ReadUsers | Where-Object id -EQ $record.testUserId).Count) { throw 'ANDROID_USER_REMOVE_NOT_CONFIRMED' }
    }
    if ((& $ReadOriginalPackage $record.originalUserId) -cne $record.originalPackageWasInstalledBefore) {
        throw 'ANDROID_USER_ORIGINAL_PACKAGE_CHANGED'
    }
    $record.state='REMOVED'
    Write-AtfUserOwnership $Path $record
    return $record
}

function Complete-AtfOwnedUser([string]$Path,[string]$Serial,[string]$RunRoot,[string]$LeaseId,
    [scriptblock]$ReadUsers,[scriptblock]$ReadCurrent,[scriptblock]$SwitchOriginal,
    [scriptblock]$RemoveUser,[scriptblock]$ReadOriginalPackage) {
    Assert-AtfUserOwnershipPath $Path
    Assert-AtfUserOwnershipPath ($Path + '.lock')
    try { $lock=[IO.File]::Open($Path + '.lock',[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None) }
    catch { throw 'ANDROID_USER_OWNERSHIP_BUSY' }
    try { Complete-AtfOwnedUserCore @PSBoundParameters } finally { $lock.Dispose() }
}
