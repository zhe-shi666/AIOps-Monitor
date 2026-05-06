# 当前企业级 TODO 收口状态

| 序号 | 任务 | 状态 | 说明 |
| --- | --- | --- | --- |
| 1 | 监控目标板块收口 | 已完成 | 目标卡片、在线/离线/禁用、最后心跳、Agent 版本、复制安装命令、重置 Key。 |
| 2 | Agent 管理能力 | 已完成 | 安装/升级命令、重启命令、Key 轮换、运行验证说明。 |
| 3 | 离线机器体验优化 | 已完成 | 前端按最后心跳自动识别离线并置灰，保留最后数据。 |
| 4 | 告警规则完善 | 已完成 | 用户级阈值、目标级阈值覆盖、连续触发、静默窗口已可配置。 |
| 5 | 事件工作台打磨 | 已完成 | 主次布局、筛选、前端去重折叠、右侧操作面板。 |
| 6 | AI 专家流程增强 | 已完成 | 事件工作台发起调查，跳转 AI 专家，支持分析、草稿、快照。 |
| 7 | 日志/Trace 接入 | 已完成轻量版 | 后端已有 `/api/agent/logs` 和 `/api/agent/traces`，agent-lite 已周期性发送日志和 Trace。 |
| 8 | 权限与多用户隔离 | 已完成 | 后端 ADMIN/OPS 可写、AUDITOR/USER 只读；前端已做路由、导航、按钮级权限矩阵。 |
| 9 | 企业使用文档 | 已完成 | 见 `docs/企业交付使用手册.md`、`docs/生产部署方案.md`、`docs/备份恢复SOP.md`。 |
| 10 | 演示数据与测试 | 已完成 | 手册包含演示脚本；新增后端、前端、Agent smoke 测试。 |
| 11 | 自动数据库迁移 | 已完成 | 接入 Flyway，新增 `V1__baseline_schema.sql` 与 `V2__enterprise_security_and_agent_release.sql`，客户部署不再手工跑 SQL。 |
| 12 | 登录账号安全 | 已完成 | 密码强度、密码重置、注册开关、初始管理员初始化、生产 JWT Secret 配置说明。 |
| 13 | Agent 安装包签名与版本升级 | 已完成 | 打包生成 SHA256/HMAC 签名、release manifest、release SQL；升级脚本校验后安装并保留 `.env`。 |
| 14 | 生产部署方案 | 已完成 | 新增 `docker-compose.prod.yml`、前后端 Dockerfile、Nginx HTTPS 反代、生产 env 示例。 |
| 15 | 备份恢复方案 | 已完成 | 新增 MySQL 备份/恢复脚本与 SOP，覆盖保留策略和升级回滚。 |

## 交付前验证命令

```bash
cd ai-ops-monitor && ./mvnw test
cd ../aiops-vue && npm run build && npm run smoke:permissions
cd .. && AGENT_SIGNING_KEY='local-dev-agent-signing-key-change-me' ./tools/agent-lite/package-agent-lite.sh
node tools/agent-lite/test/release-manifest-smoke.mjs
```

## 后续增强边界

- 密码重置已接入 SMTP 邮件；后续可继续扩展短信、企业微信或钉钉投递。
- Agent 当前为脚本升级，后续可增加后台批量下发升级任务、灰度分组与升级审计。
- 生产部署当前为单机 Docker Compose，后续大客户可扩展到 K8s、对象存储备份、外部 MySQL/Redis。
- 前端权限矩阵已覆盖关键页面，后续新增页面时需要继续复用 `usePermissions`。
