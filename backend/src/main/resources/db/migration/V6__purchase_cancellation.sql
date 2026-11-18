ALTER TABLE purchases ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE purchases ADD COLUMN cancelled_at TIMESTAMP;
ALTER TABLE purchases ADD COLUMN outbound_sale_id BIGINT;
ALTER TABLE purchases ADD COLUMN return_sale_id BIGINT;

ALTER TABLE purchases ADD CONSTRAINT ck_purchases_status CHECK (
    (status = 'ACTIVE' AND cancelled_at IS NULL)
    OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
);

CREATE INDEX idx_purchases_buyer_package_status ON purchases(buyer_id, package_id, status);
