ALTER TABLE users
  ADD COLUMN password_change_required TINYINT(1) NOT NULL DEFAULT 0 AFTER enabled;

ALTER TABLE users
  ADD COLUMN temporary_password_issued_at DATETIME NULL AFTER password_change_required;
