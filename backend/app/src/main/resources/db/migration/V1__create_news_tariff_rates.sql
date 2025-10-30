-- Create TariffRates table for news-linked tariff data
-- This is separate from the existing WITS tariff data table

CREATE TABLE IF NOT EXISTS TariffRates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    news_link VARCHAR(2000) NOT NULL UNIQUE,
    country_id VARCHAR(100),
    partner_country_id VARCHAR(100),
    product_id VARCHAR(255),
    tariff_type_id VARCHAR(100),
    year INT,
    rate DECIMAL(10,2),
    unit VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_news_link (news_link(255)),
    INDEX idx_country_year (country_id, year),
    INDEX idx_product (product_id(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
