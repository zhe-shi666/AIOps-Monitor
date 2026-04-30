$Dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogFile = Join-Path $Dir "agent.log"

if (-not (Test-Path $LogFile)) {
  New-Item -ItemType File -Path $LogFile | Out-Null
}

Get-Content $LogFile -Wait -Tail 80
