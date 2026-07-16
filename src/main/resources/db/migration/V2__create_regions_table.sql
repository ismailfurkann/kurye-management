-- ─── Regions ─────────────────────────────────────────────────────────────────
CREATE TABLE regions (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    district    VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_regions PRIMARY KEY (id),
    CONSTRAINT uq_regions_name_city_district UNIQUE (name, city, district),
    CONSTRAINT chk_regions_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_regions_city        ON regions (city);
CREATE INDEX idx_regions_district    ON regions (district);
CREATE INDEX idx_regions_status      ON regions (status);
CREATE INDEX idx_regions_city_status ON regions (city, status);
