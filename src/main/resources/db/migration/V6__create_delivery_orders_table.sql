-- ─── Delivery Orders ──────────────────────────────────────────────────────────
CREATE TABLE delivery_orders (
    id                      UUID           NOT NULL DEFAULT gen_random_uuid(),
    branch_id               UUID           NOT NULL,
    order_code              VARCHAR(30)    NOT NULL,
    customer_name           VARCHAR(100)   NOT NULL,
    customer_phone          VARCHAR(20)    NOT NULL,
    customer_address_text   VARCHAR(500)   NOT NULL,
    customer_latitude       DECIMAL(10,7),
    customer_longitude      DECIMAL(10,7),
    order_amount            DECIMAL(12,2)  NOT NULL,
    delivery_fee            DECIMAL(10,2)  NOT NULL,
    payment_collection_type VARCHAR(30)    NOT NULL,
    delivery_note           VARCHAR(500),
    status                  VARCHAR(30)    NOT NULL DEFAULT 'CREATED',
    priority                VARCHAR(20)    NOT NULL DEFAULT 'NORMAL',
    created_by              UUID           NOT NULL,
    submitted_at            TIMESTAMPTZ,
    assigned_at             TIMESTAMPTZ,
    picked_up_at            TIMESTAMPTZ,
    delivered_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    failed_at               TIMESTAMPTZ,
    cancellation_reason     VARCHAR(500),
    failure_reason          VARCHAR(500),
    version                 BIGINT         NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_delivery_orders              PRIMARY KEY (id),
    CONSTRAINT uq_delivery_orders_code_branch  UNIQUE (order_code, branch_id),
    CONSTRAINT fk_delivery_orders_branch       FOREIGN KEY (branch_id)  REFERENCES branches(id),
    CONSTRAINT fk_delivery_orders_created_by   FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_delivery_orders_status      CHECK (status IN (
        'CREATED','READY_FOR_DISPATCH','ASSIGNED','PICKED_UP','DELIVERED','CANCELLED','FAILED'
    )),
    CONSTRAINT chk_delivery_orders_payment     CHECK (payment_collection_type IN (
        'NO_COLLECTION','CASH_ON_DELIVERY','CARD_ON_DELIVERY','PAID_ONLINE'
    )),
    CONSTRAINT chk_delivery_orders_priority    CHECK (priority IN ('NORMAL','HIGH','URGENT')),
    CONSTRAINT chk_delivery_orders_amounts     CHECK (order_amount >= 0 AND delivery_fee >= 0)
);

CREATE INDEX idx_do_branch_id   ON delivery_orders (branch_id);
CREATE INDEX idx_do_status      ON delivery_orders (status);
CREATE INDEX idx_do_created_by  ON delivery_orders (created_by);
CREATE INDEX idx_do_submitted   ON delivery_orders (submitted_at) WHERE status = 'READY_FOR_DISPATCH';
CREATE INDEX idx_do_active      ON delivery_orders (status) WHERE status IN ('ASSIGNED','PICKED_UP');

-- ─── Delivery Status History ──────────────────────────────────────────────────
CREATE TABLE delivery_status_history (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    delivery_order_id   UUID         NOT NULL,
    old_status          VARCHAR(30),
    new_status          VARCHAR(30)  NOT NULL,
    changed_by_user_id  UUID         NOT NULL,
    changed_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    note                VARCHAR(500),

    CONSTRAINT pk_delivery_status_history  PRIMARY KEY (id),
    CONSTRAINT fk_dsh_order                FOREIGN KEY (delivery_order_id) REFERENCES delivery_orders(id),
    CONSTRAINT fk_dsh_user                 FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_dsh_order_id ON delivery_status_history (delivery_order_id);
CREATE INDEX idx_dsh_changed  ON delivery_status_history (changed_at);
