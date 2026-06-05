#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AUTH_BASE="${AUTH_BASE:-http://localhost:8081}"
USER_BASE="${USER_BASE:-http://localhost:8082}"
GATEWAY_BASE="${GATEWAY_BASE:-http://localhost:8080}"
SECRETS_DIR="${SECRETS_DIR:-$ROOT_DIR/secrets}"

if [[ ! -f "$SECRETS_DIR/auth-service.env" ]]; then
  echo "Missing $SECRETS_DIR/auth-service.env" >&2
  exit 1
fi

env_value() {
  local file="$1"
  local key="$2"
  local value

  value="$(grep -E "^${key}=" "$file" | tail -1 | cut -d '=' -f 2-)"
  value="${value%\"}"
  value="${value#\"}"
  value="${value%\'}"
  value="${value#\'}"
  printf '%s' "$value"
}

WEB_API_KEY="$(env_value "$SECRETS_DIR/auth-service.env" WEB_API_KEY)"
MOBILE_API_KEY="$(env_value "$SECRETS_DIR/auth-service.env" MOBILE_API_KEY)"
INTERNAL_API_KEY="$(env_value "$SECRETS_DIR/auth-service.env" INTERNAL_API_KEY)"

if [[ -z "$WEB_API_KEY" || -z "$MOBILE_API_KEY" || -z "$INTERNAL_API_KEY" ]]; then
  echo "WEB_API_KEY, MOBILE_API_KEY, and INTERNAL_API_KEY are required in $SECRETS_DIR/auth-service.env" >&2
  exit 1
fi

tmp_dir="$(mktemp -d)"
cookie_jar="$tmp_dir/cookies.txt"
smoke_ip="198.51.$((RANDOM % 255)).$((RANDOM % 255))"
trap 'rm -rf "$tmp_dir"' EXIT

pass() {
  printf 'PASS %s\n' "$1"
}

fail() {
  printf 'FAIL %s\n' "$1" >&2
  if [[ -f "$tmp_dir/last_body" ]]; then
    sed 's/"token":"[^"]*"/"token":"<redacted>"/g; s/"refreshToken":"[^"]*"/"refreshToken":"<redacted>"/g; s/"resetToken":"[^"]*"/"resetToken":"<redacted>"/g' "$tmp_dir/last_body" >&2
  fi
  exit 1
}

