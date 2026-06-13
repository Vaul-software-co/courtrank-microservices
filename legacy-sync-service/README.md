# CourtRank Legacy Sync Service

Small Node/TypeScript compatibility service for the migration period.

It keeps the legacy TypeScript backend database compatible while `auth-service`, `user-service`, and `social-service` become the source of truth for authentication, profile, and social data.

## Current Scope

- Consume `auth.events` and `user.events` from Kafka.
- Maintain minimal legacy `users` rows so legacy foreign keys keep working.
- Run one-off backfills between legacy and new service databases.
- Copy user auth/profile data from services to legacy during migration.
- Copy legacy users into service databases when bootstrapping the new services.
- Copy follows and blocks between legacy and `social-service`.

This service does not own business logic for clubs, bookings, loyalty, competitions, checkout, courts, or notifications. Those domains still belong to the legacy backend until explicitly migrated.

## Why This Exists

During migration, some flows write to the new microservices while old features still read or join against legacy tables.

`legacy-sync-service` is the bridge that prevents legacy foreign keys and legacy read paths from breaking while domains move gradually.

It is temporary. Once the legacy backend no longer needs user/social compatibility tables, this service should be removed.

## Data Direction

There are two supported sync directions:

1. `services -> legacy`
   - New services are the source of truth.
   - Legacy DB receives compatibility rows.
   - Used after auth/user/social are live.

2. `legacy -> services`
   - Legacy DB is the source for a one-time bootstrap/backfill.
   - New service DBs receive users/social data.
   - Used before or during cutover.

Do not run both backfill directions blindly in production. Choose the direction that matches the migration step.

## Runtime Consumer

At runtime the service consumes:

- `auth.events`
- `user.events`

Expected runtime behavior:

- `USER_REGISTERED` / restored auth events keep legacy user identity rows available.
- `USER_DELETED` marks or updates legacy compatibility data as needed.
- user profile events keep legacy profile-compatible fields fresh.

Runtime consumption should be idempotent. Reprocessing the same event should not create duplicate users or duplicate social relationships.

## Backfill Commands

```bash
npm run backfill:legacy-from-services
npm run backfill:services-from-legacy
```

`backfill:legacy-from-services` copies:

- `auth-service` authentications
- `user-service` user profiles
- `social-service` follows/blocks
- into the legacy `users` and legacy social tables

`backfill:services-from-legacy` copies:

- legacy users into `auth-service` `authentications`
- legacy users into `user-service` `users`
- legacy follows/blocks into `social-service`

Both backfills should be idempotent upserts.

## Recommended Migration Order

For an initial production cutover:

1. Take a backup of the legacy database.
2. Start `auth-service`, `user-service`, `social-service`, their databases, Kafka, and `legacy-sync-service`.
3. Run `npm run backfill:services-from-legacy` once to seed new service databases.
4. Validate row counts and sample users in each service DB.
5. Turn on new auth/profile/social writes through the gateway.
6. Keep `legacy-sync-service` running to keep legacy compatibility rows updated.
7. Run `npm run backfill:legacy-from-services` only if legacy needs to be repaired or refreshed from service state.

For a rollback:

1. Stop routing new traffic to microservice routes.
2. Keep the legacy backend pointed at the legacy DB.
3. If needed, run `backfill:legacy-from-services` to copy recently migrated users/social relationships back.
4. Validate the legacy app before disabling the microservices.

## Environment

Create:

```bash
cp secrets/legacy-sync-service.env.example secrets/legacy-sync-service.env
```

Important variables:

```env
LEGACY_DATABASE_URL=postgresql://legacy_user:change-me@host.docker.internal:5432/courtrank?schema=public
AUTH_DATABASE_URL=postgresql://courtrank_auth_user:change-me@auth-postgres:5432/courtrank_auth?schema=public
USER_DATABASE_URL=postgresql://courtrank_user_user:change-me@user-postgres:5432/courtrank_users?schema=public
SOCIAL_DATABASE_URL=postgresql://courtrank_social_user:change-me@social-postgres:5432/courtrank_social?schema=public

LEGACY_PLACEHOLDER_PASSWORD_HASH=managed-by-auth-service
BACKFILL_BATCH_SIZE=500

KAFKA_BROKERS=kafka:29092
KAFKA_CLIENT_ID=courtrank-legacy-sync
KAFKA_GROUP_ID=courtrank-legacy-sync
AUTH_EVENTS_TOPIC=auth.events
USER_EVENTS_TOPIC=user.events
```

For Kafka TLS:

```env
KAFKA_SSL_ENABLED=true
KAFKA_SSL_CA_PATH=/run/secrets/kafka-ca.crt
KAFKA_SSL_CERT_PATH=/run/secrets/kafka-legacy-sync-service.crt
KAFKA_SSL_KEY_PATH=/run/secrets/kafka-legacy-sync-service.key
```

In production, `LEGACY_DATABASE_URL` should point to the legacy database host. Do not assume `host.docker.internal` unless the legacy database is on the same VPS.

## Run Locally

Install dependencies:

```bash
cd legacy-sync-service
npm install
```

Run the consumer:

```bash
npm run dev
```

Run a backfill:

```bash
npm run backfill:services-from-legacy
```

or:

```bash
npm run backfill:legacy-from-services
```

## Run With Docker Compose

From the repository root:

```bash
docker compose --env-file secrets/compose.env -f docker-compose.auth.yml up -d --build legacy-sync-service
```

Follow logs:

```bash
docker compose --env-file secrets/compose.env -f docker-compose.auth.yml logs -f legacy-sync-service
```

## Validation Checklist

Before backfill:

- Confirm the source database is backed up.
- Confirm source and destination URLs point to the intended environments.
- Confirm Kafka is not required for one-off backfill unless the command explicitly emits events.
- Confirm `BACKFILL_BATCH_SIZE` is reasonable for the VPS.

After `legacy -> services`:

- Sample users exist in `auth-service` DB.
- Sample profiles exist in `user-service` DB.
- Sample follows/blocks exist in `social-service` DB.
- Sign in and profile read paths work through the gateway.

After `services -> legacy`:

- Legacy `users` rows exist for newly migrated users.
- Legacy foreign-key-dependent features still load.
- No duplicate user/social rows were created.

## Known Boundaries

- This service is intentionally coupled to multiple databases because it is a migration bridge.
- New business logic should not be added here.
- Long-term cross-service synchronization should use service events and service-owned APIs, not direct DB writes.
- If a backfill fails halfway, rerun the same direction after fixing the cause; commands are expected to be idempotent.
