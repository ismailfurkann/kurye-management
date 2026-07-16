-- ─── Branches ─────────────────────────────────────────────────────────────────
CREATE TABLE branches (
    id            UUID           NOT NULL DEFAULT gen_random_uuid(),
    restaurant_id UUID           NOT NULL,
    region_id     UUID           NOT NULL,
    name          VARCHAR(150)   NOT NULL,
    phone         VARCHAR(20),
    address_text  VARCHAR(500),
    latitude      DECIMAL(10, 7),
    longitude     DECIMAL(10, 7),
    status        VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_branches               PRIMARY KEY (id),
    CONSTRAINT fk_branches_restaurant    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_branches_region        FOREIGN KEY (region_id)     REFERENCES regions(id),
    CONSTRAINT uq_branches_name_per_rest UNIQUE (name, restaurant_id),
    CONSTRAINT chk_branches_status       CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_branches_restaurant_id ON branches (restaurant_id);
CREATE INDEX idx_branches_region_id     ON branches (region_id);
CREATE INDEX idx_branches_status        ON branches (status);
