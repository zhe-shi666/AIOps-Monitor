$ErrorActionPreference = "Stop"

$Dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $Dir "agent.pid"

if (-not (Test-Path $PidFile)) {
  Write-Host "[info] no pid file. agent-lite may not be running."
  exit 0
}

$pidValue = Get-Content $PidFile -ErrorAction SilentlyContinue
if (-not $pidValue) {
  Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
  Write-Host "[info] empty pid file removed."
  exit 0
}

$process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
if ($process) {
  Stop-Process -Id $pidValue -Force
  Write-Host "[ok] agent-lite stopped. pid=$pidValue"
} else {
  Write-Host "[info] process not found. pid=$pidValue"
}

Get-CimInstance Win32_Process |
  Where-Object { $_.CommandLine -like "*agent.mjs*" -and $_.ProcessId -ne $PID } |
  ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
  }

Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
