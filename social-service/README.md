# Social Service

Social microservice for CourtRank. It owns follows, follow requests, blocks, social counters, and the public social profile projection used by search and profile views.

## Current Scope

- Own follow relationships and follow request lifecycle.
- Own user blocks and block-aware social reads.
- Own social counters for followers and following.
- Keep a local `SocialUser` projection from `user-service` profile events.
- Consume `user.events` from Kafka.
- Publish social relationship events to `social.events`.
- Publish audit events to `audit.events`.
- Expose authenticated endpoints for social profile, search, follows, blocks, followers, and following.
- Expose internal endpoints for block checks, counter rebuilds, and social-user reconciliation.

This service does not own credentials, email, phone, username changes, avatar changes, language, or profile privacy writes. Those belong in `auth-service` and `user-service`. `SocialUser` is a read model for the public social profile and is updated from `user-service` events.

## Local Requirements

- Java 17 or newer. The Maven build currently compiles with Java release 17.
- Docker and Docker Compose.
- PostgreSQL if running without Docker Compose.
- Kafka if running with the `kafka` profile outside Docker Compose.

## Environment

From the repository root, create the service and database secret files:

```bash
cp secrets/social-service.env.example secrets/social-service.env
cp secrets/social-postgres.env.example secrets/social-postgres.env
```

Make sure the auth public key exists in the central secrets directory. It should come from the same key pair used by `auth-service`; do not store runtime PEM files under service `src/main/resources`.

```bash
openssl rsa -pubout -in secrets/jwt-private.pem -out secrets/jwt-public.pem
```

Important variables:

```env
SPRING_PROFILES_ACTIVE=local,kafka
SERVER_PORT=8084

DATABASE_URL=jdbc:postgresql://localhost:5432/courtrank_social
DATABASE_USERNAME=courtrank_social_user
DATABASE_PASSWORD=change-me

INTERNAL_API_KEY=change-me
JWT_PUBLIC_KEY=file:../secrets/jwt-public.pem
AUTH_SERVICE_URL=http://localhost:8080/internal
AUTH_SERVICE_API_KEY=change-me
USER_SERVICE_URL=http://localhost:8082
USER_SERVICE_API_KEY=change-me
CORS_ALLOWED_ORIGINS=http://localhost:3000

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP_ID=social-service
KAFKA_USER_EVENTS_TOPIC=user.events
KAFKA_SOCIAL_EVENTS_TOPIC=social.events
KAFKA_AUDIT_EVENTS_TOPIC=audit.events
```

Do not commit real `.env` files, PEM keys, API keys, or database credentials. Runtime secrets live under `secrets/`, which is ignored by git.

## Profiles

- `local`: uses console adapters for audit/events when no real bean is available.
- `kafka`: consumes user events and publishes social/audit events through Kafka.
- `production`: intended for production Kafka adapters.
- `test`: used by tests.

## Run With Docker Compose

From the repository root:

```bash
docker compose -f docker-compose.auth.yml up -d --build
```

This starts:

- `social-service` on `http://localhost:8084`
- `social-postgres` on host port `5435`
- `user-service` on `http://localhost:8082`
- `auth-service` on `http://localhost:8081`
- `kafka` on host port `9092`
- `kafka-ui` on `http://localhost:8085`

Check status:

```bash
docker compose -f docker-compose.auth.yml ps
```

Follow logs:

```bash
docker compose -f docker-compose.auth.yml logs -f social-service
```

## Run Without Docker

From `social-service`:

```bash
set -a
source ../secrets/social-service.env
set +a
./mvnw spring-boot:run
```

The service listens on `SERVER_PORT`, defaulting to `8084`.

## Security

Public social routes require either:

```http
Authorization: Bearer <access-token>
```

or the web `token` cookie. Access tokens are issued by `auth-service` and verified here with the shared JWT public key.

Internal routes require:

```http
x-internal-api-key: <internal-api-key>
```

## Public API

### POST /social/follows/{targetId}

Starts following a user. If the target profile is public, the follow is accepted immediately. If the target profile is private, a pending follow request is created.

Response `200`:

```json
{
  "followId": "00000000-0000-0000-0000-000000000000",
  "status": "ACCEPTED"
}
```

### POST /social/follow-requests/{followId}/accept

Accepts a pending follow request received by the authenticated user.

Response `200` with an empty body.

### POST /social/follow-requests/{followId}/reject

Rejects a pending follow request received by the authenticated user.

Response `200` with an empty body.

### DELETE /social/follows/{targetId}

Unfollows a user or cancels a pending follow request.

Response `200` with an empty body.

### DELETE /social/followers/{followerId}

Removes one of the authenticated user's followers.

Response `200` with an empty body.

### POST /social/blocks/{targetId}

Blocks a user. Blocking removes existing follow relationships between both users.

Response `200`:

