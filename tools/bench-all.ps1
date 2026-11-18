<#
.SYNOPSIS
Records baseline numbers for the capture benchmark scenes and the JMH benchmarks of one tree.

.DESCRIPTION
Runs each capture scene several times and each JMH include once, one after another, and writes
one summary file. Use it to record where the numbers stand after a change; use perf-ab.ps1 to
compare two trees. Leave the machine idle while it runs: other work skews both kinds of timing.

For each capture scene and perf line the summary gives the median of the run medians and the run
spread ((max - min) / median), for cpuMs (the trusted measure) and, for reference only,
gpuDrawMs and totalMs. For each JMH benchmark it gives the score, its error margin, and its unit,
read from the JMH results.json, which is also kept.

.PARAMETER TreePath
Folder of the tree to measure. Defaults to this repository.

.PARAMETER Scenes
Capture runs, one entry each: a scene id, optionally followed by a space and extra Gradle
arguments, such as 'sparse-update -Pdirty=1,50'. A comma list makes the scene write one perf
line per value. An empty list skips the captures.

.PARAMETER Rounds
Number of runs per capture entry. Defaults to 4.

.PARAMETER CaptureArgs
Gradle arguments for every capture run, separated by spaces.

.PARAMETER JmhIncludes
JMH include patterns, each run as its own :core:jmh call. A pattern is a regular expression on
the full benchmark name, such as 'GlyphLoadingBenchmark.bake'. An empty list skips JMH.

.PARAMETER JmhForks
JMH forks per benchmark. Defaults to 3, for tighter error margins.

.PARAMETER JmhIterations
JMH measurement iterations per fork. Defaults to 10.

.EXAMPLE
.\tools\bench-all.ps1
Records every default capture scene and JMH benchmark for this repository.

.EXAMPLE
.\tools\bench-all.ps1 -JmhIncludes @()
Records only the capture scenes.

.EXAMPLE
.\tools\bench-all.ps1 -Scenes @('map-scroll') -JmhIncludes @('TweenTick') -Rounds 2
Records one scene and one JMH class.
#>
param(
    [string]$TreePath = (Split-Path -Parent $PSScriptRoot),
    [string[]]$Scenes = @('animated-cells', 'sparse-update -Pdirty=1,10,50,100 -Players=1,4',
            'map-scroll'),
    [int]$Rounds = 4,
    [string]$CaptureArgs = '-Psamples=10 -PsampleSeconds=1 -PwarmUpSeconds=5 -Pwindow=1920x1080',
    [string[]]$JmhIncludes = @('AnimatedCell', 'TweenTick', 'GlyphLoading'),
    [int]$JmhForks = 3,
    [int]$JmhIterations = 10
)

$ErrorActionPreference = 'Stop'
$invariant = [Globalization.CultureInfo]::InvariantCulture
$TreePath = (Resolve-Path $TreePath).Path

function Split-Arguments([string]$text) {
    return @($text -split '\s+' | Where-Object { $_ })
}

function Format-Number([double]$value) {
    return $value.ToString('0.00', $invariant)
}

function Get-Median([double[]]$values) {
    $sorted = $values | Sort-Object
    $middle = [math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2 -eq 1) { return $sorted[$middle] }
    return ($sorted[$middle - 1] + $sorted[$middle]) / 2
}

function Get-SpreadPercent([double[]]$values, [double]$median) {
    if ($median -le 0) { return 0 }
    $measured = $values | Measure-Object -Minimum -Maximum
    return ($measured.Maximum - $measured.Minimum) / $median * 100
}

