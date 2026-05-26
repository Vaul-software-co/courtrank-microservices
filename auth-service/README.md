# Auth Service

Authentication microservice for CourtRank. It owns credentials, email verification, password reset, refresh sessions, soft delete, and authentication-related events.

## Responsibilities

- Sign up, sign in, refresh, and logout.
- Password hashing with Argon2 plus pepper.
- RSA JWT access, refresh, and password reset tokens.
- Email verification and password reset OTP flows.
- Refresh session persistence and session revocation.
- Soft delete and restore-compatible authentication state.
- Kafka publication for auth and audit events.
- HTTP cookies for web clients and token responses for mobile clients.

This service does not own user profile data such as name, username, avatar, or public profile fields. Those belong in `users-service`.

## Local Requirements

- Java 21
- Docker and Docker Compose
- PostgreSQL if running without Docker Compose
- Kafka if running with the `kafka` profile outside Docker Compose

## Environment

Copy the example file and fill the real values locally:

```bash
cp auth-service/.env.example auth-service/.env
```

For Docker Compose, also create a root `.env` from the root example:

```bash
cp .env.example .env
```

Required important variables:

```env
SPRING_PROFILES_ACTIVE=local,kafka
SERVER_PORT=8080

DATABASE_URL=jdbc:postgresql://localhost:5432/courtrank_auth
DATABASE_USERNAME=courtrank_auth_user
DATABASE_PASSWORD=change-me

JWT_PRIVATE_KEY=classpath:private.pem
JWT_PUBLIC_KEY=classpath:public.pem
PASSWORD_PEPPER=change-me

WEB_API_KEY=change-me
MOBILE_API_KEY=change-me

FRONTEND_URL=http://localhost:3000
CORS_ALLOWED_ORIGINS=http://localhost:3000

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Do not commit real `.env` files, PEM keys, peppers, API keys, or database credentials.

## Profiles

- `local`: uses console adapters for email/audit/events when no real bean is available.
- `kafka`: publishes auth and audit events to Kafka.
- `production`: intended for production adapters such as Resend and Kafka.
- `test`: used by tests.

For local Docker Compose, the default is:

```env
SPRING_PROFILES_ACTIVE=local,kafka
```

## Run With Docker Compose

From the repository root:

```bash
docker compose -f docker-compose.auth.yml up -d --build
```

This starts:

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
docker compose -f docker-compose.auth.yml logs -f auth-service
```

Stop containers:

```bash
docker compose -f docker-compose.auth.yml down
```

Reset the Docker Postgres data:

```bash
docker compose -f docker-compose.auth.yml down -v
docker compose -f docker-compose.auth.yml up -d --build
```

## Run Without Docker

From `auth-service`:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

The service listens on `SERVER_PORT`, defaulting to `8080`.

## Build Docker Image

From `auth-service`:

```bash
docker build -t courtrank-auth .
```

Run manually:

```bash
docker run --env-file .env -p 8081:8080 courtrank-auth
```

When running manually with `docker run`, remember that `localhost` inside the container is the container itself. Prefer Docker Compose for local infrastructure.

## Health

Actuator exposes:

```txt
GET /actuator/health
GET /actuator/info
```

Example:

```bash
curl http://localhost:8081/actuator/health
```

Expected:

```json
{"status":"UP"}
```

## API Contract

Public auth routes require:

```http
x-api-key: <web-or-mobile-api-key>
Content-Type: application/json
```

Authenticated routes require either the web `token` cookie or:

```http
Authorization: Bearer <access-token>
```

### POST /auth/signup

Creates an authentication record and sends the verification email.

Request:

```json
{
  "name": "Sebastian Sanchez",
  "username": "sebas",
  "email": "test@example.com",
  "password": "Password1!",
  "terms": true,
  "termsVersion": "2026-01",
  "commercial": false
}
```

Accepted aliases:

- `terms`: `isTerms`, `acceptedTerms`
- `termsVersion`: `acceptedTermsVersion`
- `commercial`: `isCommercial`, `acceptedDataCommercialization`

Response `201`:

```json
{
  "message": "User registered. Check your email to verify your account."
}
```

### POST /auth/signin

Authenticates the user and creates a refresh session.

Request:

```json
{
  "email": "test@example.com",
  "password": "Password1!"
}
```

Web response `200`:

```json
{
  "message": "Login successful"
}
```

If the user has a default club, web response can include:

```json
{
  "message": "Login successful",
  "clubId": "00000000-0000-0000-0000-000000000000"
}
```

Web also receives HTTP-only cookies:

- `token`
- `refreshToken`

Mobile response `200`:

```json
{
  "token": "<access-token>",
  "refreshToken": "<refresh-token>"
}
```

### POST /auth/refresh

Rotates the refresh session and returns a new access/refresh pair.

Request can use the `refreshToken` cookie or body:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Web response `200`:

```json
{
  "message": "Login successful"
}
```

