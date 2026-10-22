CREATE TABLE demo_seed_runs (
    id VARCHAR(60) PRIMARY KEY,
    completed_at TIMESTAMP
);
INSERT INTO demo_seed_runs(id) VALUES ('ctv-demo-v1');
ALTER TABLE packages ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX idx_packages_active_destination ON packages(active, destination_city_code);
