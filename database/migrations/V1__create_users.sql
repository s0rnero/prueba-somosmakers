CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT users_name_not_blank CHECK (btrim(name) <> '')
);