function Invoke-Gradle([string[]]$gradleArgs) {
    $quotedArgs = @($gradleArgs | ForEach-Object {
        if ($_ -match '[,.]') { "`"$_`"" } else { $_ }
    })
    Push-Location $TreePath
    try {
        & .\gradlew.bat @quotedArgs | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $($gradleArgs -join ' ')" }
    } finally {
        Pop-Location
    }
}

function Invoke-Capture([string]$entry, [string]$resultFile) {
    $entryParts = Split-Arguments $entry
    $perfFile = Join-Path $TreePath 'build\captures\perf.txt'
    if (Test-Path $perfFile) { Remove-Item $perfFile }
    $gradleArgs = @(':sandbox-desktop:run', '-Pcapture', "-Pscenes=$($entryParts[0])",
            '--console=plain', '-q') + (Split-Arguments $CaptureArgs) + @($entryParts | Select-Object -Skip 1)
    Invoke-Gradle $gradleArgs
    if (-not (Test-Path $perfFile)) { throw "No perf.txt was written for '$entry'." }
    Copy-Item $perfFile $resultFile
}

function Read-PerfLines([string]$file) {
    foreach ($line in Get-Content $file) {
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

function Add-CaptureSummary([string]$entry, [string[]]$runFiles, $summary) {
    $runs = @($runFiles | ForEach-Object { , @(Read-PerfLines $_) })
    $summary.Add("Capture: $entry")
    for ($lineIndex = 0; $lineIndex -lt $runs[0].Count; $lineIndex++) {
        $summary.Add("  $($runs[0][$lineIndex].Label):")
        foreach ($measure in 'cpuMs', 'gpuDrawMs', 'totalMs') {
            if (-not $runs[0][$lineIndex].Measures.Contains($measure)) { continue }
            $values = [double[]]@($runs | ForEach-Object { $_[$lineIndex].Measures[$measure] })
            $median = Get-Median $values
            $spread = Get-SpreadPercent $values $median
            $note = ''
            if ($measure -ne 'cpuMs') { $note = ' (reference only)' }
            $summary.Add(('    {0}: {1} (spread {2}%){3}' -f $measure, (Format-Number $median),
                    (Format-Number $spread), $note))
        }
    }
}

function Add-JmhSummary([string]$include, [string]$resultsFile, $summary) {
    $summary.Add("JMH: $include")
    foreach ($result in (Get-Content $resultsFile -Raw | ConvertFrom-Json)) {
        $name = $result.benchmark -replace '^.*\.([^.]+\.[^.]+)$', '$1'
        $parameters = ''
        if ($result.params) {
            $parameters = ' ' + (($result.params.PSObject.Properties |
                    ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ' ')
        }
        $metric = $result.primaryMetric
        $summary.Add(('  {0}{1}: {2} +- {3} {4}' -f $name, $parameters,
                (Format-Number $metric.score), (Format-Number $metric.scoreError),
                $metric.scoreUnit))
    }
}

$resultDirectory = Join-Path $TreePath ("build\bench-all\" + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force $resultDirectory | Out-Null
$summary = New-Object System.Collections.Generic.List[string]
$summary.Add("Tree: $TreePath at $(git -C $TreePath rev-parse --short HEAD)")
$summary.Add("Capture rounds: $Rounds, capture arguments: $CaptureArgs")
$summary.Add("JMH forks: $JmhForks, iterations: $JmhIterations")

$entryNumber = 0
foreach ($entry in $Scenes) {
    $entryNumber++
    $runFiles = @()
    for ($round = 1; $round -le $Rounds; $round++) {
        Write-Host "Capture '$entry', run $round of $Rounds"
        $runFile = Join-Path $resultDirectory "capture-$entryNumber-$round.txt"
        Invoke-Capture $entry $runFile
        $runFiles += $runFile
    }
    Add-CaptureSummary $entry $runFiles $summary
}

$jmhResults = Join-Path $TreePath 'core\build\results\jmh\results.json'
foreach ($include in $JmhIncludes) {
    Write-Host "JMH '$include'"
    if (Test-Path $jmhResults) { Remove-Item $jmhResults }
    Invoke-Gradle @(':core:jmh', "-PjmhIncludes=$include", "-PjmhForks=$JmhForks",
            "-PjmhIterations=$JmhIterations", '--console=plain', '-q')
    if (-not (Test-Path $jmhResults)) { throw "No JMH results were written for '$include'." }
    $keptResults = Join-Path $resultDirectory "jmh-$($include -replace '[^A-Za-z0-9]', '-').json"
    Copy-Item $jmhResults $keptResults
    Add-JmhSummary $include $keptResults $summary
}

$summaryFile = Join-Path $resultDirectory 'summary.txt'
$summary | Set-Content -Encoding utf8 $summaryFile
$summary | Out-Host
Write-Host "Summary: $summaryFile"
