CREATE TABLE monthly_schedules (
    id BIGSERIAL PRIMARY KEY,
    schedule_year INTEGER NOT NULL,
    schedule_month INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_monthly_schedules_year_month
        UNIQUE (schedule_year, schedule_month),

    CONSTRAINT ck_monthly_schedules_month
        CHECK (schedule_month BETWEEN 1 AND 12),

    CONSTRAINT ck_monthly_schedules_status
        CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

CREATE TABLE duty_assignments (
    id BIGSERIAL PRIMARY KEY,
    monthly_schedule_id BIGINT NOT NULL,
    firefighter_id BIGINT NOT NULL,
    duty_date DATE NOT NULL,
    day_type VARCHAR(30) NOT NULL,

    CONSTRAINT fk_duty_assignments_monthly_schedule
        FOREIGN KEY (monthly_schedule_id)
        REFERENCES monthly_schedules (id),

    CONSTRAINT fk_duty_assignments_firefighter
        FOREIGN KEY (firefighter_id)
        REFERENCES firefighters (id),

    CONSTRAINT uk_duty_assignments_schedule_date
        UNIQUE (monthly_schedule_id, duty_date),

    CONSTRAINT ck_duty_assignments_day_type
        CHECK (day_type IN ('WEEKDAY', 'WEEKEND_OR_HOLIDAY'))
);

CREATE INDEX idx_duty_assignments_firefighter_date
    ON duty_assignments (firefighter_id, duty_date);