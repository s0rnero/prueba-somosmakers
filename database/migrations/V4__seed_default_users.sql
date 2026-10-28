INSERT INTO users (name, email, password_hash, role)
SELECT 'Andres Valencia', 'admin@test.com',
       '$2a$12$rw9T8XfhJdO0bAhzJPsX5utO3aVkmp78kwZ1GyhefrIFtKu1PBlHa', 'ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE lower(email) = 'admin@test.com' AND deleted_at IS NULL
);

INSERT INTO users (name, email, password_hash, role)
SELECT 'Cesar Rios', 'usuario@test.com',
       '$2a$12$YIqYA3t28Qtmm7OoDtt7sOGMYq7Ks.bPuZ3aFm0r4c.JKi2clHccG', 'USER'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE lower(email) = 'usuario@test.com' AND deleted_at IS NULL
);
