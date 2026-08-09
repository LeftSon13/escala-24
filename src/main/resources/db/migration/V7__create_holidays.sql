CREATE TABLE holidays (
    id BIGSERIAL PRIMARY KEY,
    holiday_date DATE NOT NULL,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_holidays_date
        UNIQUE (holiday_date),

    CONSTRAINT ck_holidays_name_not_blank
        CHECK (BTRIM(name) <> '')
);