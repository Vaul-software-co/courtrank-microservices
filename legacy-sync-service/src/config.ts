import "dotenv/config";

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`${name} is required`);
  return value;
}

function optionalInt(name: string, fallback: number): number {
  const value = process.env[name]?.trim();
  if (!value) return fallback;
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${name} must be a positive integer`);
  }
  return parsed;
}

const kafkaSslEnabled = (process.env.KAFKA_SSL_ENABLED?.trim() || "false").toLowerCase() === "true";

function optionalKafkaSslPath(name: string): string | undefined {
  const value = process.env[name]?.trim();
  if (!kafkaSslEnabled || value) return value;
  throw new Error(`${name} is required when KAFKA_SSL_ENABLED=true`);
}

export const config = {
  legacyDatabaseUrl: required("LEGACY_DATABASE_URL"),
  authDatabaseUrl: required("AUTH_DATABASE_URL"),
  userDatabaseUrl: required("USER_DATABASE_URL"),
  socialDatabaseUrl: required("SOCIAL_DATABASE_URL"),
  kafkaBrokers: required("KAFKA_BROKERS").split(",").map((s) => s.trim()).filter(Boolean),
  kafkaClientId: process.env.KAFKA_CLIENT_ID?.trim() || "courtrank-legacy-sync",
  kafkaGroupId: process.env.KAFKA_GROUP_ID?.trim() || "courtrank-legacy-sync",
  kafkaSslEnabled,
  kafkaSslCaPath: optionalKafkaSslPath("KAFKA_SSL_CA_PATH"),
  kafkaSslCertPath: optionalKafkaSslPath("KAFKA_SSL_CERT_PATH"),
  kafkaSslKeyPath: optionalKafkaSslPath("KAFKA_SSL_KEY_PATH"),
  kafkaSslRejectUnauthorized:
    (process.env.KAFKA_SSL_REJECT_UNAUTHORIZED?.trim() || "true").toLowerCase() !== "false",
  authEventsTopic: process.env.AUTH_EVENTS_TOPIC?.trim() || "auth.events",
  userEventsTopic: process.env.USER_EVENTS_TOPIC?.trim() || "user.events",
  legacyPlaceholderPasswordHash:
    process.env.LEGACY_PLACEHOLDER_PASSWORD_HASH?.trim() || "managed-by-auth-service",
  backfillBatchSize: optionalInt("BACKFILL_BATCH_SIZE", 500),
};
