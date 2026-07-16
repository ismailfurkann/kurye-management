ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS user_branch_assignments (
    user_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_branch_assignments PRIMARY KEY (user_id, branch_id),
    CONSTRAINT fk_uba_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_uba_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE INDEX IF NOT EXISTS idx_uba_branch_id ON user_branch_assignments (branch_id);
