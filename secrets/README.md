# Local Secrets

This directory is for local/runtime secrets and is ignored by git.

Create these files locally:

```txt
auth-service.env
user-service.env
social-service.env
auth-postgres.env
user-postgres.env
social-postgres.env
jwt-private.pem
jwt-public.pem
kafka-ca.crt
kafka.truststore.p12
kafka.keystore.p12
kafka-admin.keystore.p12
kafka-admin.properties
kafka-auth-service.keystore.p12
kafka-user-service.keystore.p12
kafka-social-service.keystore.p12
kafka-legacy-sync-service.crt
kafka-legacy-sync-service.key
```

Use the example files in this directory as templates:

```bash
cp secrets/compose.prod.env.example secrets/compose.prod.env
cp secrets/auth-service.env.example secrets/auth-service.env
cp secrets/user-service.env.example secrets/user-service.env
cp secrets/social-service.env.example secrets/social-service.env
cp secrets/auth-postgres.env.example secrets/auth-postgres.env
cp secrets/user-postgres.env.example secrets/user-postgres.env
cp secrets/social-postgres.env.example secrets/social-postgres.env
```

Create the JWT key pair locally:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/jwt-private.pem
openssl rsa -pubout -in secrets/jwt-private.pem -out secrets/jwt-public.pem
```

The JWT private key must only be mounted into `auth-service`.
The JWT public key can be mounted into services that need to verify access tokens.
Do not put runtime PEM files in service `src/main/resources`; Docker and Maven are configured to exclude them from builds.

Create Kafka TLS/mTLS material locally:

```bash
KAFKA_SSL_STORE_PASSWORD=change-me KAFKA_SSL_KEY_PASSWORD=change-me \
  scripts/generate-kafka-tls-secrets.sh secrets
```

The production Compose file uses SSL on the internal Kafka listener, requires client certificates, and creates ACLs from `kafka-init`.
Use strong, rotated passwords outside development and keep `KAFKA_SSL_STORE_PASSWORD` and `KAFKA_SSL_KEY_PASSWORD` in the production Compose env file.

For production, prefer a directory outside the repository such as `/opt/courtrank/secrets` and set `COURTRANK_SECRETS_DIR` in the Compose env file.
