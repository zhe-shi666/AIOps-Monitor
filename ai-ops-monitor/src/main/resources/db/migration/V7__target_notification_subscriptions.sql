CREATE TABLE IF NOT EXISTS target_notification_subscription (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  target_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_target_notification_subscription UNIQUE (target_id, user_id),
  CONSTRAINT fk_target_notification_subscription_target FOREIGN KEY (target_id) REFERENCES monitor_targets(id) ON DELETE CASCADE,
  CONSTRAINT fk_target_notification_subscription_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  KEY idx_target_notification_subscription_target_enabled (target_id, enabled),
  KEY idx_target_notification_subscription_user_enabled (user_id, enabled)
);

INSERT INTO target_notification_subscription (target_id, user_id, enabled, created_at, updated_at)
SELECT mt.id, mt.user_id, 1, NOW(), NOW()
FROM monitor_targets mt
LEFT JOIN target_notification_subscription tns
  ON tns.target_id = mt.id AND tns.user_id = mt.user_id
WHERE mt.user_id IS NOT NULL
  AND tns.id IS NULL;
