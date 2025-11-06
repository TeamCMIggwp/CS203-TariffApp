-- WTO Tariffs schema and tables
CREATE SCHEMA IF NOT EXISTS wto_tariffs;
CREATE TABLE IF NOT EXISTS wto_tariffs.TariffRates (
  country_id           VARCHAR(10)   NOT NULL,
  partner_country_id   VARCHAR(10)   NOT NULL,
  product_id           INT           NOT NULL,
  `year`               INT           NOT NULL,
  rate                 DECIMAL(6,3)  NOT NULL,
  unit                 VARCHAR(20)   NOT NULL,
  PRIMARY KEY (country_id, partner_country_id, product_id, `year`)
);

-- News and sources
CREATE TABLE IF NOT EXISTS News (
  NewsLink  VARCHAR(512) PRIMARY KEY,
  remarks   VARCHAR(100),
  is_hidden BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS UserHiddenSources (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  user_id    VARCHAR(255) NOT NULL,
  news_link  VARCHAR(512) NOT NULL,
  hidden_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, news_link)
);

CREATE TABLE IF NOT EXISTS NewsTariffRates (
  id                   INT AUTO_INCREMENT PRIMARY KEY,
  news_link            VARCHAR(512) NOT NULL,
  country_id           VARCHAR(10) NOT NULL,
  partner_country_id   VARCHAR(10) NOT NULL,
  product_id           INT NOT NULL,
  `year`               INT NOT NULL,
  rate                 DECIMAL(6,3) NOT NULL,
  FOREIGN KEY (news_link) REFERENCES News(NewsLink) ON DELETE CASCADE
);

-- Authentication schema and tables 
CREATE SCHEMA IF NOT EXISTS accounts;

-- Core users table
CREATE TABLE IF NOT EXISTS accounts.users (
  id         VARCHAR(64) NOT NULL PRIMARY KEY,
  email      VARCHAR(191) NOT NULL UNIQUE,
  name       VARCHAR(191),
  country_code VARCHAR(3),
  role       VARCHAR(32) NOT NULL DEFAULT 'user'
);

-- Credentials table for passwords and auth
CREATE TABLE IF NOT EXISTS accounts.user_passwords (
  user_id       VARCHAR(64) NOT NULL PRIMARY KEY,
  password_hash TEXT NOT NULL,
  algorithm     VARCHAR(32) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES accounts.users(id) ON DELETE CASCADE
);

-- OAuth account mappings
CREATE TABLE IF NOT EXISTS accounts.accounts (
  provider VARCHAR(32) NOT NULL,
  provider_account_id VARCHAR(191) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  password_hash TEXT,
  password_algorithm VARCHAR(32),
  PRIMARY KEY (provider, provider_account_id),
  FOREIGN KEY (user_id) REFERENCES accounts.users(id) ON DELETE CASCADE
);

-- Session management
CREATE TABLE IF NOT EXISTS accounts.sessions (
  id VARCHAR(64) NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  FOREIGN KEY (user_id) REFERENCES accounts.users(id) ON DELETE CASCADE
);

-- Password reset tokens
CREATE TABLE IF NOT EXISTS accounts.password_reset_tokens (
  token_hash VARCHAR(128) NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used_at TIMESTAMP,
  requested_ip VARCHAR(45),
  FOREIGN KEY (user_id) REFERENCES accounts.users(id) ON DELETE CASCADE
);
