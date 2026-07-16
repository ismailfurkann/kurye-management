-- ─── Delivery Assignments ─────────────────────────────────────────────────────
CREATE TABLE delivery_assignments (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    delivery_order_id   UUID          NOT NULL,
    courier_id          UUID          NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ASSIGNED',
    assigned_by         UUID          NOT NULL,
    assigned_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    accepted_at         TIMESTAMPTZ,
    picked_up_at        TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    failed_at           TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    failure_reason      VARCHAR(500),
    cancellation_reason VARCHAR(500),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_delivery_assignments           PRIMARY KEY (id),
    CONSTRAINT fk_da_delivery_order              FOREIGN KEY (delivery_order_id) REFERENCES delivery_orders(id),
    CONSTRAINT fk_da_courier                     FOREIGN KEY (courier_id)        REFERENCES couriers(id),
    CONSTRAINT fk_da_assigned_by                 FOREIGN KEY (assigned_by)       REFERENCES users(id),
    CONSTRAINT chk_da_status                     CHECK (status IN (
        'ASSIGNED','ACCEPTED','PICKED_UP','DELIVERED','REJECTED','CANCELLED','FAILED'
    ))
);

-- Only one ACTIVE assignment per order at a time (ASSIGNED or ACCEPTED or PICKED_UP).
-- This is the DB-level safety net — application also checks before inserting.
CREATE UNIQUE INDEX uq_da_one_active_per_order
    ON delivery_assignments (delivery_order_id)
    WHERE status IN ('ASSIGNED', 'ACCEPTED', 'PICKED_UP');

CREATE INDEX idx_da_delivery_order ON delivery_assignments (delivery_order_id);
CREATE INDEX idx_da_courier        ON delivery_assignments (courier_id);
CREATE INDEX idx_da_status         ON delivery_assignments (status);
CREATE INDEX idx_da_assigned_at    ON delivery_assignments (assigned_at);
