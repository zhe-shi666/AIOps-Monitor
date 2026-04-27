-- Phase 1 - Metric extension (disk/network/process)
-- Date: 2026-04-27

ALTER TABLE system_metrics_history
  ADD COLUMN IF NOT EXISTS disk_usage DOUBLE DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS net_rx_bytes_per_sec DOUBLE DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS net_tx_bytes_per_sec DOUBLE DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS process_count INT DEFAULT NULL;

ALTER TABLE incident_log
  ADD COLUMN IF NOT EXISTS user_id BIGINT DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS target_id BIGINT DEFAULT NULL;
