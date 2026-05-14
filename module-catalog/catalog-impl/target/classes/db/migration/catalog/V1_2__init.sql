CREATE SCHEMA IF NOT EXISTS catalog;

CREATE TABLE catalog.items (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    price_amount NUMERIC(19,2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_items_seller ON catalog.items(seller_id);
CREATE INDEX idx_items_status ON catalog.items(status);
