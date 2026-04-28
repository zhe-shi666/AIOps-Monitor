# 灰度发布与回滚 SOP

## 1. 灰度发布前提

1. 单元/集成测试通过。  
2. 压测门禁通过。  
3. 最近一次故障演练通过。  
4. 上一版本可随时回退。

## 2. 灰度门禁命令

```bash
cd /Users/linx/Documents/lth/AIOps-Monitor

STABLE_API_BASE_URL=http://localhost:8080 \
CANARY_API_BASE_URL=http://localhost:8081 \
USERNAME=admin PASSWORD=123456 \
bash tools/phase-6/release/canary-check.sh
```

## 3. 推荐放量节奏

1. 10% 流量，观察 15 分钟。  
2. 30% 流量，观察 30 分钟。  
3. 50% 流量，观察 30 分钟。  
4. 100% 全量切换。

每阶段关注：
- 错误率、P95  
- 告警增长速率  
- AI 推理超时率  
- 关键页面可用性

## 4. 快速回滚命令

```bash
cd /Users/linx/Documents/lth/AIOps-Monitor

STABLE_API_BASE_URL=http://localhost:8080 \
USERNAME=admin PASSWORD=123456 \
bash tools/phase-6/release/rollback-fast.sh
```

## 5. 回滚触发条件（任一满足即回滚）

1. 错误率 > 5% 且持续 5 分钟。  
2. P95 > 2s 且持续 10 分钟。  
3. 核心路径不可用（登录、告警查询、调查查询）。  
4. 故障无法在 15 分钟内定位或缓解。
