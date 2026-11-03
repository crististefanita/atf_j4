function Invoke-AtfAdbCommand {
    param(
        [Parameter(Mandatory)][string]$AdbExecutable,
        [Parameter(Mandatory)][string[]]$Arguments,
        [string]$ServerSocket = '',
        [ValidateRange(1,120)][int]$TimeoutSeconds = 15
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = [IO.Path]::GetFullPath($AdbExecutable)
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($argument in $Arguments) { $startInfo.ArgumentList.Add($argument) }
    if (-not [string]::IsNullOrWhiteSpace($ServerSocket)) {
        $startInfo.EnvironmentVariables['ADB_SERVER_SOCKET'] = $ServerSocket
    }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    try {
        if (-not $process.Start()) { throw "Could not start ADB client: $AdbExecutable" }
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            $process.Kill($true)
            $process.WaitForExit()
            throw "ADB command timed out after ${TimeoutSeconds}s: $($Arguments -join ' ')"
        }
        [System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]@($stdout, $stderr))
        [pscustomobject]@{
            ExitCode = $process.ExitCode
            Output = ($stdout.Result + $stderr.Result).Trim()
        }
    } finally {
        $process.Dispose()
    }
}

function Connect-AtfAndroidDevice {
    param(
        [Parameter(Mandatory)][string]$AdbExecutable,
        [Parameter(Mandatory)][string]$Serial,
        [string]$ServerSocket = '',
        [ValidateRange(1,120)][int]$TimeoutSeconds = 15
    )

    if (-not (Test-Path -LiteralPath $AdbExecutable -PathType Leaf)) {
        throw "ADB client was not found: $AdbExecutable"
    }

    $connection = ''
    if ($Serial -match '^[^:]+:\d+$') {
        $connectResult = Invoke-AtfAdbCommand -AdbExecutable $AdbExecutable `
                -Arguments @('connect', $Serial) -ServerSocket $ServerSocket -TimeoutSeconds $TimeoutSeconds
        $connection = $connectResult.Output
        if ($connectResult.ExitCode -ne 0 -or $connection -match '(?i)failed to connect|cannot connect|connection refused|connection reset|protocol fault|unauthorized') {
            throw "Could not connect ADB transport '$Serial' through '$ServerSocket': $connection"
        }
    }

    $stateResult = Invoke-AtfAdbCommand -AdbExecutable $AdbExecutable `
            -Arguments @('-s', $Serial, 'get-state') -ServerSocket $ServerSocket -TimeoutSeconds $TimeoutSeconds
    $state = $stateResult.Output.Trim()
    if ($stateResult.ExitCode -ne 0 -or $state -ne 'device') {
        throw "ADB transport '$Serial' is not ready through '$ServerSocket': $state"
    }

    [pscustomobject]@{
        Ready = $true
        Serial = $Serial
        ServerSocket = $ServerSocket
        Connection = $connection
        State = $state
    }
}

Export-ModuleMember -Function Connect-AtfAndroidDevice
