-- P0 enterprise foundation - target thresholds, audit log, RBAC roles
-- Date: 2026-04-30

CREATE TABLE IF NOT EXISTS target_threshold_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  cpu_threshold DOUBLE DEFAULT NULL,
  memory_threshold DOUBLE DEFAULT NULL,
  disk_threshold DOUBLE DEFAULT NULL,
  process_count_threshold INT DEFAULT NULL,
  consecutive_breach_count INT DEFAULT NULL,
  silence_seconds INT DEFAULT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_target_threshold_target (target_id),
  KEY idx_target_threshold_user (user_id),
  CONSTRAINT fk_target_threshold_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_target_threshold_target FOREIGN KEY (target_id) REFERENCES monitor_targets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL,
  actor VARCHAR(100) DEFAULT NULL,
  action VARCHAR(80) NOT NULL,
  resource_type VARCHAR(80) DEFAULT NULL,
  resource_id BIGINT DEFAULT NULL,
  ip_address VARCHAR(64) DEFAULT NULL,
  detail_json LONGTEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_user_time (user_id, created_at),
  KEY idx_audit_action_time (action, created_at),
  KEY idx_audit_resource (resource_type, resource_id),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Existing users keep their current role. New code supports ADMIN / OPS / AUDITOR / USER.
