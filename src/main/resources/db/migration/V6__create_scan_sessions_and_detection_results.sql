CREATE TABLE tb_scan_sessions (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES tb_users(id) ON DELETE CASCADE,
    client_session_id UUID NOT NULL,
    installation_id UUID NOT NULL,
    schema_version INTEGER NOT NULL DEFAULT 1 CHECK (schema_version = 1),
    model_id VARCHAR(80) NOT NULL,
    model_version VARCHAR(80) NOT NULL,
    consent_version VARCHAR(40) NOT NULL CHECK (consent_version = 'metadata-sync-v1'),
    consent_received_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    processed_frames INTEGER,
    inference_millis_total BIGINT,
    tts_latency_samples INTEGER,
    tts_latency_millis_total BIGINT,
    UNIQUE (owner_id, client_session_id),
    CHECK (expires_at = started_at + INTERVAL '720 hours'),
    CHECK (ended_at IS NULL OR (ended_at >= started_at AND ended_at <= started_at + INTERVAL '2 hours')),
    CHECK ((ended_at IS NULL AND processed_frames IS NULL AND inference_millis_total IS NULL
        AND tts_latency_samples IS NULL AND tts_latency_millis_total IS NULL)
        OR (ended_at IS NOT NULL AND processed_frames IS NOT NULL AND inference_millis_total IS NOT NULL
        AND tts_latency_samples IS NOT NULL AND tts_latency_millis_total IS NOT NULL AND processed_frames BETWEEN 0 AND 1000000
        AND inference_millis_total BETWEEN 0 AND 1000000000
        AND tts_latency_samples BETWEEN 0 AND 200 AND tts_latency_millis_total BETWEEN 0 AND 1000000000))
);
CREATE INDEX ix_scan_sessions_owner_started ON tb_scan_sessions(owner_id, started_at DESC);
CREATE INDEX ix_scan_sessions_expires ON tb_scan_sessions(expires_at);

CREATE TABLE tb_detection_results (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES tb_scan_sessions(id) ON DELETE CASCADE,
    client_event_id UUID NOT NULL,
    object_class VARCHAR(20) NOT NULL CHECK (object_class IN ('PERSON', 'CHAIR', 'TABLE_DESK', 'BACKPACK')),
    confidence NUMERIC(7,6) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    proximity_band VARCHAR(20) NOT NULL CHECK (proximity_band IN ('DISTANT', 'ATTENTION', 'VERY_NEAR')),
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('LEFT', 'AHEAD', 'RIGHT')),
    occurred_at TIMESTAMPTZ NOT NULL,
    UNIQUE (session_id, client_event_id)
);
CREATE INDEX ix_detection_results_session_occurred ON tb_detection_results(session_id, occurred_at);
