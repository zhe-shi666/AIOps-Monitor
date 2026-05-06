ALTER TABLE users
  ADD COLUMN notification_email VARCHAR(255) NULL AFTER email;

ALTER TABLE users
  ADD COLUMN notification_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER notification_email;
