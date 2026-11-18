#requires -Version 7.0

function Assert-AtfComponentJavaEnvironment {
    foreach ($name in @('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS')) {
        if ($null -ne [Environment]::GetEnvironmentVariable($name, 'Process')) {
            throw "COMPONENT-JVM-ENV: inherited $name changes child JVMs; remove it and pass options explicitly to the owning suite."
        }
    }
}

function Get-AtfComponentJvmArguments {
    param([ValidateRange(2,2147483647)][int]$InputPartCharacters, [string]$SourceRoot)
    @('-Xmx768m', '-XX:ActiveProcessorCount=2', '-Datf.test.run.scope=suite',
        "-Datf.component.input-part-characters=$InputPartCharacters", "-Datf.governance.gitRoot=$SourceRoot")
}
