# AIOps Agent Lite

Agent Lite 是安装在被监控主机上的轻量采集器。目标机器不需要运行完整项目，只需要安装 Node.js 18+ 并运行本 Agent。

## 支持系统

- Linux：推荐生产使用，支持 shell 脚本启动。
- macOS：支持本地 Mac 主机或测试机接入，支持 shell 脚本启动。
- Windows：支持 PowerShell 脚本启动。

## 采集内容

- CPU 使用率
- 内存使用率
- 磁盘使用率
- 网络 RX/TX 速率
- 进程数量
- 周期性主机心跳日志
- 每次采集的轻量 Trace span

说明：不同系统底层命令不同，Agent 会自动按平台选择采集方式。Linux 使用 `/proc` 和系统命令，macOS 使用 `df/netstat/ps`，Windows 使用 PowerShell/CIM。

## Linux 安装/升级

```bash
curl -fsSL http://192.168.64.1:9000/install-or-upgrade.sh | bash
cd ~/aiops-agent-lite
nano .env
./stop.sh && ./start.sh && ./tail-log.sh
```

## macOS 安装/升级

```bash
curl -fL http://192.168.64.1:9000/aiops-agent-lite.tar.gz -o ~/aiops-agent-lite.tar.gz
rm -rf ~/aiops-agent-lite
tar -xzf ~/aiops-agent-lite.tar.gz -C ~
cd ~/aiops-agent-lite
nano .env
chmod +x *.sh
./stop.sh && ./start.sh && ./tail-log.sh
```

## Windows 安装/升级

在 PowerShell 中执行：

```powershell
$base = "http://192.168.64.1:9000"
$pkg = Join-Path $HOME "aiops-agent-lite.tar.gz"
Invoke-WebRequest "$base/aiops-agent-lite.tar.gz" -OutFile $pkg
if (Test-Path "$HOME\aiops-agent-lite") { Remove-Item "$HOME\aiops-agent-lite" -Recurse -Force }
tar -xzf $pkg -C $HOME
Set-Location "$HOME\aiops-agent-lite"
notepad .env
powershell -ExecutionPolicy Bypass -File .\start.ps1
powershell -ExecutionPolicy Bypass -File .\tail-log.ps1
```

## .env 配置

```bash
API_BASE_URL=http://192.168.64.1:8080
AGENT_KEY=replace-with-your-target-agent-key
HOSTNAME_OVERRIDE=demo-host-01
AGENT_IP=
AGENT_VERSION=agent-lite-1.2.0-cross
INTERVAL_MS=5000
OBSERVABILITY_ENABLED=true
LOG_EVERY_N_HEARTBEATS=12
```

说明：

- `API_BASE_URL` 指向 AIOps 后端地址。
- `AGENT_KEY` 来自平台「监控目标」。
- `AGENT_IP` 留空时自动识别真实网卡 IP。
- `OBSERVABILITY_ENABLED=true` 时会额外上报日志和 Trace，供 AI 专家诊断引用。
- `LOG_EVERY_N_HEARTBEATS` 控制日志上报频率，默认每 12 次心跳发一条。

## 常用命令

Linux/macOS：

```bash
cd ~/aiops-agent-lite
./status.sh
./tail-log.sh
./stop.sh
./start.sh
```

Windows：

```powershell
Set-Location "$HOME\aiops-agent-lite"
powershell -ExecutionPolicy Bypass -File .\status.ps1
powershell -ExecutionPolicy Bypass -File .\tail-log.ps1
powershell -ExecutionPolicy Bypass -File .\stop.ps1
powershell -ExecutionPolicy Bypass -File .\start.ps1
```

## 成功日志

```text
AIOps agent-lite started { metricMode: 'real-cross-platform', platform: 'linux', observabilityEnabled: true, ... }
[register] { status: 'ONLINE', ... }
[heartbeat] ... DISK=... RX=... TX=... PROC=... true
```
