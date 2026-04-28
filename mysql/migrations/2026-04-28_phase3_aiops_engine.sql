-- Phase 3 - AIOps engine foundation
-- Date: 2026-04-28

CREATE TABLE IF NOT EXISTS anomaly_result (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_id BIGINT DEFAULT NULL,
  hostname VARCHAR(255) DEFAULT NULL,
  metric_key VARCHAR(50) NOT NULL,
  score DOUBLE NOT NULL,
  baseline DOUBLE DEFAULT NULL,
  observed DOUBLE DEFAULT NULL,
  severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  detected_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  source_metric_id BIGINT DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_anomaly_user_detected (user_id, detected_at),
  KEY idx_anomaly_target_metric_time (target_id, metric_key, detected_at),
  KEY idx_anomaly_status_time (status, detected_at),
  CONSTRAINT fk_anomaly_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_anomaly_target FOREIGN KEY (target_id) REFERENCES monitor_targets(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rca_report (
  id BIGINT NOT NULL AUTO_INCREMENT,
  investigation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  incident_id BIGINT DEFAULT NULL,
  target_id BIGINT DEFAULT NULL,
  summary_md MEDIUMTEXT,
  evidence_json LONGTEXT,
  confidence DOUBLE DEFAULT NULL,
  model_name VARCHAR(100) DEFAULT NULL,
  prompt_hash VARCHAR(128) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_rca_investigation_created (investigation_id, created_at),
  KEY idx_rca_user_created (user_id, created_at),
  CONSTRAINT fk_rca_investigation FOREIGN KEY (investigation_id) REFERENCES ai_investigation(id) ON DELETE CASCADE,
  CONSTRAINT fk_rca_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_model_trace (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  investigation_id BIGINT DEFAULT NULL,
  phase VARCHAR(50) NOT NULL,
  model_name VARCHAR(100) DEFAULT NULL,
  prompt_text LONGTEXT,
  response_text LONGTEXT,
  prompt_tokens INT DEFAULT NULL,
  response_tokens INT DEFAULT NULL,
  latency_ms BIGINT DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  error_message VARCHAR(1000) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_model_trace_user_time (user_id, created_at),
  KEY idx_model_trace_investigation_time (investigation_id, created_at),
  KEY idx_model_trace_phase_time (phase, created_at),
  CONSTRAINT fk_model_trace_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_model_trace_investigation FOREIGN KEY (investigation_id) REFERENCES ai_investigation(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
