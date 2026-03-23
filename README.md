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
 │                  │  AiService.java
 │                  │  MetricsPublisher.java
 │                  │  PromptDataBuilder.java
 │                  │  RedisReceiver.java
 │                  │  
 │                  └─impl
 │                   |   DistributedPublisher.java
 │                   |   StandalonePublisher.java
 │                          
 └─resources
     │  application-dev.yml
     │  application-prod.yml
     │  application.yml


```
## 🚦 快速开始 (Quick Start)
**1. 环境依赖**
* JDK 17+
* Node.js 18+
* MySQL 8.0+（可以用docker拉起/自己配置）
* redis （可以用docker拉起/自己配置）

**2. 配置启动**
1. **一键拉起中间件环境（mysql、redis、(prom可无)）**：打开终端，运行：docker-compose up -d
1. **数据库**：执行根目录下 mysql初始化脚本。
2. **API Key**：在 application.yml 中配置你的 LLM (DeepSeek/GPT) 秘钥。我使用的是本地的ollama。（用LLM的话，maven也需要修改一下）
3. **启动后端**：如果是本地模式就运行dev的配置文件，如果是集群模式就运行prod的配置文件且在每一个线程的程序实参中配置（--server.port=8081 --spring.application.name=Node-B --monitor.mode=distributed --spring.profiles.active=prod）
4. **启动前端**：
* cd aiops-vue
* npm install
* npm run dev

---
## 🧠 技术亮点 (For Interview)
性能优化：通过对采集任务和推送任务的解耦，单机环境下指标延迟控制在 100ms 以内。
提示词工程：设计了结构化的 Prompt 模板，将复杂的时序数据转化为 AI 易理解的特征描述，显著提升诊断准确率。
工程化规范：全链路采用 RESTful API 设计规范，结合 Docker Compose 实现基础设施的一键化部署。

## 📄 开源协议
本项目遵循 MIT License 开源协议。
