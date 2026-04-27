-- Phase 1 - Threshold settings + incident workflow columns
-- Date: 2026-04-27

CREATE TABLE IF NOT EXISTS alert_threshold_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  cpu_threshold DOUBLE NOT NULL,
  memory_threshold DOUBLE NOT NULL,
  disk_threshold DOUBLE NOT NULL,
  process_count_threshold INT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_alert_threshold_user (user_id),
  CONSTRAINT fk_alert_threshold_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE incident_log
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  ADD COLUMN IF NOT EXISTS acknowledged_at DATETIME DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS resolved_at DATETIME DEFAULT NULL;

CREATE INDEX idx_incident_user_created ON incident_log (user_id, created_at);
CREATE INDEX idx_incident_user_status_created ON incident_log (user_id, status, created_at);

UPDATE incident_log SET status = 'OPEN' WHERE status IS NULL OR status = '';
