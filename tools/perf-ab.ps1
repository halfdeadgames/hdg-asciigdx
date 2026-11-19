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
$invariant = [Globalization.CultureInfo]::InvariantCulture
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$runsOnWindows = $PSVersionTable.PSEdition -eq 'Desktop' -or $IsWindows

function Split-Arguments([string]$text) {
    return @($text -split '\s+' | Where-Object { $_ })
}

$captureArgumentList = @(Split-Arguments $CaptureArgs)
$oldArgumentList = @(Split-Arguments $OldArgs)
$newArgumentList = @(Split-Arguments $NewArgs)

function Join-PathParts([string]$root, [string[]]$parts) {
    return [IO.Path]::Combine([string[]](@($root) + $parts))
}

function Copy-LocalProperties([string]$worktreePath) {
    $source = Join-PathParts $repositoryRoot @('local.properties')
    $target = Join-PathParts $worktreePath @('local.properties')
    if ((Test-Path $source) -and -not (Test-Path $target)) { Copy-Item $source $target }
}

function Invoke-NativeCommand([scriptblock]$command) {
    $callerErrorAction = $ErrorActionPreference
    # Git and Java print notes on stderr; Windows PowerShell treats redirected stderr as errors.
    $ErrorActionPreference = 'Continue'
    try {
        & $command | Out-Host
        return $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $callerErrorAction
    }
}

function Initialize-OldWorktree([string]$ref) {
    $worktreePath = Join-PathParts ([IO.Path]::GetTempPath()) @('hdg-asciigdx-perf-old')
    if (Test-Path $worktreePath) {
        $exitCode = Invoke-NativeCommand { git -C $worktreePath checkout --detach $ref }
    } else {
        $exitCode = Invoke-NativeCommand {
            git -C $repositoryRoot worktree add --detach $worktreePath $ref
        }
    }
    if ($exitCode -ne 0) { throw "Could not prepare the old worktree at '$ref'." }
    Copy-LocalProperties $worktreePath
    return $worktreePath
}

function Get-GradleArgumentsForPlatform([string[]]$gradleArgs) {
    if (-not $runsOnWindows) { return $gradleArgs }
    # gradlew.bat splits unquoted arguments at commas, and at dots in some Gradle versions.
    return @($gradleArgs | ForEach-Object { if ($_ -match '[,.]') { "`"$_`"" } else { $_ } })
}

function Invoke-Capture([string]$treePath, [string[]]$sideArgs, [string]$resultFile) {
    $perfFile = Join-PathParts $treePath @('build', 'captures', 'perf.txt')
    if (Test-Path $perfFile) { Remove-Item $perfFile }
    $gradleArgs = @(':sandbox-desktop:run', '-Pcapture', "-Pscenes=$Scene", '--console=plain',
            '-q') + $captureArgumentList + $sideArgs
    $wrapperName = if ($runsOnWindows) { 'gradlew.bat' } else { 'gradlew' }
    $wrapper = Join-PathParts $treePath @($wrapperName)
    $platformArgs = Get-GradleArgumentsForPlatform $gradleArgs
    Push-Location $treePath
    try {
        $exitCode = Invoke-NativeCommand { & $wrapper @platformArgs }
    } finally {
        Pop-Location
    }
    if ($exitCode -ne 0) { throw "Capture failed in '$treePath'." }
    if (-not (Test-Path $perfFile)) { throw "No perf.txt was written in '$treePath'." }
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

function Format-Number([double]$value) {
    return $value.ToString('0.00', $invariant)
}

if (-not $OldPath) { $OldPath = Initialize-OldWorktree $OldRef }
$OldPath = (Resolve-Path $OldPath).Path
$NewPath = (Resolve-Path $NewPath).Path

$resultDirectory = Join-PathParts $repositoryRoot @('build', 'perf-ab',
        (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force $resultDirectory | Out-Null

for ($round = 1; $round -le $Rounds; $round++) {
    Write-Host "Round $round of ${Rounds}: old"
    Invoke-Capture $OldPath $oldArgumentList (Join-Path $resultDirectory "old-$round.txt")
    Write-Host "Round $round of ${Rounds}: new"
    Invoke-Capture $NewPath $newArgumentList (Join-Path $resultDirectory "new-$round.txt")
}

$oldRuns = @(1..$Rounds | ForEach-Object { , @(Read-PerfLines (Join-Path $resultDirectory "old-$_.txt")) })
$newRuns = @(1..$Rounds | ForEach-Object { , @(Read-PerfLines (Join-Path $resultDirectory "new-$_.txt")) })

function Assert-SameShape([object[]]$runs, [int]$lineCount, [string]$side) {
    foreach ($run in $runs) {
        if ($run.Count -ne $lineCount) {
            throw "The $side runs wrote $($run.Count) perf lines instead of $lineCount."
        }
        foreach ($perfLine in $run) {
            if (-not $perfLine.Measures.Contains('cpuMs')) {
                throw "A $side perf line has no cpuMs. Its tree predates the split timing."
            }
        }
    }
}

$lineCount = $oldRuns[0].Count
Assert-SameShape $oldRuns $lineCount 'old'
Assert-SameShape $newRuns $lineCount 'new'

$summary = New-Object System.Collections.Generic.List[string]
$summary.Add("Old: $OldPath $OldArgs")
$summary.Add("New: $NewPath $NewArgs")
$summary.Add("Rounds: $Rounds, capture arguments: $CaptureArgs")
for ($lineIndex = 0; $lineIndex -lt $lineCount; $lineIndex++) {
    $summary.Add(('{0} vs {1}:' -f $oldRuns[0][$lineIndex].Label, $newRuns[0][$lineIndex].Label))
    foreach ($measure in 'cpuMs', 'gpuDrawMs', 'totalMs') {
        $measuredOnBothSides = $oldRuns[0][$lineIndex].Measures.Contains($measure) -and
                $newRuns[0][$lineIndex].Measures.Contains($measure)
        if (-not $measuredOnBothSides) { continue }
        $oldMedians = [double[]]@($oldRuns | ForEach-Object { $_[$lineIndex].Measures[$measure] })
        $newMedians = [double[]]@($newRuns | ForEach-Object { $_[$lineIndex].Measures[$measure] })
        $oldMedian = Get-Median $oldMedians
        $newMedian = Get-Median $newMedians
        $oldSpread = Get-SpreadPercent $oldMedians $oldMedian
        $newSpread = Get-SpreadPercent $newMedians $newMedian
        $differencePercent = 0
        if ($oldMedian -gt 0) { $differencePercent = ($newMedian - $oldMedian) / $oldMedian * 100 }
        $noisePercent = [math]::Max($oldSpread, $newSpread)
        $verdict = 'within noise'
        if ([math]::Abs($differencePercent) -gt $noisePercent) { $verdict = 'larger than noise' }
        if ($measure -ne 'cpuMs') { $verdict = 'reference only' }
        $summary.Add(('  {0}: old={1} (spread {2}%) new={3} (spread {4}%) difference={5}% ({6})' -f
                $measure, (Format-Number $oldMedian), (Format-Number $oldSpread),
                (Format-Number $newMedian), (Format-Number $newSpread),
                (Format-Number $differencePercent), $verdict))
    }
}

$summaryFile = Join-Path $resultDirectory 'summary.txt'
$summary | Set-Content -Encoding ascii $summaryFile
$summary | Out-Host
Write-Host "Results: $resultDirectory"
