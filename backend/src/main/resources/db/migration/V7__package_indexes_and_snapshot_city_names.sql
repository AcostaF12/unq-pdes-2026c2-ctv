-- B-tree indexes declared on TravelPackage (@Table indexes) that no previous
-- migration created: with ddl-auto=validate Hibernate neither creates nor
-- checks them, so they have to live here.
CREATE INDEX IF NOT EXISTS idx_packages_price ON packages (price);
CREATE INDEX IF NOT EXISTS idx_packages_route_price ON packages (origin_city_code, destination_city_code, price);
CREATE INDEX IF NOT EXISTS idx_packages_destination_price ON packages (destination_city_code, price);

-- City name snapshots must fit any cities.name (VARCHAR(255)); V2 created them as VARCHAR(100).
ALTER TABLE purchases ALTER COLUMN hotel_snapshot_city_name SET DATA TYPE VARCHAR(255);
ALTER TABLE purchases ALTER COLUMN origin_snapshot_name SET DATA TYPE VARCHAR(255);
ALTER TABLE purchases ALTER COLUMN destination_snapshot_name SET DATA TYPE VARCHAR(255);
