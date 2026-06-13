# Audit Service

Audit microservice for CourtRank. It stores operational audit events emitted by the other services and exposes a restricted read API for admin troubleshooting panels.

## Current Scope

- Consume audit events from Kafka topic `audit.events`.
- Persist audit events in PostgreSQL.
- Keep event payload metadata as JSON for flexible troubleshooting data.
- Expose read-only audit search endpoints.
- Allow filtering by source, type, user, actor, target, trace id, and time range.
- Restrict the HTTP API to JWT access tokens with role `SUPER_ADMIN`.

This service does not own auth, user profile, social, booking, billing, or notification data. It only stores an immutable audit trail useful for support, diagnostics, moderation review, and incident investigation.

## Local Requirements

- Java 17 or newer. The Maven build currently compiles with Java release 17.
- Docker and Docker Compose.
- PostgreSQL if running without Docker Compose.
- Kafka if running outside Docker Compose.

## Environment

From the repository root, create the service and database secret files:

```bash
cp secrets/audit-service.env.example secrets/audit-service.env
cp secrets/audit-postgres.env.example secrets/audit-postgres.env
```

Make sure the auth public key exists in the central secrets directory. It should come from the same key pair used by `auth-service`; do not store runtime PEM files under service `src/main/resources`.

```bash
openssl rsa -pubout -in secrets/jwt-private.pem -out secrets/jwt-public.pem
```

Important variables:

```env
SPRING_PROFILES_ACTIVE=local,kafka
SERVER_PORT=8086

DATABASE_URL=jdbc:postgresql://localhost:5432/courtrank_audit
DATABASE_USERNAME=courtrank_audit_user
DATABASE_PASSWORD=change-me

JWT_PUBLIC_KEY=file:../secrets/jwt-public.pem
CORS_ALLOWED_ORIGINS=http://localhost:3000

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_SECURITY_PROTOCOL=PLAINTEXT
KAFKA_AUDIT_EVENTS_TOPIC=audit.events
KAFKA_CONSUMER_GROUP_ID=audit-service
```

When Kafka runs with SSL, also set:

```env
KAFKA_SSL_TRUSTSTORE_LOCATION=/run/secrets/kafka.truststore.p12
KAFKA_SSL_TRUSTSTORE_PASSWORD=change-me
KAFKA_SSL_TRUSTSTORE_TYPE=PKCS12
KAFKA_SSL_KEYSTORE_LOCATION=/run/secrets/kafka-audit-service.keystore.p12
KAFKA_SSL_KEYSTORE_PASSWORD=change-me
KAFKA_SSL_KEYSTORE_TYPE=PKCS12
KAFKA_SSL_KEY_PASSWORD=change-me
```

Do not commit real `.env` files, PEM keys, API keys, Kafka keystores, or database credentials. Runtime secrets live under `secrets/`, which is ignored by git.

## Profiles

- `local`: local runtime defaults.
- `kafka`: enables Kafka consumer configuration.
- `prod`: intended for production deployment.
- `test`: used by tests.

## Run With Docker Compose

From the repository root:

```bash
docker compose --env-file secrets/compose.env -f docker-compose.auth.yml up -d --build
```

This starts:

- `audit-service` on `http://localhost:8086`
- `audit-postgres` on host port `5436`
- `auth-service` on `http://localhost:8081`
- `user-service` on `http://localhost:8082`
- `social-service` on `http://localhost:8084`
- `api-gateway` on `http://localhost:8080`
- `kafka` on host port `9092`
- `kafka-ui` on `http://localhost:8085`

Check status:

```bash
docker compose --env-file secrets/compose.env -f docker-compose.auth.yml ps
```

Follow logs:

```bash
docker compose --env-file secrets/compose.env -f docker-compose.auth.yml logs -f audit-service
```

Health check:

```bash
curl http://localhost:8086/actuator/health
```

## Run Without Docker

From `audit-service`:

```bash
set -a
source ../secrets/audit-service.env
set +a
./mvnw spring-boot:run
```

The service listens on `SERVER_PORT`, defaulting to `8086`.

## Security

Audit HTTP routes require either:

```http
Authorization: Bearer <access-token>
```

or the web `token` cookie.

