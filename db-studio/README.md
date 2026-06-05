# CourtRank DB Studio

Prisma Studio workspace for inspecting the local Java microservice databases.

This does not make the services use Prisma. It only uses Prisma introspection and Prisma Studio as a local database browser.

## Setup

```bash
cd db-studio
cp .env.example .env
npm install
```

## Introspect Schemas

Make sure the Docker stack is running first:

```bash
cd ..
docker compose -f docker-compose.auth.yml up -d
```

Then pull the schemas:

```bash
cd db-studio
npm run pull:user
npm run pull:auth
```

## Open Studio

User service DB:

```bash
npm run studio:user
```

Auth service DB:

```bash
npm run studio:auth
```

URLs:

- User DB Studio: http://localhost:5555
- Auth DB Studio: http://localhost:5556
