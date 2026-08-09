CREATE TABLE unavailabilities (
    id BIGSERIAL PRIMARY KEY,
    firefighter_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by_user_id BIGINT,
    reviewed_at TIMESTAMP,

    CONSTRAINT fk_unavailabilities_firefighter
        FOREIGN KEY (firefighter_id)
        REFERENCES firefighters (id),

    CONSTRAINT fk_unavailabilities_reviewed_by_user
        FOREIGN KEY (reviewed_by_user_id)
        REFERENCES users (id),

    CONSTRAINT ck_unavailabilities_type
        CHECK (
            type IN (
                'VACATION',
                'MEDICAL_LEAVE',
                'PERSONAL_COMMITMENT',
                'OTHER'
            )
        ),

    CONSTRAINT ck_unavailabilities_status
        CHECK (
            status IN (
                'PENDING',
                'APPROVED',
                'REJECTED'
            )
        ),

    CONSTRAINT ck_unavailabilities_period
        CHECK (end_date >= start_date),

    CONSTRAINT ck_unavailabilities_review
        CHECK (
            (
                status = 'PENDING'
                AND reviewed_by_user_id IS NULL
                AND reviewed_at IS NULL
            )
            OR
            (
                status IN ('APPROVED', 'REJECTED')
                AND reviewed_by_user_id IS NOT NULL
                AND reviewed_at IS NOT NULL
            )
        )
);

CREATE INDEX idx_unavailabilities_firefighter_period
    ON unavailabilities (firefighter_id, start_date, end_date);

CREATE INDEX idx_unavailabilities_status_period
    ON unavailabilities (status, start_date, end_date);