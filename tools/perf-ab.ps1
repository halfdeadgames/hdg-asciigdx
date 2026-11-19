<#
.SYNOPSIS
Compares capture-mode render times of two source trees, alternating old and new runs.

.DESCRIPTION
Runs the sandbox capture once per round in each tree, in the order old, new, old, new.
Alternating spreads slow drift (heat, background tasks) evenly over both sides. Each run's
perf.txt is kept, and the summary pairs the perf lines of both sides by position.

For each line and each measure the summary gives the median of the round medians per side, the
round spread ((max - min) / median of the round medians), and how far new differs from old,
in percent. Only CPU time (cpuMs) gets a verdict, because it stays steady from run to run. The GPU
drawing time (gpuDrawMs, when the platform has a GPU timer) and the total frame time (totalMs)
are listed for reference only. The driver decides when the GPU starts on the queued work, and
it decides differently from run to run. The total includes the time the work was held back,
and the GPU timer counts the gaps where the GPU waits for more commands.

.PARAMETER OldRef
Git ref for the old side. The script checks it out, detached, in its own worktree under
the temp folder. Ignored when OldPath is set. The ref must already contain the scene and the
capture changes that write cpuMs; older commits fail with a message.

.PARAMETER OldPath
Folder of an existing tree to use as the old side.

.PARAMETER NewPath
Folder of the new side. Defaults to this repository.

.PARAMETER Rounds
Number of old and new run pairs. Defaults to 4.

.PARAMETER Scene
Scene id to capture. Defaults to layered-blend-stress.

.PARAMETER CaptureArgs
Gradle arguments for both sides, separated by spaces.

.PARAMETER OldArgs
Extra Gradle arguments for the old side only, separated by spaces, such as '-Players=7'.

.PARAMETER NewArgs
Extra Gradle arguments for the new side only, separated by spaces.

.EXAMPLE
./tools/perf-ab.ps1 -OldRef HEAD
Compares the last commit with the working tree.

.EXAMPLE
./tools/perf-ab.ps1 -OldPath . -OldArgs '-Players=7' -NewArgs '-Players=8'
Compares 7 and 8 layers in the same tree.

.EXAMPLE
./tools/perf-ab.ps1 -OldRef HEAD -Scene sparse-update -OldArgs '-Pdirty=50' -NewArgs '-Pdirty=50'
Compares the last commit with the working tree on the sparse-update scene at 50% changed cells.

.EXAMPLE
./tools/perf-ab.ps1 -OldRef HEAD -Scene map-scroll
Compares the last commit with the working tree on the map-scroll scene (moving camera over a
large map).

.EXAMPLE
./tools/perf-ab.ps1 -OldRef HEAD -Scene animated-cells
Compares the last commit with the working tree on the animated-cells scene (every cell moves,
fades, and recolors each frame).

.NOTES
Needs PowerShell 7 (Windows, macOS, or Linux) or Windows PowerShell 5.1, Git, a JDK that can run
the project's Gradle build, and a desktop session with a screen at least as large as the capture
window (1920x1080 by default). Run it from any folder. The old worktree gets a copy of this
repository's local.properties when it has none. On Windows, if the execution policy blocks
scripts, start it with: powershell -ExecutionPolicy Bypass -File tools/perf-ab.ps1
#>
param(
    [string]$OldRef = 'HEAD',
    [string]$OldPath,
    [string]$NewPath = (Split-Path -Parent $PSScriptRoot),
    [int]$Rounds = 4,
    [string]$Scene = 'layered-blend-stress',
    [string]$CaptureArgs = '-Psamples=10 -PsampleSeconds=1 -PwarmUpSeconds=5 -Pwindow=1920x1080',
    [string]$OldArgs = '',
    [string]$NewArgs = ''
)

$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'Benchmark.psm1') -Force
$repositoryRoot = Split-Path -Parent $PSScriptRoot

function Copy-LocalPropertiesFile {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$SourceTree,
        [Parameter(Mandatory)] [string]$TargetTree
    )
    $source = Join-PathSegment $SourceTree @('local.properties')
    $target = Join-PathSegment $TargetTree @('local.properties')
    if ((Test-Path $source) -and -not (Test-Path $target)) { Copy-Item $source $target }
}

