$ErrorActionPreference = 'Stop'

function ConvertTo-ArgumentList {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [AllowEmptyString()] [string]$Text
    )
    return @($Text -split '\s+' | Where-Object { $_ })
}

function Join-PathSegment {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$Root,
        [Parameter(Mandatory)] [string[]]$Segment
    )
    return [IO.Path]::Combine([string[]](@($Root) + $Segment))
}

function Format-Number {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [double]$Value
    )
    return $Value.ToString('0.00', [Globalization.CultureInfo]::InvariantCulture)
}

function Measure-RunStatistic {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [double[]]$Value
    )
    $sorted = @($Value | Sort-Object)
    $middle = [math]::Floor($sorted.Count / 2)
    $median = $sorted[$middle]
    if ($sorted.Count % 2 -eq 0) { $median = ($sorted[$middle - 1] + $sorted[$middle]) / 2 }
    $spreadPercent = 0
    if ($median -gt 0) {
        $spreadPercent = ($sorted[-1] - $sorted[0]) / $median * 100
    }
    return [pscustomobject]@{ Median = $median; SpreadPercent = $spreadPercent }
}

function Read-PerfLine {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$Path
    )
    $invariant = [Globalization.CultureInfo]::InvariantCulture
    foreach ($line in Get-Content $Path) {
        if ($line -notmatch '^(?<label>[^:]+): medianMs=(?<total>[0-9.]+)') { continue }
        $measures = [ordered]@{ totalMs = [double]::Parse($Matches['total'], $invariant) }
        foreach ($name in 'cpuMs', 'gpuDrawMs') {
            if ($line -match " $name=(?<value>[0-9.]+)") {
                $measures[$name] = [double]::Parse($Matches['value'], $invariant)
            }
        }
        [pscustomobject]@{ Label = ($line -split ':')[0]; Measures = $measures }
    }
}

function ConvertTo-CaptureSummary {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$Entry,
        [Parameter(Mandatory)] [string[]]$RunFile
    )
    $runs = @($RunFile | ForEach-Object { , @(Read-PerfLine $_) })
    "Capture: $Entry"
    for ($lineIndex = 0; $lineIndex -lt $runs[0].Count; $lineIndex++) {
        "  $($runs[0][$lineIndex].Label):"
        foreach ($measure in 'cpuMs', 'gpuDrawMs', 'totalMs') {
            if (-not $runs[0][$lineIndex].Measures.Contains($measure)) { continue }
            $values = [double[]]@($runs | ForEach-Object { $_[$lineIndex].Measures[$measure] })
            $statistic = Measure-RunStatistic $values
            $note = ''
            if ($measure -ne 'cpuMs') { $note = ' (reference only)' }
            '    {0}: {1} (spread {2}%){3}' -f $measure, (Format-Number $statistic.Median),
                    (Format-Number $statistic.SpreadPercent), $note
        }
    }
}

function ConvertTo-JmhSummary {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$Include,
        [Parameter(Mandatory)] [string]$ResultFile
    )
    "JMH: $Include"
    foreach ($result in (Get-Content $ResultFile -Raw | ConvertFrom-Json)) {
        $name = $result.benchmark -replace '^.*\.([^.]+\.[^.]+)$', '$1'
        $parameters = ''
        if ($result.params) {
            $parameters = ' ' + (($result.params.PSObject.Properties |
                    ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ' ')
        }
        $metric = $result.primaryMetric
        '  {0}{1}: {2} +- {3} {4}' -f $name, $parameters, (Format-Number $metric.score),
                (Format-Number $metric.scoreError), $metric.scoreUnit
    }
}

