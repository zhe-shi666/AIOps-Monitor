-- Phase 2 closure: logs + traces ingest, incident aggregation/dedup fields
-- Date: 2026-04-28

CREATE TABLE IF NOT EXISTS log_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_id BIGINT DEFAULT NULL,
  hostname VARCHAR(255) DEFAULT NULL,
  service_name VARCHAR(100) DEFAULT NULL,
  source VARCHAR(50) DEFAULT NULL,
  level VARCHAR(20) DEFAULT 'INFO',
  message TEXT,
  trace_id VARCHAR(128) DEFAULT NULL,
  labels_json LONGTEXT,
  occurred_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_log_record_user_time (user_id, occurred_at),
  KEY idx_log_record_target_time (target_id, occurred_at),
  KEY idx_log_record_trace_id (trace_id),
  CONSTRAINT fk_log_record_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_log_record_target FOREIGN KEY (target_id) REFERENCES monitor_targets(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS trace_span (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_id BIGINT DEFAULT NULL,
  hostname VARCHAR(255) DEFAULT NULL,
  service_name VARCHAR(100) DEFAULT NULL,
  trace_id VARCHAR(128) NOT NULL,
  span_id VARCHAR(128) DEFAULT NULL,
  parent_span_id VARCHAR(128) DEFAULT NULL,
  operation_name VARCHAR(200) DEFAULT NULL,
  duration_ms BIGINT DEFAULT NULL,
  status VARCHAR(30) DEFAULT NULL,
  attributes_json LONGTEXT,
  started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_trace_span_user_time (user_id, started_at),
  KEY idx_trace_span_target_time (target_id, started_at),
  KEY idx_trace_span_trace_id (trace_id),
  KEY idx_trace_span_service_time (service_name, started_at),
  CONSTRAINT fk_trace_span_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_trace_span_target FOREIGN KEY (target_id) REFERENCES monitor_targets(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @db_name = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND COLUMN_NAME = 'fingerprint') = 0,
    'ALTER TABLE incident_log ADD COLUMN fingerprint VARCHAR(120) DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND COLUMN_NAME = 'occurrence_count') = 0,
    'ALTER TABLE incident_log ADD COLUMN occurrence_count INT NOT NULL DEFAULT 1',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND COLUMN_NAME = 'suppressed_count') = 0,
    'ALTER TABLE incident_log ADD COLUMN suppressed_count INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND COLUMN_NAME = 'first_seen_at') = 0,
    'ALTER TABLE incident_log ADD COLUMN first_seen_at DATETIME DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND COLUMN_NAME = 'last_seen_at') = 0,
    'ALTER TABLE incident_log ADD COLUMN last_seen_at DATETIME DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND COLUMN_NAME = 'source_type') = 0,
    'ALTER TABLE incident_log ADD COLUMN source_type VARCHAR(30) NOT NULL DEFAULT ''METRIC''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND COLUMN_NAME = 'source_ref') = 0,
    'ALTER TABLE incident_log ADD COLUMN source_ref VARCHAR(255) DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND COLUMN_NAME = 'service_name') = 0,
    'ALTER TABLE incident_log ADD COLUMN service_name VARCHAR(100) DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND COLUMN_NAME = 'trace_id') = 0,
    'ALTER TABLE incident_log ADD COLUMN trace_id VARCHAR(128) DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE incident_log
SET first_seen_at = COALESCE(first_seen_at, created_at),
    last_seen_at = COALESCE(last_seen_at, created_at)
WHERE first_seen_at IS NULL OR last_seen_at IS NULL;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND INDEX_NAME = 'idx_incident_user_fingerprint_status') = 0,
    'CREATE INDEX idx_incident_user_fingerprint_status ON incident_log (user_id, fingerprint, status)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND INDEX_NAME = 'idx_incident_target_metric_status') = 0,
    'CREATE INDEX idx_incident_target_metric_status ON incident_log (target_id, metric_name, status)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'incident_log'
       AND INDEX_NAME = 'idx_incident_last_seen_at') = 0,
    'CREATE INDEX idx_incident_last_seen_at ON incident_log (last_seen_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
