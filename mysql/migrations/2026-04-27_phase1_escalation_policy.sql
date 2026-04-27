-- Phase 1 - Escalation policy and incident escalation fields
-- Date: 2026-04-27

CREATE TABLE IF NOT EXISTS alert_escalation_policy (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  p1_intervals VARCHAR(100) NOT NULL DEFAULT '1,3,5,10',
  p2_intervals VARCHAR(100) NOT NULL DEFAULT '5,15,30',
  p3_intervals VARCHAR(100) NOT NULL DEFAULT '15,30,60',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_alert_escalation_user (user_id),
  CONSTRAINT fk_alert_escalation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE incident_log
  ADD COLUMN IF NOT EXISTS severity VARCHAR(10) NOT NULL DEFAULT 'P2',
  ADD COLUMN IF NOT EXISTS escalation_level INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS last_notified_at DATETIME DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS next_notify_at DATETIME DEFAULT NULL;

CREATE INDEX idx_incident_status_next_notify ON incident_log (status, next_notify_at);

UPDATE incident_log
SET severity = 'P2'
WHERE severity IS NULL OR severity = '';

UPDATE incident_log
SET escalation_level = 0
WHERE escalation_level IS NULL OR escalation_level < 0;
