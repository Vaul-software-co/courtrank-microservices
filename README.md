# CourtRank Microservices

Java and Node microservice workspace for the CourtRank backend migration.

The current architecture keeps the TypeScript legacy backend alive while selected domains move into dedicated services behind an API gateway.

## Services

| Service | Port | Owns | Database | Kafka |
| --- | ---: | --- | --- | --- |
| `auth-service` | `8080` in container, `8081` local host | credentials, sessions, JWTs, verification, password reset, auth lifecycle | `courtrank_auth` | publishes `auth.events`, `audit.events` |
| `user-service` | `8082` | profile, username, avatar reference, privacy, language, profile projection | `courtrank_users` | consumes `auth.events`; publishes `user.events`, `audit.events` |
| `social-service` | `8084` | follows, follow requests, blocks, social counters, social search projection | `courtrank_social` | consumes `user.events`; publishes `social.events`, `audit.events` |
| `audit-service` | `8086` | immutable operational audit trail | `courtrank_audit` | consumes `audit.events` |
| `legacy-sync-service` | none | temporary compatibility sync during migration | reads/writes service DBs and legacy DB | consumes `auth.events`, `user.events` |
| `api-gateway` | `8080` local/prod container | public routing boundary | none | none |

The TypeScript legacy backend remains the owner of domains that have not been migrated yet: clubs, bookings, courts, loyalty, competitions, checkout, notifications, and related admin flows.

## Routing Model

The gateway is the only public HTTP entrypoint.

Current migrated routes:

- `/auth/*` -> `auth-service`
- `/users/*` -> `user-service`
- `/social/*` -> `social-service`
- `/audit/*` -> `audit-service`
- `/internal/users/*` -> `user-service` in local only
- `/internal/social/*` -> `social-service` in local only
- `/internal/auth/*` -> `auth-service` in local only

Compatibility routes under `/user/*` are split by ownership:

- profile reads/writes -> `user-service`
- social actions/search/followers/blocks -> `social-service`
- password, delete-me, data consent -> `auth-service`
- still-unmigrated user routes -> legacy backend

All other routes fall through to the legacy backend.

## Data Ownership

Each service owns its own database schema. Cross-service state is shared through events and internal read APIs.

- `auth-service` emits lifecycle events when auth state changes.
- `user-service` builds profile records from auth lifecycle events.
- `social-service` builds social projections from user profile events.
- `audit-service` stores audit events from all services.
- `legacy-sync-service` exists only for the migration period.

Avoid direct database access across service boundaries except inside `legacy-sync-service`, which is explicitly temporary.

## Kafka Topics

| Topic | Producers | Consumers |
| --- | --- | --- |
| `auth.events` | `auth-service` | `user-service`, `legacy-sync-service` |
| `user.events` | `user-service` | `social-service`, `legacy-sync-service` |
| `social.events` | `social-service` | currently no required service consumer |
| `audit.events` | `auth-service`, `user-service`, `social-service` | `audit-service` |

Local and production Compose create topics and ACLs through `kafka-init`.

## Local Development

Create local secret files first:

```bash
cp secrets/compose.prod.env.example secrets/compose.prod.env
cp secrets/auth-service.env.example secrets/auth-service.env
cp secrets/user-service.env.example secrets/user-service.env
cp secrets/social-service.env.example secrets/social-service.env
cp secrets/audit-service.env.example secrets/audit-service.env
cp secrets/legacy-sync-service.env.example secrets/legacy-sync-service.env
cp secrets/auth-postgres.env.example secrets/auth-postgres.env
cp secrets/user-postgres.env.example secrets/user-postgres.env
cp secrets/social-postgres.env.example secrets/social-postgres.env
cp secrets/audit-postgres.env.example secrets/audit-postgres.env
```

Generate JWT keys:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/jwt-private.pem
openssl rsa -pubout -in secrets/jwt-private.pem -out secrets/jwt-public.pem
```

Generate Kafka TLS material:

```bash
KAFKA_SSL_STORE_PASSWORD=change-me KAFKA_SSL_KEY_PASSWORD=change-me \
  scripts/generate-kafka-tls-secrets.sh secrets
```

Start the local stack:

```bash
docker compose --env-file secrets/compose.env -f docker-compose.auth.yml up -d --build
```

Check status:

```bash
docker compose --env-file secrets/compose.env -f docker-compose.auth.yml ps
```

Gateway health:

```bash
curl http://localhost:8080/health
```

## Production Layout

Recommended minimal VPS split:

- `gateway-apps`: public VPS. Runs `api-gateway`, Java services, and `legacy-sync-service`.
- `data`: private or firewall-restricted VPS. Runs PostgreSQL and Kafka.
- `legacy`: private or firewall-restricted VPS. Runs the TypeScript legacy backend.

Only `gateway-apps` should expose public `80/443`. Service ports, Postgres, Kafka, and legacy backend ports should only be reachable from trusted hosts.

Production Compose expects images to already exist in a registry. Do not build images on small production VPS instances.

Important production values:

```env
COURTRANK_SECRETS_DIR=/opt/courtrank/secrets
LEGACY_BACKEND_HOST=<private-or-firewalled-legacy-host>
LEGACY_BACKEND_PORT=4000
WORKER_ACCESS_SERVICE_URL=http://<private-or-firewalled-legacy-host>:4000/internal
```

Run production Compose:

```bash
COURTRANK_SECRETS_DIR=/opt/courtrank/secrets \
docker compose --env-file /opt/courtrank/secrets/compose.prod.env -f docker-compose.prod.yml up -d
```

## Migration State

Migrated:

- auth signup/signin/session/password reset/email verification
- user profile base reads/writes
- social follows/blocks/search/counters
- centralized audit ingestion and SUPER_ADMIN audit reads

Still legacy-owned:

- clubs
- courts
- bookings
- checkout/subscriptions
- loyalty
- competitions
- notifications
- remaining admin/BFF workflows not explicitly routed to a microservice

Compatibility aliases exist so older `/user/*` clients can keep working while the frontend and backend clients move to the more specific service routes.

## Security Notes

- JWT private key is mounted only into `auth-service`.
- JWT public key is mounted into services that verify access tokens.
- Internal APIs require `x-internal-api-key`.
- Kafka production traffic uses SSL/mTLS and ACLs.
- `audit-service` HTTP endpoints require `SUPER_ADMIN`.
- Runtime secrets live under `secrets/` locally or `/opt/courtrank/secrets` in production and must not be committed.

## Useful Docs

- [Auth Service](auth-service/README.md)
- [User Service](user-service/README.md)
- [Social Service](social-service/README.md)
- [Audit Service](audit-service/README.md)
- [Legacy Sync Service](legacy-sync-service/README.md)
- [Secrets](secrets/README.md)
- [DB Studio](db-studio/README.md)
