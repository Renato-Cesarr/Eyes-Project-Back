ALTER TABLE tb_audit_logs
    ADD COLUMN result CHARACTER VARYING(20) NOT NULL DEFAULT 'SUCCESS',
    ADD COLUMN correlation_id CHARACTER VARYING(64) NOT NULL DEFAULT 'legacy',
    ADD COLUMN metadata_json TEXT NOT NULL DEFAULT '{}';

-- The actor identifier is an immutable historical snapshot, not a live relation.
-- Removing the FK also lets failure events commit independently from a rolled-back
-- administrative transaction without waiting on locks held by that transaction.
ALTER TABLE tb_audit_logs
    DROP CONSTRAINT fk_audit_log_actor;

ALTER TABLE tb_audit_logs
    ADD CONSTRAINT ck_tb_audit_logs_result
        CHECK (result IN ('SUCCESS', 'FAILURE'));

CREATE INDEX ix_tb_audit_logs_action_occurred_at
    ON tb_audit_logs (action, occurred_at DESC);

CREATE INDEX ix_tb_audit_logs_result_occurred_at
    ON tb_audit_logs (result, occurred_at DESC);
