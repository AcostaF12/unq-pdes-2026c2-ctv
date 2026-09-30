ALTER TABLE purchases
    ADD COLUMN buyer_snapshot_id BIGINT;
ALTER TABLE purchases ADD COLUMN buyer_snapshot_username VARCHAR(60);
ALTER TABLE purchases ADD COLUMN buyer_snapshot_role VARCHAR(20);
ALTER TABLE purchases ADD COLUMN buyer_snapshot_first_name VARCHAR(80);
ALTER TABLE purchases ADD COLUMN buyer_snapshot_last_name VARCHAR(80);
ALTER TABLE purchases ADD COLUMN package_snapshot_id BIGINT;
ALTER TABLE purchases ADD COLUMN package_snapshot_name VARCHAR(160);
ALTER TABLE purchases ADD COLUMN package_snapshot_price NUMERIC(12, 2);
ALTER TABLE purchases ADD COLUMN agency_snapshot_id BIGINT;
ALTER TABLE purchases ADD COLUMN agency_snapshot_name VARCHAR(160);
ALTER TABLE purchases ADD COLUMN hotel_snapshot_id BIGINT;
ALTER TABLE purchases ADD COLUMN hotel_snapshot_name VARCHAR(160);
ALTER TABLE purchases ADD COLUMN hotel_snapshot_city_code VARCHAR(3);
ALTER TABLE purchases ADD COLUMN hotel_snapshot_city_name VARCHAR(100);
ALTER TABLE purchases ADD COLUMN hotel_snapshot_photo_url VARCHAR(500);
ALTER TABLE purchases ADD COLUMN origin_snapshot_code VARCHAR(3);
ALTER TABLE purchases ADD COLUMN origin_snapshot_name VARCHAR(100);
ALTER TABLE purchases ADD COLUMN destination_snapshot_code VARCHAR(3);
ALTER TABLE purchases ADD COLUMN destination_snapshot_name VARCHAR(100);
ALTER TABLE purchases ADD COLUMN outbound_flight_snapshot_id BIGINT;
ALTER TABLE purchases ADD COLUMN return_flight_snapshot_id BIGINT;

UPDATE purchases SET
    buyer_snapshot_id = (SELECT u.id FROM users u WHERE u.id = purchases.buyer_id),
    buyer_snapshot_username = (SELECT u.username FROM users u WHERE u.id = purchases.buyer_id),
    buyer_snapshot_role = (SELECT u.role FROM users u WHERE u.id = purchases.buyer_id),
    buyer_snapshot_first_name = (SELECT u.first_name FROM users u WHERE u.id = purchases.buyer_id),
    buyer_snapshot_last_name = (SELECT u.last_name FROM users u WHERE u.id = purchases.buyer_id),
    package_snapshot_id = (SELECT tp.id FROM packages tp WHERE tp.id = purchases.package_id),
    package_snapshot_name = (SELECT tp.name FROM packages tp WHERE tp.id = purchases.package_id),
    package_snapshot_price = purchases.purchase_price,
    agency_snapshot_id = (SELECT a.id FROM agencies a WHERE a.id = purchases.agency_id),
    agency_snapshot_name = (SELECT a.name FROM agencies a WHERE a.id = purchases.agency_id),
    hotel_snapshot_id = (SELECT h.id FROM packages tp JOIN hotels h ON h.id = tp.hotel_id WHERE tp.id = purchases.package_id),
    hotel_snapshot_name = (SELECT h.name FROM packages tp JOIN hotels h ON h.id = tp.hotel_id WHERE tp.id = purchases.package_id),
    hotel_snapshot_city_code = (SELECT hc.code FROM packages tp JOIN hotels h ON h.id = tp.hotel_id JOIN cities hc ON hc.code = h.city_code WHERE tp.id = purchases.package_id),
    hotel_snapshot_city_name = (SELECT hc.name FROM packages tp JOIN hotels h ON h.id = tp.hotel_id JOIN cities hc ON hc.code = h.city_code WHERE tp.id = purchases.package_id),
    hotel_snapshot_photo_url = (SELECT h.photo_url FROM packages tp JOIN hotels h ON h.id = tp.hotel_id WHERE tp.id = purchases.package_id),
    origin_snapshot_code = (SELECT c.code FROM packages tp JOIN cities c ON c.code = tp.origin_city_code WHERE tp.id = purchases.package_id),
    origin_snapshot_name = (SELECT c.name FROM packages tp JOIN cities c ON c.code = tp.origin_city_code WHERE tp.id = purchases.package_id),
    destination_snapshot_code = (SELECT c.code FROM packages tp JOIN cities c ON c.code = tp.destination_city_code WHERE tp.id = purchases.package_id),
    destination_snapshot_name = (SELECT c.name FROM packages tp JOIN cities c ON c.code = tp.destination_city_code WHERE tp.id = purchases.package_id),
    outbound_flight_snapshot_id = (SELECT tp.outbound_flight_id FROM packages tp WHERE tp.id = purchases.package_id),
    return_flight_snapshot_id = (SELECT tp.return_flight_id FROM packages tp WHERE tp.id = purchases.package_id);

ALTER TABLE purchases
    ALTER COLUMN buyer_snapshot_id SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN buyer_snapshot_username SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN buyer_snapshot_role SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN buyer_snapshot_first_name SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN buyer_snapshot_last_name SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN package_snapshot_id SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN package_snapshot_name SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN package_snapshot_price SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN agency_snapshot_id SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN agency_snapshot_name SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN hotel_snapshot_id SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN hotel_snapshot_name SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN hotel_snapshot_city_code SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN hotel_snapshot_city_name SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN hotel_snapshot_photo_url SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN origin_snapshot_code SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN origin_snapshot_name SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN destination_snapshot_code SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN destination_snapshot_name SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN outbound_flight_snapshot_id SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN return_flight_snapshot_id SET NOT NULL;

CREATE INDEX idx_purchases_buyer_history
    ON purchases (buyer_id, purchased_at DESC, id DESC);

CREATE INDEX idx_purchases_agency_history
    ON purchases (agency_id, purchased_at DESC, id DESC);

CREATE INDEX idx_purchases_agency_buyer_history
    ON purchases (agency_id, buyer_snapshot_username, purchased_at DESC, id DESC);
