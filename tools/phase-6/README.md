# Phase-6 工具集

用于一次性完成：压测、演练、灰度门禁、快速回滚。

## 目录

1. `smoke/smoke-check.mjs`：基础健康烟测（actuator + 核心业务接口）
2. `load/http-load.mjs`：并发压测（可配置阈值门禁）
3. `drill/fault-drill.sh`：故障注入演练（Redis/MySQL/Ollama 重启）
4. `release/canary-check.sh`：灰度门禁检查
5. `release/rollback-fast.sh`：快速回滚脚本
6. `run-phase6-suite.sh`：一键执行 Phase-6 验证套件

## 快速开始

```bash
cd /Users/linx/Documents/lth/AIOps-Monitor

# 1) 一键跑烟测 + 压测
USERNAME=admin PASSWORD=123456 AUTO_REGISTER=true \
bash tools/phase-6/run-phase6-suite.sh

# 2) 跑故障演练（可选）
USERNAME=admin PASSWORD=123456 AUTO_REGISTER=true \
RUN_DRILL=true DRILL_SCENARIO=redis-restart \
bash tools/phase-6/run-phase6-suite.sh
```

## 灰度 / 回滚

```bash
# 灰度门禁（稳定版 + 金丝雀版）
STABLE_API_BASE_URL=http://localhost:8080 \
CANARY_API_BASE_URL=http://localhost:8081 \
USERNAME=admin PASSWORD=123456 \
bash tools/phase-6/release/canary-check.sh

# 快速回滚
STABLE_API_BASE_URL=http://localhost:8080 \
USERNAME=admin PASSWORD=123456 \
bash tools/phase-6/release/rollback-fast.sh
```
