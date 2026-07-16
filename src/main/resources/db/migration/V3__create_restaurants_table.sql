-- ─── Restaurants ─────────────────────────────────────────────────────────────
CREATE TABLE restaurants (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    legal_name  VARCHAR(200),
    tax_number  VARCHAR(20),
    phone       VARCHAR(20),
    email       VARCHAR(150),
    owner_id    UUID         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_restaurants          PRIMARY KEY (id),
    CONSTRAINT fk_restaurants_owner    FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT chk_restaurants_status  CHECK (status IN ('ACTIVE','PASSIVE','PENDING','SUSPENDED'))
);

-- Partial unique index: only enforce uniqueness when tax_number is provided
CREATE UNIQUE INDEX uq_restaurants_tax_number
    ON restaurants (tax_number)
    WHERE tax_number IS NOT NULL;

CREATE INDEX idx_restaurants_owner_id ON restaurants (owner_id);
CREATE INDEX idx_restaurants_status   ON restaurants (status);
CREATE INDEX idx_restaurants_name     ON restaurants (lower(name));
