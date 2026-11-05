# Builds docs/demo/imali-poe-narrated-walkthrough.mp4 from slides + Windows TTS.
# Rubric still requires a phone screen recording — use this as narration practice or B-roll.
param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$demoDir = Join-Path $ProjectRoot "docs\demo"
$slidesDir = Join-Path $demoDir "slides"
$workDir = Join-Path $demoDir "work"
$outVideo = Join-Path $demoDir "imali-poe-narrated-walkthrough.mp4"
$narration = Get-Content (Join-Path $demoDir "narration.json") -Raw | ConvertFrom-Json

New-Item -ItemType Directory -Force -Path $workDir | Out-Null

$ffmpeg = (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source
if (-not $ffmpeg) {
    $candidates = @(
        "$env:LOCALAPPDATA\Microsoft\WinGet\Links\ffmpeg.exe",
        "$env:ProgramFiles\ffmpeg\bin\ffmpeg.exe"
    )
    $ffmpeg = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $ffmpeg) { throw "ffmpeg not found. Install with: winget install Gyan.FFmpeg" }

Add-Type -AssemblyName System.Speech
$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer

$index = 0
$segmentFiles = @()
foreach ($scene in $narration) {
    $index++
    $slidePath = Join-Path $slidesDir $scene.slide
    if (-not (Test-Path $slidePath)) { Write-Warning "Missing slide $slidePath"; continue }
    $wav = Join-Path $workDir ("audio-{0:D2}.wav" -f $index)
    $mp4 = Join-Path $workDir ("seg-{0:D2}.mp4" -f $index)
    $synth.SetOutputToWaveFile($wav)
    $synth.Speak($scene.text)
    $synth.SetOutputToDefaultAudioDevice()
    $duration = [math]::Max([int]$scene.durationSec, 5)
    & $ffmpeg -y -loop 1 -i $slidePath -i $wav -c:v libx264 -tune stillimage -c:a aac -b:a 192k -pix_fmt yuv420p -shortest -t $duration $mp4
    if ($LASTEXITCODE -eq 0) { $segmentFiles += $mp4 }
}

$listFile = Join-Path $workDir "concat.txt"
$segmentFiles | ForEach-Object { "file '$($_ -replace '\\','/')'" } | Set-Content $listFile -Encoding ascii
& $ffmpeg -y -f concat -safe 0 -i $listFile -c copy $outVideo
Write-Host "Video written to $outVideo"
