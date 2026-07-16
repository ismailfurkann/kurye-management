CREATE INDEX IF NOT EXISTS idx_dsh_order_changed
    ON delivery_status_history (delivery_order_id, changed_at);

CREATE INDEX IF NOT EXISTS idx_da_courier_status_assigned_at
    ON delivery_assignments (courier_id, status, assigned_at DESC);

CREATE INDEX IF NOT EXISTS idx_comm_courier_status_period
    ON commissions (courier_id, status, payment_period);

CREATE INDEX IF NOT EXISTS idx_do_branch_status_priority_created
    ON delivery_orders (branch_id, status, priority, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_do_status_created
    ON delivery_orders (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_restaurants_email_lower
    ON restaurants (lower(email));

CREATE INDEX IF NOT EXISTS idx_restaurants_legal_name_lower
    ON restaurants (lower(legal_name));

CREATE INDEX IF NOT EXISTS idx_couriers_plate_number_lower
    ON couriers (lower(plate_number));

CREATE INDEX IF NOT EXISTS idx_users_full_name_lower
    ON users (lower(full_name));

CREATE INDEX IF NOT EXISTS idx_users_email_lower
    ON users (lower(email));

CREATE INDEX IF NOT EXISTS idx_users_phone_lower
    ON users (lower(phone));
