CREATE TABLE loans (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_email VARCHAR(180) NOT NULL,
    user_name VARCHAR(120) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    term_months INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at TIMESTAMPTZ,
    decided_by VARCHAR(180),
    CONSTRAINT loans_email_not_blank CHECK (btrim(user_email) <> ''),
    CONSTRAINT loans_name_not_blank CHECK (btrim(user_name) <> ''),
    CONSTRAINT loans_amount_range CHECK (amount BETWEEN 100 AND 50000),
    CONSTRAINT loans_term_range CHECK (term_months BETWEEN 1 AND 60),
    CONSTRAINT loans_status_valid CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_loans_user_requested ON loans (user_email, requested_at DESC);
CREATE INDEX idx_loans_status ON loans (status);

INSERT INTO loans (user_email, user_name, amount, term_months, status, requested_at, decided_at, decided_by)
VALUES
    ('usuario@test.com', 'Usuario', 1000.00, 12, 'PENDING', now() - interval '2 days', NULL, NULL),
    ('usuario@test.com', 'Usuario', 2000.00, 24, 'APPROVED', now() - interval '5 days', now() - interval '4 days', 'admin@test.com'),
    ('usuario@test.com', 'Usuario', 30000.00, 60, 'PENDING', now() - interval '1 day', NULL, NULL);
