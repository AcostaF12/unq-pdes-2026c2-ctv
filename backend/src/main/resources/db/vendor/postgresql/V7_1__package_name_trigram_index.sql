-- PostgreSQL-only migration (Flyway location db/vendor/{vendor}); H2 never loads it.
-- Trigram index backing the free-text package name filter:
-- LOWER(name) LIKE '%text%' cannot be served by a B-tree index; pg_trgm's GIN
-- index accelerates that access pattern without changing the query itself.
-- pg_trgm ships with the official postgres image and is a trusted extension,
-- so the database owner used by the app can create it.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_packages_name_trgm ON packages USING gin (lower(name) gin_trgm_ops);
