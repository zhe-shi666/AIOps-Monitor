$TaskName = "AIOpsAgentLite"
Stop-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
Write-Host "[ok] Windows scheduled task removed: $TaskName"
