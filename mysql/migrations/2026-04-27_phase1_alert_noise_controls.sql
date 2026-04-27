-- Phase 1 - Alert noise controls (consecutive trigger + silence window)
-- Date: 2026-04-27

ALTER TABLE alert_threshold_config
  ADD COLUMN IF NOT EXISTS consecutive_breach_count INT NOT NULL DEFAULT 2,
  ADD COLUMN IF NOT EXISTS silence_seconds INT NOT NULL DEFAULT 180;

UPDATE alert_threshold_config
SET consecutive_breach_count = 2
WHERE consecutive_breach_count IS NULL OR consecutive_breach_count < 1;

UPDATE alert_threshold_config
SET silence_seconds = 180
WHERE silence_seconds IS NULL OR silence_seconds < 10;
