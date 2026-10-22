ALTER TABLE flights ADD COLUMN seed_key VARCHAR(80) UNIQUE;
ALTER TABLE flight_sales ADD COLUMN seed_key VARCHAR(300) UNIQUE;
CREATE INDEX idx_flight_sales_flight ON flight_sales(flight_id);
