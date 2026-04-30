$ErrorActionPreference = "Stop"

$Dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile = Join-Path $Dir ".env"
$PidFile = Join-Path $Dir "agent.pid"
$LogFile = Join-Path $Dir "agent.log"

function Load-EnvFile($Path) {
  Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $parts = $line.Split("=", 2)
    if ($parts.Length -eq 2) {
      [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
    }
  }
}

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
  Write-Error "Node.js is not installed. Please install Node.js 18+ first."
}

$nodeMajor = [int](& node -p "Number(process.versions.node.split('.')[0])")
if ($nodeMajor -lt 18) {
  Write-Error "Node.js version must be >= 18. Current: $(& node -v)"
}

if (-not (Test-Path $EnvFile)) {
  Copy-Item (Join-Path $Dir ".env.example") $EnvFile
  Write-Host "[info] .env created from .env.example, please edit it first: $EnvFile"
  exit 1
}

if (Test-Path $PidFile) {
  $oldPid = Get-Content $PidFile -ErrorAction SilentlyContinue
  if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
    Write-Host "[info] agent-lite already running. pid=$oldPid"
    exit 0
  }
}

Load-EnvFile $EnvFile

if (-not $env:API_BASE_URL -or -not $env:AGENT_KEY) {
  Write-Error "API_BASE_URL and AGENT_KEY are required in .env"
}

$AgentFile = Join-Path $Dir "agent.mjs"
$command = "node `"$AgentFile`" >> `"$LogFile`" 2>&1"
$process = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $command -WorkingDirectory $Dir -PassThru -WindowStyle Hidden
Set-Content -Path $PidFile -Value $process.Id

Write-Host "[ok] agent-lite started. pid=$($process.Id)"
Write-Host "[info] log: $LogFile"
Write-Host "[info] stop: $Dir\stop.ps1"
