CREATE TABLE platform_mail_settings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  smtp_host VARCHAR(255) NOT NULL,
  smtp_port INT NOT NULL DEFAULT 587,
  smtp_username VARCHAR(255) NOT NULL,
  smtp_password VARCHAR(500) NOT NULL,
  smtp_auth TINYINT(1) NOT NULL DEFAULT 1,
  smtp_starttls TINYINT(1) NOT NULL DEFAULT 1,
  from_address VARCHAR(255) NOT NULL,
  from_name VARCHAR(100) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  updated_by VARCHAR(50) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL
);
