-- Phase 4 - Runbook governance, approval flow, rollback audit
-- Date: 2026-04-28

CREATE TABLE IF NOT EXISTS ai_action_audit (
  id BIGINT NOT NULL AUTO_INCREMENT,
  investigation_id BIGINT NOT NULL,
  action_plan_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  actor VARCHAR(100) DEFAULT NULL,
  decision VARCHAR(30) DEFAULT NULL,
  risk_level VARCHAR(20) DEFAULT NULL,
  detail_json LONGTEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_action_audit_investigation_created (investigation_id, created_at),
  KEY idx_ai_action_audit_action_created (action_plan_id, created_at),
  KEY idx_ai_action_audit_user_created (user_id, created_at),
  KEY idx_ai_action_audit_event_created (event_type, created_at),
  CONSTRAINT fk_ai_action_audit_investigation FOREIGN KEY (investigation_id) REFERENCES ai_investigation(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_action_audit_action_plan FOREIGN KEY (action_plan_id) REFERENCES ai_action_plan(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_action_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_rollback_run (
  id BIGINT NOT NULL AUTO_INCREMENT,
  action_plan_id BIGINT NOT NULL,
  investigation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  executor VARCHAR(100) DEFAULT NULL,
  drill_mode TINYINT(1) NOT NULL DEFAULT 0,
  execution_mode VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  note_text TEXT,
  output_text MEDIUMTEXT,
  error_message TEXT,
  started_at DATETIME DEFAULT NULL,
  ended_at DATETIME DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_rollback_run_action_created (action_plan_id, created_at),
  KEY idx_ai_rollback_run_investigation_created (investigation_id, created_at),
  KEY idx_ai_rollback_run_user_created (user_id, created_at),
  CONSTRAINT fk_ai_rollback_run_action_plan FOREIGN KEY (action_plan_id) REFERENCES ai_action_plan(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_rollback_run_investigation FOREIGN KEY (investigation_id) REFERENCES ai_investigation(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_rollback_run_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
