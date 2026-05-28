create table users (
    id uuid primary key,
    name varchar(100) not null,
    username varchar(30),
    email varchar(320) not null,
    gender varchar(32),
    phone_number varchar(20),
    avatar_url text,
    username_changed_at timestamptz,
    username_prev_changed_at timestamptz,
    lang varchar(10),
    private_profile boolean not null default false,
    status varchar(32) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index users_username_lower_unique
    on users (lower(username))
    where username is not null;

create unique index users_email_lower_unique
    on users (lower(email));

create index users_status_idx on users (status);
