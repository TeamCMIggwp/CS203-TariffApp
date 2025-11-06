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

-- Auth tables (for authentication integration tests)
CREATE TABLE IF NOT EXISTS accounts (
  user_id      VARCHAR(255) PRIMARY KEY,
  email        VARCHAR(255) UNIQUE NOT NULL,
  password     VARCHAR(255),
  provider     VARCHAR(50),
  role         VARCHAR(50) DEFAULT 'user',
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
  token_id     VARCHAR(255) PRIMARY KEY,
  user_id      VARCHAR(255) NOT NULL,
  expires_at   TIMESTAMP NOT NULL,
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES accounts(user_id) ON DELETE CASCADE
);
