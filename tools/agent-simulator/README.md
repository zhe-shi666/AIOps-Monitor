# Agent Simulator

用于本地联调 `monitor_targets + /api/agent/register + /api/agent/heartbeat`。

## 1. 前置

1. 已启动后端服务（默认 `http://localhost:8080`）
2. 已登录前端并创建一个监控目标，拿到 `agentKey`
3. 本机 Node.js 18+

## 2. 启动

```bash
API_BASE_URL=http://localhost:8080 \
AGENT_KEY=你的AgentKey \
HOSTNAME_OVERRIDE=demo-host-01 \
AGENT_IP=10.0.0.12 \
INTERVAL_MS=5000 \
node tools/agent-simulator/simulate-agent.mjs
```

## 3. 可选参数

- `AGENT_VERSION` 默认 `sim-1.0.0`
- `CPU_BASE` 默认 `35`
- `MEM_BASE` 默认 `60`

脚本会先调用 `/api/agent/register`，随后定时调用 `/api/agent/heartbeat`。