Web also receives new `token` and `refreshToken` cookies.

Mobile response `200`:

```json
{
  "token": "<access-token>",
  "refreshToken": "<refresh-token>"
}
```

### POST /auth/logout

### DELETE /auth/logout

Revokes a refresh session and clears auth cookies for web clients.

Request can use `refreshToken` cookie, query parameter, or body:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Response `200`:

```json
{
  "message": "Logout successful"
}
```

### POST /auth/resend-verification-email

### POST /auth/verify-email/resend

Resends the account verification email.

Request:

```json
{
  "email": "test@example.com",
  "lang": "es"
}
```

Response `200`:

```json
{
  "message": "Verification email sent"
}
```

### POST /auth/verify-email

### POST /auth/verify-email/confirm

Verifies an account email.

Request:

```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "token": "<verification-token>",
  "password": "Password1!"
}
```

Response `200`:

```json
{
  "message": "Email verified"
}
```

### POST /auth/request-password-reset

### POST /auth/password-reset/request

Requests a password reset OTP email.

Request:

```json
{
  "email": "test@example.com",
  "lang": "es"
}
```

Response `200`:

```json
{
  "message": "If the email exists, you will receive a code"
}
```

### POST /auth/verify-password-otp

### POST /auth/password-reset/verify

Verifies the password reset OTP and creates a short-lived reset token.

Request:

```json
{
  "email": "test@example.com",
  "otp": "123456"
}
```

Web response `200`:

```json
{
  "message": "OTP verified"
}
```

Web also receives an HTTP-only `resetToken` cookie.

Mobile response `200`:

```json
{
  "message": "OTP verified",
  "resetToken": "<password-reset-token>"
}
```

### POST /auth/reset-password

### PUT /auth/password-reset/confirm

Confirms password reset and updates the password.

Request can use the `resetToken` cookie or body:

```json
{
  "newPassword": "NewPassword1!",
  "resetToken": "<password-reset-token>"
}
```

For web, `resetToken` is normally read from the HTTP-only cookie.

Response `200`:

```json
{
  "message": "Password updated successfully"
}
```

### POST /auth/change-password

Changes the password for the authenticated user.

Request:

```json
{
  "oldPassword": "OldPassword1!",
  "newPassword": "NewPassword1!"
}
```

Response `200`:

```json
{
  "message": "Password updated successfully"
}
```

### DELETE /auth/me

Soft deletes the authenticated user.

Response `200`:

```json
{
  "message": "User deleted"
}
```

### GET /auth/sessions

Lists active sessions for the authenticated user.

Response `200`:

```json
[
  {
    "id": "00000000-0000-0000-0000-000000000000",
    "client": "web",
    "ip": "127.0.0.1",
    "userAgent": "Safari",
    "createdAt": "2026-05-26T20:00:00Z",
    "expiresAt": "2026-06-02T20:00:00Z"
  }
]
```

### DELETE /auth/sessions/{sessionId}

Revokes one session for the authenticated user.

Response `200`:

```json
{
  "message": "Session revoked"
}
```

### DELETE /auth/sessions

Revokes all sessions for the authenticated user.

Response `200`:

```json
{
  "message": "Sessions revoked"
}
```

### Common Errors

Examples:

```json
{ "error": "Invalid API key" }
```

```json
{ "error": "Authentication required" }
```

```json
{ "error": "Access denied" }
```

```json
{ "error": "Too many requests" }
```

Validation errors return `400` with an `error` field and field-level details when available.

## Web And Mobile Token Behavior

Web clients use HTTP-only cookies:

- `token`
- `refreshToken`
- `resetToken`

Mobile clients can receive tokens in the JSON response where needed.

The password reset cookie path intentionally matches the webapp route handler:

```txt
/api/auth/password-reset/confirm
```

## Kafka

Default topics:

```txt
auth.events
audit.events
```

Kafka UI is available locally through Docker Compose:

```txt
http://localhost:8085
```

Do not expose Kafka UI publicly without protection such as VPN or Cloudflare Access.

## Database

Flyway runs automatically at startup.

Current core tables:

- `authentications`
- `sessions`
- `verification_tokens`
- `flyway_schema_history`

For Docker Compose, the app connects to:

```txt
jdbc:postgresql://auth-postgres:5432/courtrank_auth
```

## Tests

Run all tests:

```bash
./mvnw test
```

Run controller tests:

```bash
./mvnw test -Dtest=AuthControllerIntegrationTest
```

Some persistence tests use Testcontainers and require Docker access.

## Production Notes

- Inject secrets with AWS Secrets Manager, SSM Parameter Store, or the deployment platform.
- Do not bake `.env` or PEM files into the Docker image.
- Use RDS/Aurora PostgreSQL for production.
- Use Cloudflare/WAF or Redis for distributed rate limiting when running more than one instance.
- Keep `/actuator/health` available for load balancer/container health checks.
- Keep Kafka UI private.
