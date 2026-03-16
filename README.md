AIOps-Monitor: 智能运维监控与 AI 诊断系统
🌟 项目简介 (Project Overview)
AIOps-Monitor 是一款基于全栈技术栈与大语言模型（LLM）驱动的现代化运维监控平台。系统不仅能够实时采集服务器硬件指标（CPU、内存等），更能通过 AI 引擎对异常数据进行深度分析，实现从“发现问题”到“诊断问题”的自动化闭环。

🚀 核心特性 (Key Features)
实时硬件感知：集成 OSHI 库，实现对 CPU、内存、磁盘等硬件指标的毫秒级精确采集。

AI 智能诊断：对接大语言模型（如 DeepSeek/GPT），结合历史趋势（Metric Context）实现故障根因分析（RCA）。

动态可视化大屏：前端采用 Vue 3 + ECharts，打造极具科技感的霓虹动态流向图。

实时全双工通信：基于 WebSocket (STOMP) 协议，确保监控告警与 AI 报告秒级触达前端。

异步高性能架构：利用 Spring Async 机制，确保高耗时的 AI 分析不阻塞核心监测链路。

🛠️ 技术栈 (Tech Stack)
后端 (Backend)
框架: Spring Boot 3.x

采集: OSHI (Operating System and Hardware Information)

数据库: MySQL 8.x + Spring Data JPA

通信: Spring WebSocket + STOMP

AI 集成: Spring AI / RestTemplate (LLM API Integration)

前端 (Frontend)
框架: Vue 3 (Composition API)

构建: Vite

图表: ECharts 5

样式: Tailwind CSS

实时: SockJS + Stomp.js

📂 项目结构 (Project Structure)
Plaintext
ai-ops-monitor
├── src/main/java/com/aiops/monitor
│   ├── collector/     # 核心采集器与定时任务 (OSHI, Scheduler)
│   ├── config/        # 系统配置 (WebSocket, Async, Metrics)
│   ├── service/       # 业务逻辑 (AiService, PromptBuilder)
│   ├── repository/    # 数据库访问层 (JPA)
│   └── model/         # 数据模型 (Entity, DTO)
└── aiops-vue/         # 前端 Vue 3 项目源码
⚙️ 快速开始 (Quick Start)
数据库准备：创建 MySQL 数据库并在 application.yml 中配置连接。

AI 密钥配置：在配置文件中填入你的大模型 API Key。

启动后端：运行 MonitorApplication.java。

启动前端：

Bash
cd aiops-vue
npm install
npm run dev
📸 系统预览 (Screenshots)
实时大屏：深蓝黑色科技风界面，实时折线图反馈硬件波动。

AI 诊断报告：自动弹出 Markdown 格式的深度分析，包含故障原因与排查建议。
