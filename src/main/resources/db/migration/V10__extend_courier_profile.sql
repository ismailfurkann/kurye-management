ALTER TABLE couriers
    ADD COLUMN plate_number VARCHAR(30),
    ADD COLUMN region_id UUID,
    ADD COLUMN avatar_url VARCHAR(500),
    ADD COLUMN approved_at TIMESTAMPTZ;

ALTER TABLE couriers
    ADD CONSTRAINT fk_couriers_region
    FOREIGN KEY (region_id) REFERENCES regions(id);

CREATE INDEX idx_couriers_region_id ON couriers (region_id);
