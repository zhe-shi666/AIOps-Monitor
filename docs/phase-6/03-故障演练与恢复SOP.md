# 故障演练与恢复 SOP

## 1. 演练目标

验证关键依赖异常时系统可恢复，且恢复后核心接口可用。

## 2. 可执行场景

1. Redis 重启  
2. MySQL 重启  
3. Ollama 重启

## 3. 演练命令

```bash
cd /path/to/AIOps-Monitor

# Redis
USERNAME=admin PASSWORD=123456 AUTO_REGISTER=true \
bash tools/phase-6/drill/fault-drill.sh redis-restart

# MySQL
USERNAME=admin PASSWORD=123456 AUTO_REGISTER=true \
bash tools/phase-6/drill/fault-drill.sh mysql-restart

# Ollama
USERNAME=admin PASSWORD=123456 AUTO_REGISTER=true \
bash tools/phase-6/drill/fault-drill.sh ollama-restart
```

## 4. 演练验收标准

1. 注入后依赖能自动恢复到 ready。  
2. 恢复后烟测全通过（`actuator/health` + 业务接口）。  
3. 无持续错误风暴（日志错误不应连续增长）。

## 5. 失败处理

1. 若依赖未恢复：停止发布，转入基础设施故障处理。  
2. 若恢复后烟测失败：执行快速回滚并开 Incident。  
3. 必须补齐复盘：故障原因、监控缺口、修复计划与截止时间。
