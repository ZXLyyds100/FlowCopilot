-- FlowCopilot workflow phase-3/phase-4 incremental schema patch.
-- Use this file when an existing PostgreSQL database already has the
-- base chat/RAG tables but is missing workflow approval, trace,
-- observation or checkpoint tables.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS approval_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    workflow_instance_id UUID NOT NULL REFERENCES workflow_instance(id) ON DELETE CASCADE,
    status TEXT NOT NULL,
    title TEXT NOT NULL,
    summary TEXT,
    comment TEXT,
    decided_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_approval_record_status
    ON approval_record(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_approval_record_workflow
    ON approval_record(workflow_instance_id, created_at DESC);

CREATE TABLE IF NOT EXISTS execution_trace_ref (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    workflow_instance_id UUID NOT NULL REFERENCES workflow_instance(id) ON DELETE CASCADE,
    trace_id TEXT NOT NULL,
    graph_template TEXT NOT NULL,
    node_key TEXT NOT NULL,
    event_type TEXT NOT NULL,
    status TEXT NOT NULL,
    input_snapshot JSONB,
    output_snapshot JSONB,
    error_message TEXT,
    duration_ms BIGINT,

    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_execution_trace_ref_workflow
    ON execution_trace_ref(workflow_instance_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_execution_trace_ref_trace
    ON execution_trace_ref(trace_id, created_at ASC);

CREATE TABLE IF NOT EXISTS execution_observation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    run_id TEXT NOT NULL,
    trace_id TEXT NOT NULL,
    span_id TEXT NOT NULL,
    parent_span_id TEXT,
    workflow_instance_id UUID NOT NULL REFERENCES workflow_instance(id) ON DELETE CASCADE,
    node_key TEXT,
    span_type TEXT NOT NULL,
    name TEXT NOT NULL,
    status TEXT NOT NULL,
    input_summary TEXT,
    output_summary TEXT,
    error_message TEXT,
    attributes_json JSONB,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    duration_ms BIGINT,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_execution_observation_workflow
    ON execution_observation(workflow_instance_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_execution_observation_trace
    ON execution_observation(trace_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_execution_observation_run
    ON execution_observation(run_id, created_at ASC);

CREATE TABLE IF NOT EXISTS workflow_execution_checkpoint (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    workflow_instance_id UUID NOT NULL REFERENCES workflow_instance(id) ON DELETE CASCADE,
    trace_id TEXT NOT NULL,
    run_id TEXT NOT NULL,
    node_key TEXT NOT NULL,
    checkpoint_type TEXT NOT NULL,
    state_snapshot JSONB NOT NULL,
    metadata JSONB,

    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_execution_checkpoint_workflow
    ON workflow_execution_checkpoint(workflow_instance_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_workflow_execution_checkpoint_node
    ON workflow_execution_checkpoint(workflow_instance_id, node_key, checkpoint_type, created_at DESC);
