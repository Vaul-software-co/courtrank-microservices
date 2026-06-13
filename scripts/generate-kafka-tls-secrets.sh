#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SECRETS_DIR="${1:-$ROOT_DIR/secrets}"
STORE_PASSWORD="${KAFKA_SSL_STORE_PASSWORD:-change-me-kafka-store-password}"
KEY_PASSWORD="${KAFKA_SSL_KEY_PASSWORD:-$STORE_PASSWORD}"
DAYS="${KAFKA_TLS_DAYS:-825}"

if [[ "$STORE_PASSWORD" != "$KEY_PASSWORD" ]]; then
  echo "KAFKA_SSL_STORE_PASSWORD and KAFKA_SSL_KEY_PASSWORD must match for generated PKCS12 stores." >&2
  exit 1
fi

mkdir -p "$SECRETS_DIR"
chmod 700 "$SECRETS_DIR"

CA_KEY="$SECRETS_DIR/kafka-ca.key"
CA_CERT="$SECRETS_DIR/kafka-ca.crt"
TRUSTSTORE="$SECRETS_DIR/kafka.truststore.p12"

if [[ ! -f "$CA_KEY" || ! -f "$CA_CERT" ]]; then
  openssl req \
    -new \
    -x509 \
    -days "$DAYS" \
    -nodes \
    -newkey rsa:4096 \
    -keyout "$CA_KEY" \
    -out "$CA_CERT" \
    -subj "/CN=courtrank-kafka-ca"
fi

rm -f "$TRUSTSTORE"

keytool \
  -importcert \
  -noprompt \
  -alias courtrank-kafka-ca \
  -file "$CA_CERT" \
  -keystore "$TRUSTSTORE" \
  -storetype PKCS12 \
  -storepass "$STORE_PASSWORD" >/dev/null

create_cert() {
  local common_name="$1"
  local output_name="$2"
  local san="${3:-}"
  local key="$SECRETS_DIR/$output_name.key"
  local csr="$SECRETS_DIR/$output_name.csr"
  local cert="$SECRETS_DIR/$output_name.crt"
  local ext="$SECRETS_DIR/$output_name.ext"
  local keystore="$SECRETS_DIR/$output_name.keystore.p12"

  if [[ -d "$keystore" ]]; then
    rmdir "$keystore"
  fi
  rm -f "$keystore"

  openssl req \
    -new \
    -nodes \
    -newkey rsa:2048 \
    -keyout "$key" \
    -out "$csr" \
    -subj "/CN=$common_name"

  if [[ -n "$san" ]]; then
    printf "subjectAltName=%s\nextendedKeyUsage=serverAuth,clientAuth\n" "$san" > "$ext"
    openssl x509 \
      -req \
      -days "$DAYS" \
      -in "$csr" \
      -CA "$CA_CERT" \
      -CAkey "$CA_KEY" \
      -CAcreateserial \
      -out "$cert" \
      -sha256 \
      -extfile "$ext"
  else
    openssl x509 \
      -req \
      -days "$DAYS" \
      -in "$csr" \
      -CA "$CA_CERT" \
      -CAkey "$CA_KEY" \
      -CAcreateserial \
      -out "$cert" \
      -sha256
  fi

  openssl pkcs12 \
    -export \
    -name "$common_name" \
    -inkey "$key" \
    -in "$cert" \
    -certfile "$CA_CERT" \
    -out "$keystore" \
    -passout "pass:$STORE_PASSWORD"

  rm -f "$csr" "$ext"
}

create_cert "kafka" "kafka" "DNS:kafka,DNS:localhost,IP:127.0.0.1"
create_cert "kafka-admin" "kafka-admin"
create_cert "auth-service" "kafka-auth-service"
create_cert "user-service" "kafka-user-service"
create_cert "social-service" "kafka-social-service"
create_cert "legacy-sync-service" "kafka-legacy-sync-service"
create_cert "audit-service" "kafka-audit-service"

cat > "$SECRETS_DIR/kafka-admin.properties" <<EOF
security.protocol=SSL
ssl.truststore.location=/run/secrets/kafka.truststore.p12
ssl.truststore.password=$STORE_PASSWORD
ssl.truststore.type=PKCS12
ssl.keystore.location=/run/secrets/kafka-admin.keystore.p12
ssl.keystore.password=$STORE_PASSWORD
ssl.keystore.type=PKCS12
ssl.key.password=$KEY_PASSWORD
EOF

chmod 600 "$SECRETS_DIR"/kafka-* "$TRUSTSTORE" "$SECRETS_DIR/kafka-admin.properties"

echo "Kafka TLS secrets written to $SECRETS_DIR"
echo "Set KAFKA_SSL_STORE_PASSWORD and KAFKA_SSL_KEY_PASSWORD to the password used for this run."
