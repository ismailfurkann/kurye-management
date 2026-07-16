-- ─── Commissions ──────────────────────────────────────────────────────────────
-- One commission record per completed delivery assignment.
CREATE TABLE commissions (
    id                      UUID           NOT NULL DEFAULT gen_random_uuid(),
    delivery_assignment_id  UUID           NOT NULL,
    delivery_order_id       UUID           NOT NULL,
    courier_id              UUID           NOT NULL,
    gross_amount            DECIMAL(10,2)  NOT NULL,   -- delivery_fee from order
    commission_rate         DECIMAL(5,4)   NOT NULL DEFAULT 1.0000, -- future: partial rates
    net_amount              DECIMAL(10,2)  NOT NULL,   -- gross * rate
    status                  VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    payment_period          VARCHAR(7),                -- YYYY-MM for batch payment grouping
    paid_at                 TIMESTAMPTZ,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_commissions                   PRIMARY KEY (id),
    CONSTRAINT uq_commissions_per_assignment    UNIQUE (delivery_assignment_id),
    CONSTRAINT fk_comm_assignment               FOREIGN KEY (delivery_assignment_id) REFERENCES delivery_assignments(id),
    CONSTRAINT fk_comm_order                    FOREIGN KEY (delivery_order_id)       REFERENCES delivery_orders(id),
    CONSTRAINT fk_comm_courier                  FOREIGN KEY (courier_id)              REFERENCES couriers(id),
    CONSTRAINT chk_comm_status                  CHECK (status IN ('PENDING','PAID','CANCELLED')),
    CONSTRAINT chk_comm_amounts                 CHECK (gross_amount >= 0 AND net_amount >= 0 AND commission_rate BETWEEN 0 AND 1)
);

CREATE INDEX idx_comm_courier_id      ON commissions (courier_id);
CREATE INDEX idx_comm_status          ON commissions (status);
CREATE INDEX idx_comm_payment_period  ON commissions (payment_period);
CREATE INDEX idx_comm_created         ON commissions (created_at);

-- ─── Audit Logs ───────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id           UUID          NOT NULL DEFAULT gen_random_uuid(),
    actor_id     UUID,                               -- NULL for system actions
    action       VARCHAR(80)   NOT NULL,             -- e.g. ORDER_CANCELLED, COURIER_APPROVED
    entity_type  VARCHAR(50)   NOT NULL,             -- DeliveryOrder, Courier, etc.
    entity_id    UUID          NOT NULL,
    details      TEXT,                               -- JSON summary of the change
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_actor      ON audit_logs (actor_id);
CREATE INDEX idx_audit_entity     ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_action     ON audit_logs (action);
CREATE INDEX idx_audit_created    ON audit_logs (created_at DESC);
