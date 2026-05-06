ALTER TABLE notification_channel
  MODIFY COLUMN webhook_url VARCHAR(500) NULL;

ALTER TABLE notification_channel
  ADD COLUMN email_to VARCHAR(255) NULL AFTER webhook_url;