function ConvertTo-ComparisonSummary {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [object[]]$OldRun,
        [Parameter(Mandatory)] [object[]]$NewRun
    )
    for ($lineIndex = 0; $lineIndex -lt $OldRun[0].Count; $lineIndex++) {
        '{0} vs {1}:' -f $OldRun[0][$lineIndex].Label, $NewRun[0][$lineIndex].Label
        foreach ($measure in 'cpuMs', 'gpuDrawMs', 'totalMs') {
            $measuredOnBothSides = $OldRun[0][$lineIndex].Measures.Contains($measure) -and
                    $NewRun[0][$lineIndex].Measures.Contains($measure)
            if (-not $measuredOnBothSides) { continue }
            $old = Measure-RunStatistic ([double[]]@($OldRun |
                    ForEach-Object { $_[$lineIndex].Measures[$measure] }))
            $new = Measure-RunStatistic ([double[]]@($NewRun |
                    ForEach-Object { $_[$lineIndex].Measures[$measure] }))
            $differencePercent = 0
            if ($old.Median -gt 0) {
                $differencePercent = ($new.Median - $old.Median) / $old.Median * 100
            }
            $noisePercent = [math]::Max($old.SpreadPercent, $new.SpreadPercent)
            $verdict = 'within noise'
            if ([math]::Abs($differencePercent) -gt $noisePercent) {
                $verdict = 'larger than noise'
            }
            if ($measure -ne 'cpuMs') { $verdict = 'reference only' }
            '  {0}: old={1} (spread {2}%) new={3} (spread {4}%) difference={5}% ({6})' -f
                    $measure, (Format-Number $old.Median), (Format-Number $old.SpreadPercent),
                    (Format-Number $new.Median), (Format-Number $new.SpreadPercent),
                    (Format-Number $differencePercent), $verdict
        }
    }
}

function Test-WindowsPlatform {
    return $PSVersionTable.PSEdition -eq 'Desktop' -or $IsWindows
}

function ConvertTo-PlatformGradleArgument {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string[]]$Argument
    )
    if (-not (Test-WindowsPlatform)) { return $Argument }
    # gradlew.bat splits unquoted arguments at commas, and at dots in some Gradle versions.
    return @($Argument | ForEach-Object { if ($_ -match '[,.]') { "`"$_`"" } else { $_ } })
}

function Invoke-NativeCommand {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$FilePath,
        [Parameter(Mandatory)] [string[]]$ArgumentList,
        [Parameter(Mandatory)] [string]$FailureMessage
    )
    # Git and Java print notes on stderr; Windows PowerShell treats redirected stderr as errors.
    $ErrorActionPreference = 'Continue'
    & $FilePath @ArgumentList | Out-Host
    if ($LASTEXITCODE -ne 0) { throw $FailureMessage }
}

function Invoke-Gradle {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$TreePath,
        [Parameter(Mandatory)] [string[]]$Argument
    )
    $wrapperName = 'gradlew'
    if (Test-WindowsPlatform) { $wrapperName = 'gradlew.bat' }
    $wrapper = Join-PathSegment $TreePath @($wrapperName)
    $platformArguments = @(ConvertTo-PlatformGradleArgument $Argument)
    Push-Location $TreePath
    try {
        Invoke-NativeCommand -FilePath $wrapper -ArgumentList $platformArguments `
                -FailureMessage "Gradle failed in '$TreePath': $($Argument -join ' ')"
    } finally {
        Pop-Location
    }
}

function Invoke-Capture {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$TreePath,
        [Parameter(Mandatory)] [string]$Scene,
        [string[]]$Argument = @(),
        [Parameter(Mandatory)] [string]$Destination
    )
    $perfFile = Join-PathSegment $TreePath @('build', 'captures', 'perf.txt')
    if (Test-Path $perfFile) { Remove-Item $perfFile }
    $gradleArguments = @(':sandbox-desktop:run', '-Pcapture', "-Pscenes=$Scene",
            '--console=plain', '-q') + $Argument
    Invoke-Gradle -TreePath $TreePath -Argument $gradleArguments
    if (-not (Test-Path $perfFile)) {
        throw "No perf.txt was written in '$TreePath' for scene '$Scene'."
    }
    Copy-Item $perfFile $Destination
}

Export-ModuleMember -Function ConvertTo-ArgumentList, Join-PathSegment, Format-Number,
        Measure-RunStatistic, Read-PerfLine, ConvertTo-CaptureSummary, ConvertTo-JmhSummary,
        ConvertTo-ComparisonSummary, Invoke-NativeCommand, Invoke-Gradle, Invoke-Capture