The token must be a valid access JWT issued by `auth-service`, and the user role must be `SUPER_ADMIN`. Non-admin users receive `403`; missing or invalid tokens receive `401`.

## Kafka Input Contract

`audit-service` consumes messages from `audit.events`.

Expected message shape:

```json
{
  "eventId": "00000000-0000-0000-0000-000000000000",
  "source": "auth-service",
  "payload": {
    "type": "USER_SIGNED_IN",
    "actorId": "00000000-0000-0000-0000-000000000000",
    "targetId": "00000000-0000-0000-0000-000000000000",
    "traceId": "request-id-or-trace-id",
    "metadata": {
      "client": "web",
      "ip": "127.0.0.1"
    },
    "occurredAt": "2026-06-13T18:00:00Z"
  },
  "publishedAt": "2026-06-13T18:00:01Z"
}
```

Required fields:

- `eventId`
- `source`
- `payload.type`
- `payload.occurredAt`
- `publishedAt`

Optional fields:

- `payload.actorId`
- `payload.targetId`
- `payload.traceId`
- `payload.metadata`

Invalid messages are logged and ignored. The consumer does not currently send invalid records to a dead-letter topic.

## Public API

All routes below require `SUPER_ADMIN`.

### GET /audit/events

Searches audit events.

Query params:

- `source`: exact service/source name.
- `type`: exact audit event type.
- `userId`: matches either `actorId` or `targetId`.
- `actorId`: exact actor user id.
- `targetId`: exact target user id.
- `traceId`: exact trace/request id.
- `occurredFrom`: ISO-8601 lower bound for `occurredAt`.
- `occurredTo`: ISO-8601 upper bound for `occurredAt`.
- `limit`: defaults to `50`, max `200`.
- `offset`: defaults to `0`.

Example:

```bash
curl "http://localhost:8086/audit/events?userId=00000000-0000-0000-0000-000000000000&limit=20" \
  -H "Authorization: Bearer $SUPER_ADMIN_ACCESS_TOKEN"
```

Response `200`:

```json
{
  "events": [
    {
      "eventId": "00000000-0000-0000-0000-000000000000",
      "source": "auth-service",
      "type": "USER_SIGNED_IN",
      "actorId": "00000000-0000-0000-0000-000000000000",
      "targetId": "00000000-0000-0000-0000-000000000000",
      "traceId": "request-id-or-trace-id",
      "metadata": {
        "client": "web",
        "ip": "127.0.0.1"
      },
      "occurredAt": "2026-06-13T18:00:00Z",
      "publishedAt": "2026-06-13T18:00:01Z",
      "ingestedAt": "2026-06-13T18:00:02Z"
    }
  ],
  "limit": 20,
  "offset": 0
}
```

### GET /audit/events/{eventId}

Returns one audit event by id.

Example:

```bash
curl "http://localhost:8086/audit/events/00000000-0000-0000-0000-000000000000" \
  -H "Authorization: Bearer $SUPER_ADMIN_ACCESS_TOKEN"
```

Response `200`: same event shape as search items.

If the event does not exist, the service returns `404`.

## Gateway Routes

The API gateway forwards:

- `/audit/` to `audit-service`
- `/audit` to `audit-service`

In local Docker Compose, the service is also directly exposed on `http://localhost:8086` for development.

## Database

Flyway owns the audit schema migration:

```txt
src/main/resources/db/migration/V1__create_audit_schema.sql
```

The main table is `audit_events`. It includes indexes for:

- `event_id`
- `source`
- `type`
- `actor_id`
- `target_id`
- `trace_id`
- `occurred_at`
- `ingested_at`

The `metadata` column is PostgreSQL `jsonb`.

## Tests

Run all tests:

```bash
./mvnw test
```

The current suite covers:

- domain validation
- event ingestion use case
- single event lookup use case
- search filter normalization and repository delegation

## Production Notes

- Only expose the gateway publicly. Do not expose `audit-service` port `8086` directly.
- Keep Kafka and Postgres accessible only from trusted private hosts/security groups.
- Configure Kafka SSL using the audit-service client keystore.
- Keep the `audit.events` ACL limited to read/describe for `audit-service`.
- Use a SUPER_ADMIN JWT for the dashboard/API client that reads audit data.
- Keep Docker image builds outside production VPS instances; deploy already-built images.
