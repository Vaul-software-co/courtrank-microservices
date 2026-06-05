# CourtRank Legacy Sync Service

Small Node/TypeScript compatibility service for the migration period.

It keeps the legacy TypeScript backend database compatible while `auth-service`
and `user-service` become the source of truth for authentication and profile
data.

## Responsibilities

- Consume `auth.events` and `user.events` from Kafka.
- Maintain a minimal row in legacy `users` so legacy foreign keys keep working.
- Run one-off backfills between legacy and the new auth/user databases.

It does not own business logic for clubs, bookings, loyalty, competitions, or
social relationships.

## Commands

```bash
npm run dev
npm run backfill:legacy-from-services
npm run backfill:services-from-legacy
```

`backfill:legacy-from-services` copies `auth-service` + `user-service` users
into legacy `users`.

`backfill:services-from-legacy` copies legacy users into `auth-service`
`authentications` and `user-service` `users`.

Both backfills are idempotent upserts.
