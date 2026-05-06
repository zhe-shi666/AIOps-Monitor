# 🚀 AIOps-Monitor: AI 驱动的实时运维监控系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-blue.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**AIOps-Monitor** 是一款将传统实时监控与大语言模型（LLM）相结合的智能化运维平台。它不仅能秒级感知服务器硬件波动，更能通过 AI 实现异常告警的自动化根因分析。

## 🛠️ 技术架构 (Architecture)

1. **核心框架**：Spring Boot 3.x
2. **数据采集**：OSHI (SystemHardwareCollector)
3. **实时通信**：WebSocket + STOMP (WebSocketConfig)
4. **分布式/缓存**：Redis (RedisConfig + RedisPubSubConfig)
5. **数据存储**：MySQL + Spring Data JPA (Repository 层)
6. **人工智能**：LangChain4j / Spring AI (AiService)
7. **任务调度**：Spring Task (@Scheduled 在 CollectorScheduler 中)



---

## ✨ 核心功能 (Features)

* **单兵作战（Standalone）**，适合小服务器自守。
* **集群合力（Distributed）**，通过 Redis 实现数据的汇聚。
* **引入了AI 运维（AIOps）**，通过实时指标 -> 自动化 Prompt -> 大模型诊断，解决了传统监控“只报警不给方案”的痛点。
* **实时全双工监控**：不同于传统的 HTTP 轮询，系统采用 WebSocket 协议，实现 CPU、内存等指标的“流式”渲染。
* **智能告警诊断**：当指标超过阈值时，自动提取故障前 20 条历史记录构建 Context，调用 AI 给出修复方案。
* **异步非阻塞处理**：通过 `@EnableAsync` 线程池隔离 AI 推理任务，确保监控主链路的高可用性。
---

## 🏗️ 目录结构 (Structure)

```text

 ├─java
 │  └─com
 │      └─aiops
 │          └─monitor
 │              │  AiOpsMonitorApplication.java
 │              │  
 │              ├─collector
 │              │      AlarmCheckTask.java
 │              │      CollectorScheduler.java
 │              │      MetricsExporter.java
 │              │      SystemHardwareCollector.java
 │              │      
 │              ├─config
 │              │      AsyncConfig.java
 │              │      RedisConfig.java
 │              │      RedisPubSubConfig.java
 │              │      WebSocketConfig.java
 │              │      
 │              │      
 │              ├─listener
 │              │      WebSocketModeListener.java
 │              │      
 │              ├─model
 │              │  ├─dto
 │              │  │      MetricDTO.java
 │              │  │      
 │              │  └─entity
 │              │          IncidentLog.java
 │              │          SystemMetricsHistory.java
 │              │          
 │              ├─repository
 │              │      IncidentLogRepository.java
 │              │      SystemMetricsRepository.java
 │              │      
 │              └─service
 │                  │     AiService.java
 │                  │     MetricsPublisher.java
 │                  │     PromptDataBuilder.java
 │                  │     RedisReceiver.java
 │                  │  
 │                  └─impl
 │                   |    DistributedPublisher.java
 │                   └─   StandalonePublisher.java
 │                          
 └─resources
     │   application-dev.yml
     │   application-prod.yml
     └─  application.yml


```
## 🚦 快速开始 (Quick Start)
**1. 环境依赖**
* JDK 17+
* Node.js 18+
* Docker / Docker Compose（推荐，一键拉起 MySQL、Redis、Prometheus、Ollama）

**2. 配置启动**
1. **一键拉起依赖（mysql、redis、prometheus、ollama）**：
   `docker compose up -d`
2. **首次拉取 Ollama 模型**（只需一次，后续会复用卷缓存）：
   `docker compose exec ollama ollama pull qwen3:0.6b`
3. **数据库**：后端已接入 Flyway 自动迁移，新部署不再需要手工执行 `mysql.sql`。启动后端时会自动执行 `ai-ops-monitor/src/main/resources/db/migration` 下的版本化脚本。
4. **启动后端**：本地开发可用 `dev`，多节点可用 `prod` 并传入参数（例如 `--server.port=8081 --spring.application.name=Node-B --monitor.mode=distributed --spring.profiles.active=prod`）。
5. **启动前端**：
* cd aiops-vue
* npm install
* npm run dev

> AI 配置默认使用 Ollama：`OLLAMA_BASE_URL=http://localhost:11434`，模型默认 `qwen3:0.6b`。可通过环境变量 `OLLAMA_BASE_URL`、`OLLAMA_MODEL` 覆盖。

---
## 🧠 技术亮点 (For Interview)
性能优化：通过对采集任务和推送任务的解耦，单机环境下指标延迟控制在 100ms 以内。
提示词工程：设计了结构化的 Prompt 模板，将复杂的时序数据转化为 AI 易理解的特征描述，显著提升诊断准确率。
工程化规范：全链路采用 RESTful API 设计规范，结合 Docker Compose 实现基础设施的一键化部署。

## 📦 Agent Lite（目标机器轻量接入）
目标机器不需要部署完整项目，只需要运行轻量 agent 包：

1. 打包：
   `./tools/agent-lite/package-agent-lite.sh`
2. 产物：
   `dist/aiops-agent-lite.tar.gz`
3. 在目标机器上解压并启动：
   `./start.sh`

详细说明见：
`tools/agent-lite/README.md`

## 企业交付文档

本轮已补齐企业使用与交付说明：

1. `docs/企业交付使用手册.md`：客户使用、Agent 接入、AI 专家、告警处理、演示脚本。
2. `docs/生产部署方案.md`：Docker Compose 生产部署、Nginx、HTTPS、环境变量、Flyway 自动迁移。
3. `docs/备份恢复SOP.md`：MySQL 备份、恢复、保留策略、升级回滚 SOP。
4. `docs/企业安全与迁移说明.md`：账号安全、密码重置、Agent 签名升级、权限矩阵、测试命令。
5. `docs/当前企业级TODO收口.md`：当前企业级 TODO 的完成状态与后续增强边界。

## 企业交付验证命令

```bash
cd ai-ops-monitor && ./mvnw test
cd ../aiops-vue && npm run build && npm run smoke:permissions
cd .. && AGENT_SIGNING_KEY='local-dev-agent-signing-key-change-me' ./tools/agent-lite/package-agent-lite.sh
node tools/agent-lite/test/release-manifest-smoke.mjs
```

## 📄 开源协议
本项目遵循 MIT License 开源协议。
