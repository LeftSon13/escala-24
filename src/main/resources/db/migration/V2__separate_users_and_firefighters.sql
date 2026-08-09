ALTER TABLE employees RENAME TO users;

ALTER TABLE users
    RENAME CONSTRAINT employees_pkey TO users_pkey;

ALTER TABLE users
    RENAME CONSTRAINT employees_email_key TO users_email_key;

ALTER TABLE users
    ADD COLUMN role VARCHAR(20);

UPDATE users
SET role = CASE
    WHEN administrator = TRUE THEN 'ADMIN'
    ELSE 'FIREFIGHTER'
END;

ALTER TABLE users
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
        CHECK (role IN ('ADMIN', 'FIREFIGHTER'));

CREATE TABLE firefighters (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    registration VARCHAR(50) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,

    CONSTRAINT fk_firefighters_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
);

INSERT INTO firefighters (user_id, registration, phone)
SELECT id, registration, phone
FROM users
WHERE administrator = FALSE;

ALTER TABLE users
    DROP COLUMN registration,
    DROP COLUMN phone,
    DROP COLUMN administrator;