function Initialize-OldWorktree {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$RepositoryRoot,
        [Parameter(Mandatory)] [string]$Ref
    )
    $commit = git -C $RepositoryRoot rev-parse --verify --quiet "$Ref^{commit}"
    if ($LASTEXITCODE -ne 0 -or -not $commit) {
        throw "Could not find the commit '$Ref' in '$RepositoryRoot'."
    }
    $worktreePath = Join-PathSegment ([IO.Path]::GetTempPath()) @('hdg-asciigdx-perf-old')
    $gitArguments = @('-C', $RepositoryRoot, 'worktree', 'add', '--detach', $worktreePath, $commit)
    if (Test-Path $worktreePath) {
        $gitArguments = @('-C', $worktreePath, 'checkout', '--detach', $commit)
    }
    Invoke-NativeCommand -FilePath 'git' -ArgumentList $gitArguments `
            -FailureMessage "Could not prepare the old worktree at '$Ref'."
    Copy-LocalPropertiesFile -SourceTree $RepositoryRoot -TargetTree $worktreePath
    return $worktreePath
}

function Read-SideRun {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$ResultDirectory,
        [Parameter(Mandatory)] [string]$Side,
        [Parameter(Mandatory)] [int]$RoundCount
    )
    return , @(1..$RoundCount | ForEach-Object {
        , @(Read-PerfLine (Join-Path $ResultDirectory "$Side-$_.txt"))
    })
}

function Confirm-RunShape {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [object[]]$Run,
        [Parameter(Mandatory)] [int]$LineCount,
        [Parameter(Mandatory)] [string]$Side
    )
    foreach ($sideRun in $Run) {
        if ($sideRun.Count -ne $LineCount) {
            throw "The $Side runs wrote $($sideRun.Count) perf lines instead of $LineCount."
        }
        foreach ($perfLine in $sideRun) {
            if (-not $perfLine.Measures.Contains('cpuMs')) {
                throw "A $Side perf line has no cpuMs. Its tree predates the split timing."
            }
        }
    }
}

if (-not $OldPath) {
    $OldPath = Initialize-OldWorktree -RepositoryRoot $repositoryRoot -Ref $OldRef
}
$OldPath = (Resolve-Path $OldPath).Path
$NewPath = (Resolve-Path $NewPath).Path

$captureArgumentList = @(ConvertTo-ArgumentList $CaptureArgs)
$oldArguments = $captureArgumentList + @(ConvertTo-ArgumentList $OldArgs)
$newArguments = $captureArgumentList + @(ConvertTo-ArgumentList $NewArgs)
$resultDirectory = Join-PathSegment $repositoryRoot @('build', 'perf-ab',
        (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force $resultDirectory | Out-Null

for ($round = 1; $round -le $Rounds; $round++) {
    Write-Host "Round $round of ${Rounds}: old"
    Invoke-Capture -TreePath $OldPath -Scene $Scene -Argument $oldArguments `
            -Destination (Join-Path $resultDirectory "old-$round.txt")
    Write-Host "Round $round of ${Rounds}: new"
    Invoke-Capture -TreePath $NewPath -Scene $Scene -Argument $newArguments `
            -Destination (Join-Path $resultDirectory "new-$round.txt")
}

$oldRuns = Read-SideRun -ResultDirectory $resultDirectory -Side 'old' -RoundCount $Rounds
$newRuns = Read-SideRun -ResultDirectory $resultDirectory -Side 'new' -RoundCount $Rounds
$lineCount = $oldRuns[0].Count
Confirm-RunShape -Run $oldRuns -LineCount $lineCount -Side 'old'
Confirm-RunShape -Run $newRuns -LineCount $lineCount -Side 'new'

$summary = New-Object System.Collections.Generic.List[string]
$summary.Add("Old: $OldPath $OldArgs")
$summary.Add("New: $NewPath $NewArgs")
$summary.Add("Rounds: $Rounds, capture arguments: $CaptureArgs")
$summary.AddRange([string[]]@(ConvertTo-ComparisonSummary $oldRuns $newRuns))

$summaryFile = Join-Path $resultDirectory 'summary.txt'
$summary | Set-Content -Encoding ascii $summaryFile
$summary | Out-Host
Write-Host "Results: $resultDirectory"
