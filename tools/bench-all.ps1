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
./tools/bench-all.ps1
Records every default capture scene and JMH benchmark for this repository.

.EXAMPLE
./tools/bench-all.ps1 -JmhIncludes @()
Records only the capture scenes.

.EXAMPLE
./tools/bench-all.ps1 -Scenes @('map-scroll') -JmhIncludes @('TweenTick') -Rounds 2
Records one scene and one JMH class.

.EXAMPLE
./tools/bench-all.ps1 -CaptureArgs '-Psamples=10 -PwarmUpSeconds=5 -Pwindow=1280x720'
Records at 1280x720, for a screen smaller than 1920x1080. CaptureArgs replaces all default
capture arguments. Numbers taken at different window sizes can't be compared.

.NOTES
Needs PowerShell 7 (Windows, macOS, or Linux) or Windows PowerShell 5.1, Git, a JDK that can run
the project's Gradle build, and a desktop session with a screen at least as large as the capture
window (1920x1080 by default). Run it from any folder. On Windows, if the execution policy
blocks scripts, start it with: powershell -ExecutionPolicy Bypass -File tools/bench-all.ps1
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
Import-Module (Join-Path $PSScriptRoot 'Benchmark.psm1') -Force
$TreePath = (Resolve-Path $TreePath).Path

function Invoke-JmhBenchmark {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$TreePath,
        [Parameter(Mandatory)] [string]$Include,
        [Parameter(Mandatory)] [int]$ForkCount,
        [Parameter(Mandatory)] [int]$IterationCount,
        [Parameter(Mandatory)] [string]$Destination
    )
    $jmhResults = Join-PathSegment $TreePath @('core', 'build', 'results', 'jmh', 'results.json')
    if (Test-Path $jmhResults) { Remove-Item $jmhResults }
    Invoke-Gradle -TreePath $TreePath -Argument @(':core:jmh', "-PjmhIncludes=$Include",
            "-PjmhForks=$ForkCount", "-PjmhIterations=$IterationCount", '--console=plain', '-q')
    if (-not (Test-Path $jmhResults)) { throw "No JMH results were written for '$Include'." }
    Copy-Item $jmhResults $Destination
}

$resultDirectory = Join-PathSegment $TreePath @('build', 'bench-all',
        (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force $resultDirectory | Out-Null
$summary = New-Object System.Collections.Generic.List[string]
$summary.Add("Tree: $TreePath at $(git -C $TreePath rev-parse --short HEAD)")
$summary.Add("Capture rounds: $Rounds, capture arguments: $CaptureArgs")
$summary.Add("JMH forks: $JmhForks, iterations: $JmhIterations")

$captureArgumentList = @(ConvertTo-ArgumentList $CaptureArgs)
$entryNumber = 0
foreach ($entry in $Scenes) {
    $entryNumber++
    $entryParts = @(ConvertTo-ArgumentList $entry)
    $entryArguments = $captureArgumentList + @($entryParts | Select-Object -Skip 1)
    $runFiles = @()
    for ($round = 1; $round -le $Rounds; $round++) {
        Write-Host "Capture '$entry', run $round of $Rounds"
        $runFile = Join-Path $resultDirectory "capture-$entryNumber-$round.txt"
        Invoke-Capture -TreePath $TreePath -Scene $entryParts[0] -Argument $entryArguments `
                -Destination $runFile
        $runFiles += $runFile
    }
    $summary.AddRange([string[]]@(ConvertTo-CaptureSummary $entry $runFiles))
}

foreach ($include in $JmhIncludes) {
    Write-Host "JMH '$include'"
    $keptResults = Join-Path $resultDirectory "jmh-$($include -replace '[^A-Za-z0-9]', '-').json"
    Invoke-JmhBenchmark -TreePath $TreePath -Include $include -ForkCount $JmhForks `
            -IterationCount $JmhIterations -Destination $keptResults
    $summary.AddRange([string[]]@(ConvertTo-JmhSummary $include $keptResults))
}

$summaryFile = Join-Path $resultDirectory 'summary.txt'
$summary | Set-Content -Encoding utf8 $summaryFile
$summary | Out-Host
Write-Host "Summary: $summaryFile"
