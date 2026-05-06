-- Enterprise security and release metadata baseline.
-- Safe on existing databases bootstrapped before Flyway.

ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN','OPS','AUDITOR','USER') NOT NULL DEFAULT 'USER';

CREATE TABLE IF NOT EXISTS password_reset_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at DATETIME DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_password_reset_token_hash (token_hash),
  KEY idx_password_reset_user_created (user_id, created_at),
  KEY idx_password_reset_expires (expires_at),
  CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS agent_release (
  id BIGINT NOT NULL AUTO_INCREMENT,
  version VARCHAR(64) NOT NULL,
  package_name VARCHAR(128) NOT NULL,
  sha256 VARCHAR(128) NOT NULL,
  signature VARCHAR(256) DEFAULT NULL,
  release_notes TEXT,
  mandatory TINYINT(1) NOT NULL DEFAULT 0,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_agent_release_version (version),
  KEY idx_agent_release_active_created (active, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
