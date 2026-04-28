-- Phase 2 finalization - enterprise workflow fields
-- Date: 2026-04-27

SET @db_name = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'ai_action_plan'
       AND COLUMN_NAME = 'approved_by') = 0,
    'ALTER TABLE ai_action_plan ADD COLUMN approved_by VARCHAR(100) NULL AFTER status',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'ai_action_plan'
       AND COLUMN_NAME = 'approved_at') = 0,
    'ALTER TABLE ai_action_plan ADD COLUMN approved_at DATETIME NULL AFTER approved_by',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'ai_action_plan'
       AND COLUMN_NAME = 'approval_note') = 0,
    'ALTER TABLE ai_action_plan ADD COLUMN approval_note TEXT NULL AFTER approved_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'ai_action_plan'
       AND COLUMN_NAME = 'retry_count') = 0,
    'ALTER TABLE ai_action_plan ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER approval_note',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = @db_name
       AND TABLE_NAME = 'ai_action_plan'
       AND INDEX_NAME = 'idx_ai_action_plan_approved_at') = 0,
    'CREATE INDEX idx_ai_action_plan_approved_at ON ai_action_plan (approved_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
