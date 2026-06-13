# Runtime Secrets

This directory is for local/runtime secrets and is ignored by git.

Do not commit real `.env` files, PEM keys, Kafka keystores, API keys, database passwords, peppers, or TLS material.

## Local Files

Create these local files from examples:

```txt
compose.env
compose.prod.env
auth-service.env
user-service.env
social-service.env
audit-service.env
legacy-sync-service.env
auth-postgres.env
user-postgres.env
social-postgres.env
audit-postgres.env
```

Example copy commands:

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

`compose.env` is used by local Compose. It should include shared Compose variables such as Kafka SSL passwords:

```env
KAFKA_SSL_STORE_PASSWORD=change-me
KAFKA_SSL_KEY_PASSWORD=change-me
```

## JWT Keys

Create the JWT key pair locally:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/jwt-private.pem
openssl rsa -pubout -in secrets/jwt-private.pem -out secrets/jwt-public.pem
```

Required files:

```txt
jwt-private.pem
jwt-public.pem
```

The JWT private key must only be mounted into `auth-service`.

The JWT public key can be mounted into services that verify access tokens:

- `auth-service`
- `user-service`
- `social-service`
- `audit-service`

Do not put runtime PEM files in service `src/main/resources`; Docker and Maven are configured to exclude them from builds.

## Kafka TLS Material

Create Kafka TLS/mTLS material locally:

```bash
KAFKA_SSL_STORE_PASSWORD=change-me KAFKA_SSL_KEY_PASSWORD=change-me \
  scripts/generate-kafka-tls-secrets.sh secrets
```

Expected Kafka files:

```txt
kafka-ca.crt
kafka.truststore.p12
kafka.keystore.p12
kafka-admin.keystore.p12
kafka-admin.properties
kafka-auth-service.keystore.p12
kafka-user-service.keystore.p12
kafka-social-service.keystore.p12
kafka-audit-service.keystore.p12
kafka-legacy-sync-service.crt
kafka-legacy-sync-service.key
```

Kafka uses:

- broker keystore: `kafka.keystore.p12`
- shared truststore: `kafka.truststore.p12`
- admin client keystore: `kafka-admin.keystore.p12`
- per-service client keystores for Java services
- PEM cert/key pair for `legacy-sync-service`

The production Compose file uses SSL on the internal Kafka listener, requires client certificates, and creates ACLs from `kafka-init`.

Use strong, rotated passwords outside development and keep these values in the production Compose env file:

```env
KAFKA_SSL_STORE_PASSWORD=<strong-password>
KAFKA_SSL_KEY_PASSWORD=<strong-password>
```

## Production Location

For production, prefer a directory outside the repository:

```txt
/opt/courtrank/secrets
```

Set:

```env
COURTRANK_SECRETS_DIR=/opt/courtrank/secrets
```

The production Compose file reads service env files from `COURTRANK_SECRETS_DIR` and mounts keys/keystores from that same directory.

## Production Gateway And Legacy

When the legacy backend runs on another VPS, configure the gateway with the private or firewall-restricted host:

```env
LEGACY_BACKEND_HOST=<legacy-private-ip-or-dns>
LEGACY_BACKEND_PORT=4000
WORKER_ACCESS_SERVICE_URL=http://<legacy-private-ip-or-dns>:4000/internal
```

Do not rely on `host.docker.internal` in production unless the legacy backend is intentionally running on the same host.

## Production Images

`compose.prod.env` should point to prebuilt images:

```env
AUTH_SERVICE_IMAGE=ghcr.io/your-org/courtrank-auth:1.0.0
USER_SERVICE_IMAGE=ghcr.io/your-org/courtrank-user:1.0.0
SOCIAL_SERVICE_IMAGE=ghcr.io/your-org/courtrank-social:1.0.0
AUDIT_SERVICE_IMAGE=ghcr.io/your-org/courtrank-audit:1.0.0
LEGACY_SYNC_SERVICE_IMAGE=ghcr.io/your-org/courtrank-legacy-sync:1.0.0
```

Avoid building images on small production VPS instances.

## Checklist

Before local `docker compose up`, verify:

- service `.env` files exist
- Postgres `.env` files exist
- JWT private/public keys exist
- Kafka TLS files exist
- `compose.env` includes Kafka SSL passwords

Before production deploy, verify:

- `/opt/courtrank/secrets` exists on the target host
- `compose.prod.env` contains image tags and remote legacy host
- JWT private key is only mounted into `auth-service`
- Kafka keystore passwords match generated files
- firewall allows gateway to reach data and legacy hosts
- no database/Kafka ports are publicly exposed
