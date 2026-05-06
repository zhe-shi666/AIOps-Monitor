# MySQL 备份恢复 SOP

本文档用于企业客户执行 AIOps Monitor 的数据备份、恢复、保留与升级回滚。核心原则：升级前必备份，恢复前先停止写入，恢复后做业务验收。

## 1. 备份内容

必须备份：

- MySQL 数据库：用户、监控目标、指标历史、事件、AI 调查、审计日志。
- `.env.production`：生产环境变量与密钥。
- `deploy/certs`：HTTPS 证书。
- `dist`：Agent 发布包与 manifest。

Docker volume 也可以做底层快照，但业务恢复优先使用 SQL 备份。

## 2. 手工备份

默认备份到 `./backups/mysql`：

```bash
MYSQL_CONTAINER=aiops-mysql \
MYSQL_DATABASE=aiops_monitor \
MYSQL_USER=root \
MYSQL_PASSWORD='root或生产root密码' \
./deploy/scripts/backup-mysql.sh
```

输出示例：

```text
[ok] backup created: ./backups/mysql/aiops_monitor_20260506_103000.sql.gz
```

## 3. 定时备份

建议每天凌晨执行一次：

```bash
crontab -e
```

加入：

```cron
30 2 * * * cd /opt/AIOps-Monitor && MYSQL_CONTAINER=aiops-mysql MYSQL_DATABASE=aiops_monitor MYSQL_USER=root MYSQL_PASSWORD='生产root密码' ./deploy/scripts/backup-mysql.sh >> ./backups/backup.log 2>&1
```

建议保留策略：

- 每日备份保留 14 天。
- 每周备份保留 8 周。
- 每月备份保留 12 个月。
- 审计要求高的客户，需要把备份同步到对象存储或异地服务器。

清理 30 天前备份示例：

```bash
find ./backups/mysql -name '*.sql.gz' -mtime +30 -delete
```

## 4. 恢复前检查

1. 确认备份文件存在：

```bash
ls -lh backups/mysql/*.sql.gz
```

2. 暂停后端写入：

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml stop backend
```

3. 可选：恢复前再做一次当前库备份：

```bash
./deploy/scripts/backup-mysql.sh
```

## 5. 执行恢复

```bash
MYSQL_CONTAINER=aiops-mysql \
MYSQL_DATABASE=aiops_monitor \
MYSQL_USER=root \
MYSQL_PASSWORD='root或生产root密码' \
./deploy/scripts/restore-mysql.sh backups/mysql/aiops_monitor_YYYYMMDD_HHMMSS.sql.gz
```

恢复完成后重启后端：

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml start backend
```

## 6. 恢复后验收

- 管理员能登录。
- 监控目标列表存在。
- 最近事件和 AI 调查记录存在。
- Agent 心跳恢复后，目标状态重新变为在线。
- `/actuator/health` 返回 `UP`。

## 7. 升级回滚流程

升级前：

```bash
./deploy/scripts/backup-mysql.sh
git tag backup-before-upgrade-$(date +%Y%m%d_%H%M%S)
```

升级：

```bash
git pull
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```

如果升级失败：

```bash
git checkout 上一个稳定tag
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
./deploy/scripts/restore-mysql.sh 升级前备份.sql.gz
```

注意：

- 已发布的 Flyway migration 不允许修改，只允许新增版本。
- 如果新版本已经写入了新数据结构，回滚应用前必须确认数据库是否也需要恢复到升级前备份。

## 8. 安全要求

- 备份文件包含用户、告警、审计数据，应放在受控目录。
- 备份文件传输必须使用 SSH/SCP/VPN，不建议明文公网下载。
- `.env.production` 不要提交到 Git，不要发到聊天工具。
- 定期演练恢复，不要等真实故障才第一次执行恢复。
