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
