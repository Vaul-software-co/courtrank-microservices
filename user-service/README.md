# User Service

User profile microservice for CourtRank. This service is currently the active migration target from the TypeScript backend into Java microservices.

## Current Scope

- Own user profile data created from auth lifecycle events.
- Consume `auth.events` from Kafka.
- Create profiles from `USER_REGISTERED`.
- Restore or create profiles from `USER_RESTORED`.
- Mark profiles as deleted from `USER_DELETED`.
- Publish `USER_PROFILE_CREATED` to `user.events`.
- Publish audit events to `audit.events`.
- Expose an internal username availability endpoint for `auth-service`.
- Expose authenticated profile endpoints for the TypeScript BFF/mobile clients.
- Expose internal summary/active-check endpoints for other services.
- Expose internal moderation endpoints for BFF/admin flows.

This service does not own credentials, password reset, refresh sessions, JWTs, or email verification. Those belong in `auth-service`.

## Local Requirements

- Java 17 or newer. The Maven build currently compiles with Java release 17.
- Docker and Docker Compose.
- PostgreSQL if running without Docker Compose.
- Kafka if running with the `kafka` profile outside Docker Compose.

## Environment

From the repository root, create the service and database secret files:

```bash
cp secrets/user-service.env.example secrets/user-service.env
cp secrets/user-postgres.env.example secrets/user-postgres.env
```

Make sure the auth public key exists in the central secrets directory. It should come from the same key pair used by `auth-service`; do not store runtime PEM files under service `src/main/resources`.

```bash
openssl rsa -pubout -in secrets/jwt-private.pem -out secrets/jwt-public.pem
```

Important variables:

```env
SPRING_PROFILES_ACTIVE=local,kafka
SERVER_PORT=8082

DATABASE_URL=jdbc:postgresql://localhost:5432/courtrank_users
DATABASE_USERNAME=courtrank_user_user
DATABASE_PASSWORD=change-me

INTERNAL_API_KEY=change-me
JWT_PUBLIC_KEY=file:../secrets/jwt-public.pem
AUTH_SERVICE_URL=http://localhost:8080/internal
AUTH_SERVICE_API_KEY=change-me
CORS_ALLOWED_ORIGINS=http://localhost:3000
SEARCH_RATE_LIMIT_MAX_REQUESTS=30
SEARCH_RATE_LIMIT_WINDOW_SECONDS=60

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_AUTH_EVENTS_TOPIC=auth.events
KAFKA_USER_EVENTS_TOPIC=user.events
KAFKA_AUDIT_EVENTS_TOPIC=audit.events
KAFKA_CONSUMER_GROUP_ID=user-service
```

Do not commit real `.env` files, PEM keys, API keys, or database credentials. Runtime secrets live under `secrets/`, which is ignored by git.

## Profiles

- `local`: uses console adapters for audit/events when no real bean is available.
- `kafka`: consumes auth events and publishes user/audit events through Kafka.
- `production`: intended for production Kafka adapters.
- `test`: used by tests.

## Run With Docker Compose

From the repository root:

```bash
docker compose -f docker-compose.auth.yml up -d --build
```

This starts:

- `user-service` on `http://localhost:8082`
- `user-postgres` on host port `5434`
- `auth-service` on `http://localhost:8081`
- `auth-postgres` on host port `5433`
- `kafka` on host port `9092`
- `kafka-ui` on `http://localhost:8085`

Check status:

```bash
docker compose -f docker-compose.auth.yml ps
```

Follow logs:

```bash
docker compose -f docker-compose.auth.yml logs -f user-service
```

## Run Without Docker

From `user-service`:

```bash
set -a
source ../secrets/user-service.env
set +a
./mvnw spring-boot:run
```

The service listens on `SERVER_PORT`, defaulting to `8082`.

## Internal API

Internal routes require:

```http
x-internal-api-key: <internal-api-key>
```

Public user routes require either:

```http
Authorization: Bearer <access-token>
```

or the web `token` cookie. Access tokens are issued by `auth-service` and verified here with the shared JWT public key.

## Public API

### GET /users/me

Returns the authenticated user's profile.

Response `200`:

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "name": "Sebastian Sanchez",
  "username": "sebas",
  "email": "test@example.com",
  "phoneNumber": "+573001112233",
  "gender": "MALE",
  "avatarUrl": "users/00000000-0000-0000-0000-000000000000/avatar.webp",
  "privateProfile": false,
  "status": "VISIBLE",
  "lang": "es",
  "createdAt": "2026-06-02T20:00:00Z"
}
```

### PATCH /users/me

Patch profile fields. Only non-null fields are updated.

Request:

```json
{
  "name": "Sebastian Sanchez",
  "username": "sebas",
  "phoneNumber": "+573001112233",
  "gender": "MALE"
}
```

Response `200`: same shape as `GET /users/me`.

Username changes are limited by the domain rule: 2 changes per 180 days.

### PATCH /users/me/privacy

Request:

```json
{
  "privateProfile": true
}
```

Response `200`:

```json
{
  "privateProfile": true
}
```

If the value is unchanged, the use case returns without saving or auditing an update.

### PATCH /users/me/lang

Request:

```json
{
  "lang": "es"
}
```

Allowed values:

```txt
es
en
```

Response `200`:

```json
{
  "lang": "es"
}
```

### PUT /users/me/avatar

Stores the current avatar reference. This currently uses `avatarKey` as the value stored in the existing avatar field. A dedicated storage provider should later generate read URLs from stable object keys.

Request:

```json
{
  "avatarKey": "users/00000000-0000-0000-0000-000000000000/avatar.webp"
}
```

Response `200`:

```json
{
  "avatarKey": "users/00000000-0000-0000-0000-000000000000/avatar.webp",
  "avatarUrl": "users/00000000-0000-0000-0000-000000000000/avatar.webp"
}
```

### DELETE /users/me/avatar

Removes the avatar reference from the profile.

Response `200`:

```json
{
  "avatarKey": null,
  "avatarUrl": null
}
```

### GET /users/search

Searches visible public users by name or username.

Query parameters:

```txt
q=sebas
limit=20
```

Response `200`:

```json
[
  {
    "id": "00000000-0000-0000-0000-000000000000",
    "name": "Sebastian Sanchez",
    "username": "sebas",
    "avatarUrl": null
  }
]
```

Social filtering such as blocked users should be composed by the BFF/social-service later.

### GET /users/{id}

Returns the base public profile. Social fields such as follower counts, following counts, blocks, and viewer follow status belong to `social-service`.

Response `200`:

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "name": "Sebastian Sanchez",
  "username": "sebas",
  "avatarUrl": null,
  "privateProfile": false,
  "status": "VISIBLE",
  "createdAt": "2026-06-02T20:00:00Z"
}
```

