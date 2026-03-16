# 🚀 AIOps-Monitor: AI 驱动的实时运维监控系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-blue.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**AIOps-Monitor** 是一款将传统实时监控与大语言模型（LLM）相结合的智能化运维平台。它不仅能秒级感知服务器硬件波动，更能通过 AI 实现异常告警的自动化根因分析。

---

## 📸 项目演示
![GitHub图像](/photo/shizhe.png)
---

## 🛠️ 技术架构 (Architecture)

系统采用 **“感知-处理-决策”** 的闭环设计：
1. **感知层**：利用 OSHI 跨平台库进行毫秒级硬件指标抓取。
2. **通信层**：基于 WebSocket (STOMP) 实现服务端向客户端的亚秒级数据推送。
3. **决策层**：结合 Spring AI 异步调用大模型，根据历史趋势进行故障诊断。



---

## ✨ 核心功能 (Features)

* **实时全双工监控**：不同于传统的 HTTP 轮询，系统采用 WebSocket 协议，实现 CPU、内存等指标的“流式”渲染。
* **智能告警诊断**：当指标超过阈值时，自动提取故障前 20 条历史记录构建 Context，调用 AI 给出修复方案。
* **多源指标适配**：预留 Prometheus 接口，支持从单机监控向分布式集群监控的无缝扩展。
* **异步非阻塞处理**：通过 `@EnableAsync` 线程池隔离 AI 推理任务，确保监控主链路的高可用性。

---

## 🏗️ 目录结构 (Structure)

```text
ai-ops-monitor
├── ai-ops-monitor (后端)
│   ├── src/main/java/com/aiops/monitor
│   │   ├── collector/   # 数据采集与定时任务
│   │   ├── service/     # AI 逻辑与核心业务
│   │   ├── config/      # WebSocket/Async/Metrics 配置
│   │   └── model/       # Entity 与 DTO
│   └── docker-compose.yml # 环境一键编排
└── aiops-vue (前端)
    ├── src/components/  # ECharts 监控组件
    └── src/api/         # WebSocket STOMP 客户端配置


