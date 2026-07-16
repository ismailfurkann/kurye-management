-- ─── Couriers ─────────────────────────────────────────────────────────────────
CREATE TABLE couriers (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid(),
    user_id               UUID          NOT NULL,
    vehicle_type          VARCHAR(20)   NOT NULL,
    availability_status   VARCHAR(20)   NOT NULL DEFAULT 'OFFLINE',
    approval_status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    current_latitude      DECIMAL(10,7),
    current_longitude     DECIMAL(10,7),
    last_location_at      TIMESTAMPTZ,
    rating                DECIMAL(3,2)  NOT NULL DEFAULT 0.00,
    active_assignment_count INTEGER     NOT NULL DEFAULT 0,
    version               BIGINT        NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_couriers              PRIMARY KEY (id),
    CONSTRAINT uq_couriers_user_id      UNIQUE (user_id),
    CONSTRAINT fk_couriers_user         FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_couriers_vehicle     CHECK (vehicle_type        IN ('MOTORBIKE','BICYCLE','CAR','WALKING')),
    CONSTRAINT chk_couriers_avail       CHECK (availability_status IN ('AVAILABLE','BUSY','OFFLINE','ON_BREAK')),
    CONSTRAINT chk_couriers_approval    CHECK (approval_status     IN ('PENDING','APPROVED','REJECTED','BLOCKED')),
    CONSTRAINT chk_couriers_rating      CHECK (rating BETWEEN 0.00 AND 5.00),
    CONSTRAINT chk_couriers_active_cnt  CHECK (active_assignment_count >= 0)
);

CREATE INDEX idx_couriers_user_id         ON couriers (user_id);
CREATE INDEX idx_couriers_approval        ON couriers (approval_status);
CREATE INDEX idx_couriers_availability    ON couriers (availability_status);
-- Composite index for the dispatch "available couriers" query
CREATE INDEX idx_couriers_dispatch        ON couriers (approval_status, availability_status);
