create table audit_events (
    event_id uuid primary key,
    source varchar(64) not null,
    type varchar(128) not null,
    actor_id uuid,
    target_id uuid,
    trace_id varchar(128),
    metadata jsonb not null default '{}'::jsonb,
    occurred_at timestamptz not null,
    published_at timestamptz not null,
    ingested_at timestamptz not null
);

create index idx_audit_events_occurred_at on audit_events (occurred_at desc);
create index idx_audit_events_source_type on audit_events (source, type);
create index idx_audit_events_actor_id on audit_events (actor_id);
create index idx_audit_events_target_id on audit_events (target_id);
create index idx_audit_events_trace_id on audit_events (trace_id);
