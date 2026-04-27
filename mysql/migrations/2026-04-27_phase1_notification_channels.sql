-- Phase 1 - Notification channels and delivery audit
-- Date: 2026-04-27

CREATE TABLE IF NOT EXISTS notification_channel (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20) NOT NULL DEFAULT 'WEBHOOK',
  webhook_url VARCHAR(500) NOT NULL,
  secret VARCHAR(255) DEFAULT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  last_notified_at DATETIME DEFAULT NULL,
  last_error VARCHAR(500) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notification_channel_user (user_id),
  KEY idx_notification_channel_user_enabled (user_id, enabled),
  CONSTRAINT fk_notification_channel_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS notification_delivery_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  incident_id BIGINT NOT NULL,
  channel_id BIGINT DEFAULT NULL,
  channel_name VARCHAR(100) NOT NULL,
  target VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL,
  http_status INT DEFAULT NULL,
  response_body VARCHAR(1000) DEFAULT NULL,
  error_message VARCHAR(500) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_delivery_user_incident_created (user_id, incident_id, created_at),
  KEY idx_delivery_channel_created (channel_id, created_at),
  CONSTRAINT fk_delivery_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_delivery_incident FOREIGN KEY (incident_id) REFERENCES incident_log(id) ON DELETE CASCADE,
  CONSTRAINT fk_delivery_channel FOREIGN KEY (channel_id) REFERENCES notification_channel(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
