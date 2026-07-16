-- ─── Users ───────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    phone       VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    role        VARCHAR(30)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role   CHECK (role   IN ('PLATFORM_ADMIN','PLATFORM_MANAGER','RESTAURANT_OWNER','RESTAURANT_STAFF','COURIER')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','PASSIVE','SUSPENDED'))
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_status ON users (status);
