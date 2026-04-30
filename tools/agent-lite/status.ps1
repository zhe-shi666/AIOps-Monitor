$Dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $Dir "agent.pid"
$LogFile = Join-Path $Dir "agent.log"

if (-not (Test-Path $PidFile)) {
  Write-Host "[status] stopped"
  exit 0
}

$pidValue = Get-Content $PidFile -ErrorAction SilentlyContinue
if ($pidValue -and (Get-Process -Id $pidValue -ErrorAction SilentlyContinue)) {
  Write-Host "[status] running pid=$pidValue"
  Write-Host "[log] $LogFile"
  exit 0
}

Write-Host "[status] stopped (stale pid=$pidValue)"