```json
{
  "blockId": "00000000-0000-0000-0000-000000000000",
  "created": true
}
```

### DELETE /social/blocks/{targetId}

Unblocks a user.

Response `200` with an empty body.

### GET /social/users/search

Searches visible social users by name or username. Blocked users and users that blocked the viewer are excluded.

Query params:

- `q`: search text, 2 to 100 chars.
- `limit`: result limit, defaults to `20`, max `50`.

Response `200`:

```json
[
  {
    "userId": "00000000-0000-0000-0000-000000000000",
    "name": "Sebastian Sanchez",
    "username": "sebas",
    "avatarUrl": "users/00000000-0000-0000-0000-000000000000/avatar.webp",
    "relatedAt": null
  }
]
```

### GET /social/users/{userId}

Returns the social profile summary visible to the authenticated viewer.

Response `200`:

```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "name": "Sebastian Sanchez",
  "username": "sebas",
  "avatarUrl": "users/00000000-0000-0000-0000-000000000000/avatar.webp",
  "privateProfile": false,
  "viewerFollowStatus": "ACCEPTED",
  "blocked": false,
  "followersCount": 10,
  "followingCount": 4,
  "pendingRequestsCount": 0,
  "blockedCount": 0,
  "createdAt": "2026-06-12T12:00:00Z"
}
```

### GET /social/users/{userId}/followers

Returns accepted followers visible to the authenticated viewer.

Response `200`: list of social user summaries.

### GET /social/users/{userId}/following

Returns accepted following users visible to the authenticated viewer.

Response `200`: list of social user summaries.

### GET /social/users/{userId}/counters

Returns social counters for a user.

Response `200`:

```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "followersCount": 10,
  "followingCount": 4,
  "pendingRequestsCount": 0,
  "blockedCount": 0
}
```

### GET /social/users/{userId}/follow-status

Returns the authenticated viewer's follow status for a target user.

Response `200`:

```json
"ACCEPTED"
```

Possible values are defined by `ViewerFollowStatus`.

### GET /social/me/follow-requests

Returns pending follow requests received by the authenticated user.

Response `200`:

```json
[
  {
    "followId": "00000000-0000-0000-0000-000000000000",
    "user": {
      "userId": "11111111-1111-1111-1111-111111111111",
      "name": "Court Player",
      "username": "courtplayer",
      "avatarUrl": null,
      "relatedAt": "2026-06-12T12:00:00Z"
    },
    "requestedAt": "2026-06-12T12:00:00Z"
  }
]
```

### GET /social/me/blocks

Returns users blocked by the authenticated user.

Response `200`:

```json
[
  {
    "user": {
      "userId": "11111111-1111-1111-1111-111111111111",
      "name": "Court Player",
      "username": "courtplayer",
      "avatarUrl": null,
      "relatedAt": "2026-06-12T12:00:00Z"
    },
    "blockedAt": "2026-06-12T12:00:00Z"
  }
]
```

## Internal API

All internal routes require `x-internal-api-key`.

### GET /internal/social/blocked

Checks whether either user has blocked the other.

Query params:

- `userA`
- `userB`

Response `200`:

```json
{
  "blocked": true
}
```

### GET /internal/social/users/{userId}/related-blocked-users

Returns user IDs blocked by the target user or users that have blocked the target user.

Response `200`:

```json
[
  "00000000-0000-0000-0000-000000000000"
]
```

### POST /internal/social/users/{userId}/reconcile

Fetches the latest profile projection from `user-service` and upserts the local `SocialUser` snapshot.

Response `200` with an empty body.

### POST /internal/social/users/{userId}/rebuild-counter

Rebuilds one user's counters from accepted follows.

Response `200`: social counter.

### POST /internal/social/counters/rebuild

Rebuilds all social counters from accepted follows.

Response `200`: list of social counters.

## Kafka

Consumes from `user.events`:

- `USER_PROFILE_CREATED`
- `USER_PROFILE_UPDATED`
- `USER_PROFILE_DELETED`
- `USER_PROFILE_RESTORED`
- `USER_PROFILE_BECAME_PUBLIC`

Publishes to `social.events`:

- `FOLLOW_REQUESTED`
- `FOLLOW_ACCEPTED`
- `FOLLOW_REJECTED`
- `FOLLOW_REMOVED`
- `FOLLOWER_REMOVED`
- `USER_BLOCKED`
- `USER_UNBLOCKED`

Publishes social audit messages to `audit.events` when the `kafka` or `production` profile is active.

## Health

Actuator exposes:

```txt
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
GET /actuator/info
```

Example:

```bash
curl http://localhost:8084/actuator/health
```

Expected:

```json
{"status":"UP"}
```

## Tests

From `social-service`:

```bash
./mvnw test
```

## Database

Flyway migrations live under:

```txt
src/main/resources/db/migration
```

The first migration creates:

- `social_users`
- `follows`
- `blocks`
- `social_counters`
