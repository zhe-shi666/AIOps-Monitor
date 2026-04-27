-- Phase 1 - Target & Agent ingest migration
-- Date: 2026-04-27
-- Safe to run multiple times on MySQL 8+

CREATE TABLE IF NOT EXISTS monitor_targets (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL COMMENT '用户自定义名称',
  hostname VARCHAR(255) DEFAULT NULL,
  agent_key VARCHAR(64) NOT NULL COMMENT 'Agent 上报身份凭证',
  description VARCHAR(255) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  last_heartbeat_at DATETIME DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
  ip_address VARCHAR(64) DEFAULT NULL,
  agent_version VARCHAR(64) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_agent_key (agent_key),
  KEY idx_user_id (user_id),
  KEY idx_last_heartbeat_at (last_heartbeat_at),
  CONSTRAINT fk_target_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE monitor_targets
  ADD COLUMN IF NOT EXISTS enabled TINYINT(1) NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS last_heartbeat_at DATETIME DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
  ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS agent_version VARCHAR(64) DEFAULT NULL;

ALTER TABLE system_metrics_history
  ADD COLUMN IF NOT EXISTS user_id BIGINT DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS target_id BIGINT DEFAULT NULL;

ALTER TABLE incident_log
  ADD COLUMN IF NOT EXISTS user_id BIGINT DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS target_id BIGINT DEFAULT NULL;

UPDATE monitor_targets SET status = 'OFFLINE' WHERE status IS NULL OR status = '';
UPDATE monitor_targets SET enabled = 1 WHERE enabled IS NULL;
