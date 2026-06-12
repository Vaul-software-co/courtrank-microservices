# CourtRank Legacy Sync Service

Small Node/TypeScript compatibility service for the migration period.

It keeps the legacy TypeScript backend database compatible while `auth-service`,
`user-service`, and `social-service` become the source of truth for
authentication, profile, and social data.

## Responsibilities

- Consume `auth.events` and `user.events` from Kafka.
- Maintain a minimal row in legacy `users` so legacy foreign keys keep working.
- Run one-off backfills between legacy and the new auth/user/social databases.

It does not own business logic for clubs, bookings, loyalty, or competitions.

## Commands

```bash
npm run dev
npm run backfill:legacy-from-services
npm run backfill:services-from-legacy
```

`backfill:legacy-from-services` copies `auth-service` + `user-service` users
into legacy `users`, and `social-service` follows/blocks into legacy social
tables.

`backfill:services-from-legacy` copies legacy users into `auth-service`
`authentications`, `user-service` `users`, and `social-service` social tables.

Both backfills are idempotent upserts.
