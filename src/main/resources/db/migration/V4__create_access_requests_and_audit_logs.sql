UPDATE tb_users
SET email = LOWER(BTRIM(email));

CREATE UNIQUE INDEX ux_tb_users_normalized_email
    ON tb_users (LOWER(BTRIM(email)));

CREATE TABLE tb_access_requests (
    id UUID PRIMARY KEY,
    name CHARACTER VARYING(150) NOT NULL,
    email CHARACTER VARYING(150) NOT NULL,
    request_reason CHARACTER VARYING(500),
    status CHARACTER VARYING(20) NOT NULL DEFAULT 'PENDING',
    decision_reason CHARACTER VARYING(500),
    decided_by_user_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    decided_at TIMESTAMP,
    CONSTRAINT ck_tb_access_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_tb_access_requests_normalized_email
        CHECK (email = LOWER(BTRIM(email))),
    CONSTRAINT ck_tb_access_requests_decision
        CHECK (
            (status = 'PENDING'
                AND decided_by_user_id IS NULL
                AND decided_at IS NULL
                AND decision_reason IS NULL)
            OR (status = 'APPROVED'
                AND decided_by_user_id IS NOT NULL
                AND decided_at IS NOT NULL
                AND decision_reason IS NULL)
            OR (status = 'REJECTED'
                AND decided_by_user_id IS NOT NULL
                AND decided_at IS NOT NULL
                AND decision_reason IS NOT NULL)
        ),
    CONSTRAINT fk_access_request_decider
        FOREIGN KEY (decided_by_user_id) REFERENCES tb_users (id)
);

CREATE UNIQUE INDEX ux_tb_access_requests_pending_email
    ON tb_access_requests (email)
    WHERE status = 'PENDING';

CREATE INDEX ix_tb_access_requests_status_created_at
    ON tb_access_requests (status, created_at DESC);

CREATE TABLE tb_audit_logs (
    id UUID PRIMARY KEY,
    action CHARACTER VARYING(80) NOT NULL,
    actor_user_id UUID NOT NULL,
    target_type CHARACTER VARYING(50) NOT NULL,
    target_id CHARACTER VARYING(100) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_log_actor
        FOREIGN KEY (actor_user_id) REFERENCES tb_users (id)
);

CREATE INDEX ix_tb_audit_logs_actor_occurred_at
    ON tb_audit_logs (actor_user_id, occurred_at DESC);

CREATE INDEX ix_tb_audit_logs_target
    ON tb_audit_logs (target_type, target_id);
