ALTER TABLE users
    ADD COLUMN email VARCHAR(320),
    ADD COLUMN password_hash VARCHAR(255),
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN deleted_at TIMESTAMPTZ;

-- Existing rows cannot authenticate with the old schema. Keep them unusable and
-- soft-deleted instead of inventing valid credentials.
UPDATE users
SET email = 'legacy+' || id || '@local.invalid',
    password_hash = '!',
    deleted_at = COALESCE(deleted_at, now())
WHERE email IS NULL OR password_hash IS NULL;

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL,
    ALTER COLUMN password_hash SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN'));

CREATE UNIQUE INDEX users_email_active_unique
    ON users (lower(email))
    WHERE deleted_at IS NULL;

CREATE INDEX users_deleted_at_idx ON users (deleted_at);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_set_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