request() {
  local method="$1"
  local url="$2"
  local expected="$3"
  local body=""

  if [[ $# -ge 4 ]]; then
    body="$4"
    shift 4
  else
    shift 3
  fi

  local response="$tmp_dir/last_body"
  local status

  if [[ -n "$body" ]]; then
    status="$(curl -sS -o "$response" -w '%{http_code}' -X "$method" "$url" \
      -b "$cookie_jar" -c "$cookie_jar" -H "x-forwarded-for: $smoke_ip" -H 'Content-Type: application/json' "$@" --data "$body")"
  else
    status="$(curl -sS -o "$response" -w '%{http_code}' -X "$method" "$url" \
      -b "$cookie_jar" -c "$cookie_jar" -H "x-forwarded-for: $smoke_ip" "$@")"
  fi

  if [[ "$status" != "$expected" ]]; then
    fail "$method $url expected $expected got $status"
  fi
}

json_value() {
  jq -r "$1 // empty" "$tmp_dir/last_body"
}

set_auth_headers() {
  auth_headers=()
  if [[ -n "${access_token:-}" && "${access_token:-}" != "null" ]]; then
    auth_headers=(-H "Authorization: Bearer $access_token")
  fi
}

wait_for_user_profile() {
  local token="$1"
  local attempts=30

  for _ in $(seq 1 "$attempts"); do
    local curl_args=(-b "$cookie_jar" -c "$cookie_jar")
    if [[ -n "$token" && "$token" != "null" ]]; then
      curl_args+=(-H "Authorization: Bearer $token")
    fi

    if curl -sS -f "$USER_BASE/users/me" "${curl_args[@]}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  fail "GET /users/me did not become available"
}

latest_auth_log_for_email() {
  local email="$1"
  docker logs courtrank-auth --tail 500 2>&1 | grep "$email" | tail -1
}

extract_query_param() {
  local text="$1"
  local key="$2"
  printf '%s\n' "$text" | sed -n "s/.*[?&]$key=\\([^& ]*\\).*/\\1/p"
}

extract_otp() {
  local text="$1"
  printf '%s\n' "$text" | sed -n 's/.* otp=\([0-9][0-9]*\).*/\1/p'
}

run_id="$(date +%s)"
email="smoke-${run_id}@example.com"
username="smoke_${run_id}"
updated_username="smokeu_${run_id}"
alias_email="smoke-alias-${run_id}@example.com"
alias_username="smokea_${run_id}"
password="Password1!"
changed_password="Changed1!"
reset_password="Reset123!"
final_password="Final123!"
avatar_key="users/00000000-0000-0000-0000-000000000000/avatar.webp"

request GET "$AUTH_BASE/actuator/health" 200
pass "GET /actuator/health auth"

request GET "$AUTH_BASE/actuator/health/readiness" 200
pass "GET /actuator/health/readiness auth"

request GET "$USER_BASE/actuator/health" 401
pass "GET /actuator/health user protected from host"

request GET "$GATEWAY_BASE/health" 200
pass "GET /health gateway"

signup_body="$(jq -nc \
  --arg name "Smoke User" \
  --arg username "$username" \
  --arg email "$email" \
  --arg password "$password" \
  '{name:$name, username:$username, email:$email, password:$password, terms:true, termsVersion:"2026-01", commercial:false}')"
request POST "$AUTH_BASE/auth/signup" 201 "$signup_body" -H "x-api-key: $MOBILE_API_KEY"
pass "POST /auth/signup"

request POST "$AUTH_BASE/auth/resend-verification-email" 200 "$(jq -nc --arg email "$email" '{email:$email, lang:"es"}')" -H "x-api-key: $MOBILE_API_KEY"
pass "POST /auth/resend-verification-email"

request POST "$AUTH_BASE/auth/verify-email/resend" 200 "$(jq -nc --arg email "$email" '{email:$email, lang:"en"}')" -H "x-api-key: $MOBILE_API_KEY"
pass "POST /auth/verify-email/resend"

sleep 2
verification_log="$(latest_auth_log_for_email "$email")"
verification_token="$(extract_query_param "$verification_log" token)"
user_id="$(extract_query_param "$verification_log" userId)"

if [[ -z "$verification_token" || -z "$user_id" ]]; then
  fail "could not extract email verification token from auth logs"
fi

verify_body="$(jq -nc --arg userId "$user_id" --arg token "$verification_token" --arg password "$password" '{userId:$userId, token:$token, password:$password}')"
request POST "$AUTH_BASE/auth/verify-email/confirm" 200 "$verify_body" -H "x-api-key: $MOBILE_API_KEY"
pass "POST /auth/verify-email/confirm"

alias_signup_body="$(jq -nc \
  --arg name "Smoke Alias User" \
  --arg username "$alias_username" \
  --arg email "$alias_email" \
  --arg password "$password" \
  '{name:$name, username:$username, email:$email, password:$password, terms:true, termsVersion:"2026-01", commercial:false}')"
request POST "$AUTH_BASE/auth/signup" 201 "$alias_signup_body" -H "x-api-key: $MOBILE_API_KEY"
sleep 2
alias_verification_log="$(latest_auth_log_for_email "$alias_email")"
alias_verification_token="$(extract_query_param "$alias_verification_log" token)"
alias_user_id="$(extract_query_param "$alias_verification_log" userId)"
if [[ -z "$alias_verification_token" || -z "$alias_user_id" ]]; then
  fail "could not extract alias email verification token from auth logs"
fi

alias_verify_body="$(jq -nc --arg userId "$alias_user_id" --arg token "$alias_verification_token" --arg password "$password" '{userId:$userId, token:$token, password:$password}')"
request POST "$AUTH_BASE/auth/verify-email" 200 "$alias_verify_body" -H "x-api-key: $MOBILE_API_KEY"
pass "POST /auth/verify-email"

request POST "$AUTH_BASE/auth/signin" 200 "$(jq -nc --arg email "$alias_email" --arg password "$password" '{email:$email, password:$password}')" -H "x-api-key: $MOBILE_API_KEY"
alias_access_token="$(json_value '.token')"
alias_auth_headers=()
if [[ -n "$alias_access_token" && "$alias_access_token" != "null" ]]; then
  alias_auth_headers=(-H "Authorization: Bearer $alias_access_token")
fi
request DELETE "$AUTH_BASE/auth/me" 200 "" "${alias_auth_headers[@]}"
pass "DELETE /auth/me alias cleanup"

signin_body="$(jq -nc --arg email "$email" --arg password "$password" '{email:$email, password:$password}')"
request POST "$AUTH_BASE/auth/signin" 200 "$signin_body" -H "x-api-key: $MOBILE_API_KEY"
access_token="$(json_value '.token')"
refresh_token="$(json_value '.refreshToken')"
set_auth_headers
if [[ -z "$refresh_token" || "$refresh_token" == "null" ]]; then
  fail "POST /auth/signin did not return a refresh token"
fi
pass "POST /auth/signin"

if [[ -n "$refresh_token" && "$refresh_token" != "null" ]]; then
  refresh_body="$(jq -nc --arg refreshToken "$refresh_token" '{refreshToken:$refreshToken}')"
else
  refresh_body='{}'
fi
refresh_status="$(curl -sS -o "$tmp_dir/last_body" -w '%{http_code}' -X POST "$AUTH_BASE/auth/refresh" \
  -H "x-forwarded-for: $smoke_ip" \
  -H "x-api-key: $MOBILE_API_KEY" \
  -H 'Content-Type: application/json' \
  --data "$refresh_body")"
if [[ "$refresh_status" != "200" ]]; then
  fail "POST $AUTH_BASE/auth/refresh expected 200 got $refresh_status"
fi
access_token="$(json_value '.token')"
refresh_token="$(json_value '.refreshToken')"
set_auth_headers
pass "POST /auth/refresh"

wait_for_user_profile "$access_token"

request GET "$USER_BASE/users/me" 200 "" "${auth_headers[@]}"
profile_id="$(json_value '.id')"
pass "GET /users/me"

request PATCH "$USER_BASE/users/me" 200 "$(jq -nc --arg username "$updated_username" '{name:"Smoke User Updated", username:$username, phoneNumber:"+573001112233", gender:"MALE"}')" "${auth_headers[@]}"
pass "PATCH /users/me"

request PATCH "$USER_BASE/users/me/privacy" 200 '{"privateProfile":false}' "${auth_headers[@]}"
pass "PATCH /users/me/privacy"

request PATCH "$USER_BASE/users/me/lang" 200 '{"lang":"es"}' "${auth_headers[@]}"
pass "PATCH /users/me/lang"

request PUT "$USER_BASE/users/me/avatar" 200 "$(jq -nc --arg avatarKey "$avatar_key" '{avatarKey:$avatarKey}')" "${auth_headers[@]}"
pass "PUT /users/me/avatar"

request DELETE "$USER_BASE/users/me/avatar" 200 "" "${auth_headers[@]}"
pass "DELETE /users/me/avatar"

request GET "$USER_BASE/users/search?q=$updated_username&limit=20" 200 "" "${auth_headers[@]}"
pass "GET /users/search"

request GET "$USER_BASE/users/$profile_id" 200 "" "${auth_headers[@]}"
pass "GET /users/{id}"

request GET "$USER_BASE/internal/users/username-available?username=${updated_username}_x&userId=$profile_id" 200 "" -H "x-internal-api-key: $INTERNAL_API_KEY"
pass "GET /internal/users/username-available"

request GET "$USER_BASE/internal/users/$profile_id/summary" 200 "" -H "x-internal-api-key: $INTERNAL_API_KEY"
pass "GET /internal/users/{id}/summary"

request POST "$USER_BASE/internal/users/summaries" 200 "$(jq -nc --arg id "$profile_id" '{userIds:[$id]}')" -H "x-internal-api-key: $INTERNAL_API_KEY"
pass "POST /internal/users/summaries"

request GET "$USER_BASE/internal/users/$profile_id/active" 200 "" -H "x-internal-api-key: $INTERNAL_API_KEY"
pass "GET /internal/users/{id}/active"

request POST "$USER_BASE/internal/users/$profile_id/ban" 200 "$(jq -nc --arg id "$profile_id" '{adminUserId:$id}')" -H "x-internal-api-key: $INTERNAL_API_KEY"
pass "POST /internal/users/{id}/ban"

request POST "$USER_BASE/internal/users/$profile_id/unban" 200 "$(jq -nc --arg id "$profile_id" '{adminUserId:$id}')" -H "x-internal-api-key: $INTERNAL_API_KEY"
pass "POST /internal/users/{id}/unban"

request POST "$AUTH_BASE/auth/me/data-consent" 200 '{"accept":true}' "${auth_headers[@]}"
pass "POST /auth/me/data-consent"

request GET "$AUTH_BASE/auth/sessions" 200 "" "${auth_headers[@]}"
session_id="$(json_value '.[0].id')"
pass "GET /auth/sessions"

request GET "$AUTH_BASE/internal/auth/sessions/$session_id/active" 200 "" -H "x-internal-api-key: $INTERNAL_API_KEY"
pass "GET /internal/auth/sessions/{sessionId}/active"

request POST "$AUTH_BASE/auth/change-password" 200 "$(jq -nc --arg old "$password" --arg new "$changed_password" '{oldPassword:$old, newPassword:$new}')" "${auth_headers[@]}"
pass "POST /auth/change-password"

request POST "$AUTH_BASE/auth/request-password-reset" 200 "$(jq -nc --arg email "$email" '{email:$email, lang:"es"}')" -H "x-api-key: $MOBILE_API_KEY"
pass "POST /auth/request-password-reset"

request POST "$AUTH_BASE/auth/password-reset/request" 200 "$(jq -nc --arg email "$email" '{email:$email, lang:"en"}')" -H "x-api-key: $MOBILE_API_KEY"
pass "POST /auth/password-reset/request"

sleep 2
otp_log="$(latest_auth_log_for_email "$email")"
otp="$(extract_otp "$otp_log")"
if [[ -z "$otp" ]]; then
  fail "could not extract password reset otp from auth logs"
fi

request POST "$AUTH_BASE/auth/password-reset/verify" 200 "$(jq -nc --arg email "$email" --arg otp "$otp" '{email:$email, otp:$otp}')" -H "x-api-key: $MOBILE_API_KEY"
reset_token="$(json_value '.resetToken')"
pass "POST /auth/password-reset/verify"

request PUT "$AUTH_BASE/auth/password-reset/confirm" 200 "$(jq -nc --arg new "$reset_password" --arg resetToken "$reset_token" '{newPassword:$new, resetToken:$resetToken}')" -H "x-api-key: $MOBILE_API_KEY"
pass "PUT /auth/password-reset/confirm"

request POST "$AUTH_BASE/auth/request-password-reset" 200 "$(jq -nc --arg email "$email" '{email:$email, lang:"es"}')" -H "x-api-key: $MOBILE_API_KEY"
sleep 2
otp_log="$(latest_auth_log_for_email "$email")"
otp="$(extract_otp "$otp_log")"
if [[ -z "$otp" ]]; then
  fail "could not extract second password reset otp from auth logs"
fi

request POST "$AUTH_BASE/auth/verify-password-otp" 200 "$(jq -nc --arg email "$email" --arg otp "$otp" '{email:$email, otp:$otp}')" -H "x-api-key: $MOBILE_API_KEY"
reset_token="$(json_value '.resetToken')"
pass "POST /auth/verify-password-otp"

request POST "$AUTH_BASE/auth/reset-password" 200 "$(jq -nc --arg new "$final_password" --arg resetToken "$reset_token" '{newPassword:$new, resetToken:$resetToken}')" -H "x-api-key: $MOBILE_API_KEY"
pass "POST /auth/reset-password"

request POST "$AUTH_BASE/auth/signin" 200 "$(jq -nc --arg email "$email" --arg password "$final_password" '{email:$email, password:$password}')" -H "x-api-key: $MOBILE_API_KEY"
access_token="$(json_value '.token')"
refresh_token="$(json_value '.refreshToken')"
set_auth_headers
pass "POST /auth/signin after reset"

request DELETE "$AUTH_BASE/auth/sessions/$session_id" 200 "" "${auth_headers[@]}"
pass "DELETE /auth/sessions/{sessionId}"

request POST "$AUTH_BASE/auth/logout" 200 "$(jq -nc --arg refreshToken "$refresh_token" '{refreshToken:$refreshToken}')" -H "x-api-key: $MOBILE_API_KEY"
pass "POST /auth/logout"

request POST "$AUTH_BASE/auth/signin" 200 "$(jq -nc --arg email "$email" --arg password "$final_password" '{email:$email, password:$password}')" -H "x-api-key: $MOBILE_API_KEY"
access_token="$(json_value '.token')"
refresh_token="$(json_value '.refreshToken')"
set_auth_headers
pass "POST /auth/signin for cleanup"

request DELETE "$AUTH_BASE/auth/logout?refreshToken=$refresh_token" 200 "" -H "x-api-key: $MOBILE_API_KEY"
pass "DELETE /auth/logout"

request POST "$AUTH_BASE/auth/signin" 200 "$(jq -nc --arg email "$email" --arg password "$final_password" '{email:$email, password:$password}')" -H "x-api-key: $MOBILE_API_KEY"
access_token="$(json_value '.token')"
set_auth_headers
pass "POST /auth/signin final"

request DELETE "$AUTH_BASE/auth/sessions" 200 "" "${auth_headers[@]}"
pass "DELETE /auth/sessions"

smoke_ip="198.51.$((RANDOM % 255)).$((RANDOM % 255))"
request POST "$AUTH_BASE/auth/signin" 200 "$(jq -nc --arg email "$email" --arg password "$final_password" '{email:$email, password:$password}')" -H "x-api-key: $MOBILE_API_KEY"
access_token="$(json_value '.token')"
set_auth_headers
pass "POST /auth/signin for delete-me"

request DELETE "$AUTH_BASE/auth/me" 200 "" "${auth_headers[@]}"
pass "DELETE /auth/me"

printf 'Smoke endpoints completed for %s\n' "$email"
