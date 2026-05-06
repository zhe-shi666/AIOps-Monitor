# AIOps-Monitor

AIOps-Monitor 是一个面向中小企业运维场景的 AI 智能监控平台，聚焦“监控采集、告警治理、AI 调查、通知触达、目标接入、企业交付”六条主线，帮助团队从传统看图告警，升级到可落地的智能运维协作流程。

项目当前采用前后端分离架构：
- 后端：Spring Boot 3
- 前端：Vue 3 + Vite + Element Plus
- 数据库：MySQL
- 缓存与消息：Redis
- 监控链路：Prometheus + Agent Lite
- AI 能力：Ollama / 可扩展 LLM 接口

## 核心价值

- 统一监控多台主机：支持管理员分配监控目标，按用户隔离查看目标与告警。
- 实时指标可视化：覆盖 CPU、内存、磁盘、网络、进程数等核心主机指标。
- 告警中心治理：支持阈值、降噪、升级、通知投递与审计追踪。
- AI 专家协作：由事件工作台人工加入调查对象，再进行结构化分析、诊断草稿与人工审核。
- 企业交付友好：补齐迁移脚本、生产部署说明、备份恢复 SOP、权限矩阵、Agent 打包与版本升级链路。

## 主要功能

### 1. 总览看板
- 实时展示主机运行状态与关键指标趋势
- 汇总当前在线目标、活跃告警、通知状态
- 支持多用户场景下的数据隔离展示

### 2. 事件工作台
- 告警泳道与调查泳道联动
- 支持告警去重、筛选、重点信息折叠
- 支持将告警事件人工加入 AI 调查流程

### 3. AI 专家
- 基于调查对象生成结构化分析结果
- 生成诊断草稿、调查记录和快照信息
- 支持人工审核、结论补充与处置协同

### 4. 监控目标
- 管理员分配监控目标与 Agent 安装信息
- 支持 Linux / macOS / Windows 目标接入
- 支持 Agent 版本识别、安装包分发与升级脚本

### 5. 告警中心与通知通道
- 阈值策略、升级策略、邮件通知、投递审计
- 管理员统一维护发件配置
- 普通用户维护自己的收件邮箱与通知偏好

### 6. 管理后台
- 用户、角色、权限与审计管理
- 首次登录修改密码、管理员临时密码重置
- 支持企业项目常见的权限控制边界

## 技术架构

### 后端
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA
- Flyway 自动数据库迁移
- Redis
- WebSocket
- Prometheus 指标接口

### 前端
- Vue 3
- Vite
- Vue Router
- Pinia
- Element Plus

### Agent 与交付
- Node.js 轻量 Agent Lite
- 跨平台安装脚本（Linux / macOS / Windows）
- Agent 发布清单、签名校验、升级脚本

## 仓库结构

```text
AIOps-Monitor/
├── ai-ops-monitor/         # Spring Boot 后端源码
├── aiops-vue/              # Vue 前端源码
├── mysql/                  # 数据迁移脚本与本地数据库目录
├── prometheus/             # Prometheus 配置
├── deploy/                 # 生产部署配置与备份恢复脚本
├── tools/                  # Agent、模拟器、压测与演练工具
├── dist/                   # 已打包的 Agent 发布产物
├── docs/                   # 项目设计、阶段文档、交付与 SOP
├── docker-compose.yml      # 本地开发编排
├── docker-compose.prod.yml # 生产部署编排示例
└── README.md
```

## 快速开始

### 1. 环境要求
- JDK 17+
- Node.js 18+
- Docker / Docker Compose

### 2. 启动基础依赖

```bash
docker compose up -d
```

如需启用本地 AI 能力，首次拉取 Ollama 模型：

```bash
docker compose exec ollama ollama pull qwen3:0.6b
```

### 3. 启动后端

```bash
cd ai-ops-monitor
./mvnw spring-boot:run
```

说明：
- 后端已接入 Flyway。
- 启动时会自动执行 `ai-ops-monitor/src/main/resources/db/migration` 下的版本化迁移脚本。
- 不再需要手工执行历史 `mysql.sql`。

### 4. 启动前端

```bash
cd aiops-vue
npm install
npm run dev
```

### 5. 接入目标机器 Agent

打包 Agent：

```bash
./tools/agent-lite/package-agent-lite.sh
```

打包后产物位于：
- `dist/aiops-agent-lite.tar.gz`
- `dist/agent-release.json`
- `dist/install-or-upgrade.sh`

详细接入方式见：
- [tools/agent-lite/README.md](tools/agent-lite/README.md)

## 企业交付文档

以下文档已经补齐，可直接用于部署、演示与交付：

- [企业交付使用手册](docs/企业交付使用手册.md)
- [生产部署方案](docs/生产部署方案.md)
- [备份恢复 SOP](docs/备份恢复SOP.md)
- [企业安全与迁移说明](docs/企业安全与迁移说明.md)
- [当前企业级 TODO 收口](docs/当前企业级TODO收口.md)

阶段性交付过程文档：
- [Phase 0](docs/phase-0/README.md)
- [Phase 2](docs/phase-2/README.md)
- [Phase 3](docs/phase-3/README.md)
- [Phase 4](docs/phase-4/README.md)
- [Phase 5](docs/phase-5/README.md)
- [Phase 6](docs/phase-6/README.md)

## 测试与验证

后端测试：

```bash
cd ai-ops-monitor
./mvnw test
```

前端构建与权限冒烟：

```bash
cd aiops-vue
npm install
npm run build
npm run smoke:permissions
```

Agent 发布清单校验：

```bash
AGENT_SIGNING_KEY='local-dev-agent-signing-key-change-me' ./tools/agent-lite/package-agent-lite.sh
node tools/agent-lite/test/release-manifest-smoke.mjs
```

## 当前适用场景

适合以下团队：
- 需要监控 1~N 台 Linux / Windows / macOS 主机的中小企业
- 希望在传统监控基础上增加 AI 辅助诊断能力的运维团队
- 希望做私有化部署、演示交付、PoC 验证的项目团队

## 说明

- `dist/` 目录当前保存了 Agent 发布产物，便于演示和交付。
- 如果你希望把本仓库作为纯源码仓库，可以将 `dist/` 移出仓库，改为通过 GitHub Releases 分发。
- `mysql/data/` 为本地运行数据目录，已通过 `.gitignore` 忽略，不会进入版本库。

## License

当前仓库尚未补充正式 `LICENSE` 文件。
如果你计划公开开源，建议在发布前补充许可证文本后再对外声明授权方式。
