ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE users ADD CONSTRAINT ck_users_role_agency CHECK (
    (role = 'AGENCY' AND user_type = 'AGENCY' AND agency_id IS NOT NULL)
    OR (role IN ('BUYER', 'ADMIN') AND user_type = 'STANDARD' AND agency_id IS NULL)
);
