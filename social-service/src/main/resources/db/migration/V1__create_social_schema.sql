create table social_users (
    user_id uuid primary key,
    name varchar(100) not null,
    username varchar(30),
    avatar_url text,
    private_profile boolean not null,
    active boolean not null,
    deleted_at timestamp with time zone,
    source_updated_at timestamp with time zone,
    synced_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_social_users_search on social_users (name, username);
create index idx_social_users_visible on social_users (active, deleted_at);

create table follows (
    id uuid primary key,
    follower_id uuid not null,
    following_id uuid not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    accepted_at timestamp with time zone,
    updated_at timestamp with time zone not null,
    constraint uq_follows_pair unique (follower_id, following_id),
    constraint chk_follows_different_users check (follower_id <> following_id)
);

create index idx_follows_follower_id on follows (follower_id);
create index idx_follows_following_id on follows (following_id);
create index idx_follows_following_status on follows (following_id, status);

create table blocks (
    id uuid primary key,
    blocker_id uuid not null,
    blocked_id uuid not null,
    created_at timestamp with time zone not null,
    constraint uq_blocks_pair unique (blocker_id, blocked_id),
    constraint chk_blocks_different_users check (blocker_id <> blocked_id)
);

create index idx_blocks_blocker_id on blocks (blocker_id);
create index idx_blocks_blocked_id on blocks (blocked_id);

create table social_counters (
    user_id uuid primary key,
    followers_count integer not null default 0,
    following_count integer not null default 0,
    pending_requests_count integer not null default 0,
    blocked_count integer not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint chk_social_counters_non_negative check (
        followers_count >= 0
        and following_count >= 0
        and pending_requests_count >= 0
        and blocked_count >= 0
    )
);