### GET /internal/users/username-available

Checks username availability for internal service calls.

Query parameters:

```txt
username=sebas
userId=00000000-0000-0000-0000-000000000000
```

Response `200`:

```json
{
  "available": true
}
```

### GET /internal/users/{id}/summary

Returns an internal profile summary for other services.

Response `200`:

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "name": "Sebastian Sanchez",
  "username": "sebas",
  "email": "test@example.com",
  "avatarUrl": null,
  "privateProfile": false,
  "status": "VISIBLE"
}
```

### POST /internal/users/summaries

Batch summary lookup.

Request:

```json
{
  "userIds": [
    "00000000-0000-0000-0000-000000000000"
  ]
}
```

Response `200`:

```json
[
  {
    "id": "00000000-0000-0000-0000-000000000000",
    "name": "Sebastian Sanchez",
    "username": "sebas",
    "email": "test@example.com",
    "avatarUrl": null,
    "privateProfile": false,
    "status": "VISIBLE"
  }
]
```

### GET /internal/users/{id}/active

Checks whether a user can operate in other services.

Response `200`:

```json
{
  "active": true,
  "status": "VISIBLE"
}
```

### POST /internal/users/{id}/ban

Suspends a user profile. Intended for a BFF/admin route after the caller has already enforced admin permissions.

Request:

```json
{
  "adminUserId": "00000000-0000-0000-0000-000000000000"
}
```

Response `200`:

```json
{
  "status": "SUSPENDED"
}
```

### POST /internal/users/{id}/unban

Restores a suspended user profile to visible.

Request:

```json
{
  "adminUserId": "00000000-0000-0000-0000-000000000000"
}
```

Response `200`:

```json
{
  "status": "VISIBLE"
}
```

## TypeScript Backend Migration Notes

The old TypeScript BFF still owns several `/user` routes. The intended split is:

| Capability | Target owner |
| --- | --- |
| `GET /user/me` base profile fields | `user-service` via `/users/me` |
| `PATCH /user/me` name, username, phone, gender | `user-service` via `/users/me` |
| `PATCH /user/me/privacy` | `user-service` via `/users/me/privacy` |
| `POST /user/me/lang` | `user-service` via `/users/me/lang` |
| public user search base fields | `user-service` via `/users/search`; social enrichment in `social-service` |
| public user profile base fields | `user-service` via `/users/{id}`; social enrichment in `social-service` |
| followers, following, follow requests, blocks, viewer follow status | `social-service` |
| `clubsCount`, club memberships | club/member service |
| `matchesPlayed`, tournament participation | competition/match service |
| `loyaltyPoints` | loyalty service |
| password changes, account deletion auth lifecycle | `auth-service` |
| notification preferences | notification/user-preferences service, or keep in BFF until that exists |
| terms/data-commercialization consent | legal/compliance service, or keep in BFF until that exists |
| admin ban/unban | BFF/admin guard calling `/internal/users/{id}/ban` and `/internal/users/{id}/unban` |

Nginx/BFF routing can keep the mobile contract stable while forwarding base user profile requests to `user-service` and social-rich requests to `social-service`.

## Kafka

Default topics:

```txt
auth.events
user.events
audit.events
```

`user-service` consumes auth lifecycle events from `auth.events`:

- `USER_REGISTERED`
- `USER_RESTORED`
- `USER_DELETED`

It currently publishes:

- `USER_PROFILE_CREATED`

## Database

Flyway runs automatically at startup.

Current core tables:

- `users`
- `flyway_schema_history`

For Docker Compose, the app connects to:

```txt
jdbc:postgresql://user-postgres:5432/courtrank_users
```

## Tests

Run all tests:

```bash
./mvnw test
```

The current test suite covers:

- application startup
- authenticated user controller routes
- internal user controller routes and API-key protection
- request validation for body/query parameters
- JWT authentication filter behavior
- RSA access-token verification and session-id extraction
- internal API-key filter behavior
- auth event consumer routing
- Kafka user event publishing
- Kafka audit event publishing
- profile update patch behavior
- username conflict handling
- privacy no-op behavior
- language update auditing
- public profile not-found behavior for suspended users
- search result mapping
- internal batch summaries
- active checks
- restore from auth event
- avatar update/remove behavior
- ban profile behavior
