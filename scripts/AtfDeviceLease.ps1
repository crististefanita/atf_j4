#requires -Version 7.0

function Write-AtfDeviceLease([IO.FileStream]$Stream, $Value) {
    $bytes = [Text.Encoding]::UTF8.GetBytes(($Value | ConvertTo-Json))
    $Stream.SetLength(0)
    $Stream.Position = 0
    $Stream.Write($bytes,0,$bytes.Length)
    $Stream.Flush($true)
}

function Read-AtfDeviceLease([IO.FileStream]$Stream) {
    $reader = [IO.StreamReader]::new($Stream,[Text.Encoding]::UTF8,$true,1024,$true)
    try {
        $content = $reader.ReadToEnd()
        if (-not [string]::IsNullOrWhiteSpace($content)) { return $content | ConvertFrom-Json }
    } finally { $reader.Dispose() }
}

function New-AtfDeviceLease([string]$Path, [string]$Serial, [string]$Owner, [string]$RunRoot) {
    $stream = [IO.File]::Open($Path,[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    try {
        $current = Read-AtfDeviceLease $stream
        if ($current.active) { throw 'The Android emulator is already leased. No UI action was sent.' }
        $id = [guid]::NewGuid().ToString('N')
        Write-AtfDeviceLease $stream ([ordered]@{active=$true;owner=$Owner;leaseId=$id;run=$RunRoot
            serial=$Serial;startedAt=[DateTimeOffset]::UtcNow.ToString('o')})
        return $id
    } finally { $stream.Dispose() }
}

function Remove-AtfDeviceLease([string]$Path, [string]$LeaseId) {
    $stream = [IO.File]::Open($Path,[IO.FileMode]::Open,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    try {
        $current = Read-AtfDeviceLease $stream
        if ($current.leaseId -ceq $LeaseId -and $current.active) {
            $current.active = $false
            $current | Add-Member -NotePropertyName updatedAt -NotePropertyValue ([DateTimeOffset]::UtcNow.ToString('o')) -Force
            Write-AtfDeviceLease $stream $current
        }
    } finally { $stream.Dispose() }
}
