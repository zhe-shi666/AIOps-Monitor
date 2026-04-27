-- Phase 2 - AI Investigation domain model
-- Date: 2026-04-27

CREATE TABLE IF NOT EXISTS ai_investigation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  incident_id BIGINT DEFAULT NULL,
  target_id BIGINT DEFAULT NULL,
  title VARCHAR(200) DEFAULT NULL,
  trigger_source VARCHAR(30) NOT NULL DEFAULT 'INCIDENT',
  status VARCHAR(30) NOT NULL DEFAULT 'COLLECTING',
  severity VARCHAR(10) NOT NULL DEFAULT 'P2',
  summary TEXT,
  root_cause VARCHAR(300) DEFAULT NULL,
  confidence DOUBLE DEFAULT NULL,
  started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  closed_at DATETIME DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_investigation_user_status (user_id, status),
  KEY idx_ai_investigation_incident (incident_id),
  KEY idx_ai_investigation_target (target_id),
  KEY idx_ai_investigation_created_at (created_at),
  CONSTRAINT fk_ai_investigation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_investigation_incident FOREIGN KEY (incident_id) REFERENCES incident_log(id) ON DELETE SET NULL,
  CONSTRAINT fk_ai_investigation_target FOREIGN KEY (target_id) REFERENCES monitor_targets(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_observation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  investigation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  source_ref VARCHAR(255) DEFAULT NULL,
  hostname VARCHAR(255) DEFAULT NULL,
  metric_name VARCHAR(50) DEFAULT NULL,
  metric_value DOUBLE DEFAULT NULL,
  observed_at DATETIME DEFAULT NULL,
  confidence DOUBLE DEFAULT NULL,
  payload_json LONGTEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_observation_investigation_type (investigation_id, type),
  KEY idx_ai_observation_investigation_observed_at (investigation_id, observed_at),
  KEY idx_ai_observation_user_created (user_id, created_at),
  CONSTRAINT fk_ai_observation_investigation FOREIGN KEY (investigation_id) REFERENCES ai_investigation(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_observation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_hypothesis (
  id BIGINT NOT NULL AUTO_INCREMENT,
  investigation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  reasoning TEXT,
  confidence DOUBLE DEFAULT NULL,
  rank_order INT DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_hypothesis_investigation_status (investigation_id, status),
  KEY idx_ai_hypothesis_user_created (user_id, created_at),
  CONSTRAINT fk_ai_hypothesis_investigation FOREIGN KEY (investigation_id) REFERENCES ai_investigation(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_hypothesis_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_action_plan (
  id BIGINT NOT NULL AUTO_INCREMENT,
  investigation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  hypothesis_id BIGINT DEFAULT NULL,
  action_type VARCHAR(50) NOT NULL,
  title VARCHAR(255) NOT NULL,
  command_text TEXT,
  runbook_ref VARCHAR(500) DEFAULT NULL,
  risk_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  requires_approval TINYINT(1) NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'PROPOSED',
  rollback_plan TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_action_plan_investigation_status (investigation_id, status),
  KEY idx_ai_action_plan_user_created (user_id, created_at),
  CONSTRAINT fk_ai_action_plan_investigation FOREIGN KEY (investigation_id) REFERENCES ai_investigation(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_action_plan_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_action_plan_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES ai_hypothesis(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_action_run (
  id BIGINT NOT NULL AUTO_INCREMENT,
  action_plan_id BIGINT NOT NULL,
  investigation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  executor VARCHAR(100) DEFAULT NULL,
  execution_mode VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  output_text MEDIUMTEXT,
  error_message TEXT,
  started_at DATETIME DEFAULT NULL,
  ended_at DATETIME DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_action_run_action_status (action_plan_id, status),
  KEY idx_ai_action_run_investigation_created (investigation_id, created_at),
  KEY idx_ai_action_run_user_created (user_id, created_at),
  CONSTRAINT fk_ai_action_run_action_plan FOREIGN KEY (action_plan_id) REFERENCES ai_action_plan(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_action_run_investigation FOREIGN KEY (investigation_id) REFERENCES ai_investigation(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_action_run_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_report_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT,
  investigation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  version_no INT NOT NULL DEFAULT 1,
  format VARCHAR(20) NOT NULL DEFAULT 'MARKDOWN',
  report_markdown MEDIUMTEXT,
  report_json LONGTEXT,
  created_by VARCHAR(100) DEFAULT 'AI',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_report_snapshot_version (investigation_id, version_no),
  KEY idx_ai_report_snapshot_user_created (user_id, created_at),
  CONSTRAINT fk_ai_report_snapshot_investigation FOREIGN KEY (investigation_id) REFERENCES ai_investigation(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_report_snapshot_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
