-- Trigram index backing the free-text package name filter.
-- LOWER(name) LIKE '%text%' cannot be served by a B-tree index; pg_trgm's
-- GIN index accelerates that access pattern without changing the query itself.
-- Only applies to PostgreSQL (spring.sql.init.platform=postgresql); tests run
-- against H2 and never load this script.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_packages_name_trgm ON packages USING gin (lower(name) gin_trgm_ops);
