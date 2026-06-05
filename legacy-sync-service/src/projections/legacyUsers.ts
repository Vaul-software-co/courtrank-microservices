import { authDb, legacyDb, userDb } from "../db.js";
import { config } from "../config.js";
import type {
  AuthRole,
  LegacyUserProjection,
  UserDeletedPayload,
  UserProfileCreatedPayload,
  UserRegisteredPayload,
} from "../types.js";

function asDate(value: unknown, fallback = new Date()): Date {
  if (typeof value !== "string" || !value) return fallback;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? fallback : parsed;
}

function legacyType(role: AuthRole | null | undefined): AuthRole {
  return role ?? "MEMBER";
}

export async function upsertLegacyUser(user: LegacyUserProjection): Promise<void> {
  await legacyDb.query(
    `
      insert into users (
        id, name, username, email, gender, password_hash, type, phone,
        avatar_url, is_email_verified, is_active, is_private,
        created_at, updated_at, deleted_at, lang, notification_settings,
        accepted_terms_at, accepted_terms_version, accepted_data_commercialization_at
      )
      values (
        $1, $2, $3, $4, $5, $6, $7, $8,
        $9, $10, $11, $12,
        $13, $14, $15, $16, '{}'::jsonb,
        $17, $18, $19
      )
      on conflict (id) do update set
        name = excluded.name,
        username = excluded.username,
        email = excluded.email,
        gender = excluded.gender,
        type = excluded.type,
        phone = excluded.phone,
        avatar_url = excluded.avatar_url,
        is_email_verified = excluded.is_email_verified,
        is_active = excluded.is_active,
        is_private = excluded.is_private,
        updated_at = excluded.updated_at,
        deleted_at = excluded.deleted_at,
        lang = excluded.lang,
        accepted_terms_at = excluded.accepted_terms_at,
        accepted_terms_version = excluded.accepted_terms_version,
        accepted_data_commercialization_at = excluded.accepted_data_commercialization_at
    `,
    [
      user.id,
      user.name,
      user.username,
      user.email,
      user.gender,
      config.legacyPlaceholderPasswordHash,
      user.type,
      user.phone,
      user.avatarUrl,
      user.isEmailVerified,
      user.isActive,
      user.isPrivate,
      user.createdAt,
      user.updatedAt,
      user.deletedAt,
      user.lang,
      user.acceptedTermsAt,
      user.acceptedTermsVersion,
      user.acceptedDataCommercializationAt,
    ],
  );
}

export async function projectAuthRegisteredToLegacy(payload: UserRegisteredPayload): Promise<void> {
  const now = asDate(payload.occurredAt);
  await upsertLegacyUser({
    id: payload.id,
    email: payload.email,
    name: payload.name,
    username: payload.username ?? null,
    gender: null,
    phone: null,
    avatarUrl: null,
    isEmailVerified: false,
    isActive: true,
    isPrivate: false,
    deletedAt: null,
    lang: "es",
    type: legacyType(payload.role),
    acceptedTermsVersion: payload.acceptedTermsVersion ?? null,
    acceptedTermsAt: payload.acceptedTermsVersion ? now : null,
    acceptedDataCommercializationAt: payload.acceptedDataCommercialization ? now : null,
    createdAt: now,
    updatedAt: now,
  });
}

export async function projectAuthDeletedToLegacy(payload: UserDeletedPayload): Promise<void> {
  const deletedAt = asDate(payload.occurredAt);
  await legacyDb.query(
    `
      update users
      set deleted_at = $2, is_active = false, updated_at = $2
      where id = $1
    `,
    [payload.id, deletedAt],
  );
}

export async function projectUserProfileCreatedToLegacy(payload: UserProfileCreatedPayload): Promise<void> {
  const now = asDate(payload.occurredAt);
  await legacyDb.query(
    `
      insert into users (
        id, name, username, email, password_hash, type,
        is_email_verified, is_active, is_private,
        created_at, updated_at, deleted_at, lang, notification_settings
      )
      values ($1, $2, $3, $4, $5, 'MEMBER', false, $6, $7, $8, $8, $9, 'es', '{}'::jsonb)
      on conflict (id) do update set
        name = excluded.name,
        username = excluded.username,
        email = excluded.email,
        is_active = excluded.is_active,
        is_private = excluded.is_private,
        deleted_at = excluded.deleted_at,
        updated_at = excluded.updated_at
    `,
    [
      payload.id,
      payload.name,
      payload.username ?? null,
      payload.email,
      config.legacyPlaceholderPasswordHash,
      payload.status !== "DELETED" && payload.status !== "SUSPENDED",
      payload.privateProfile ?? false,
      now,
      payload.status === "DELETED" ? now : null,
    ],
  );
}

export async function getServicesUserProjection(id: string): Promise<LegacyUserProjection | null> {
  const [authResult, userResult] = await Promise.all([
    authDb.query(
      `
        select id, email, role, is_email_verified, is_active,
               terms_version_accepted, terms_accepted_at, data_consent_accepted_at,
               deleted_at, created_at, updated_at
        from authentications
        where id = $1
      `,
      [id],
    ),
    userDb.query(
      `
        select id, name, username, email, gender, phone_number, avatar_url,
               lang, private_profile, status, created_at, updated_at
        from users
        where id = $1
      `,
      [id],
    ),
  ]);

  const auth = authResult.rows[0];
  const profile = userResult.rows[0];
  if (!auth && !profile) return null;

  const createdAt = profile?.created_at ?? auth?.created_at ?? new Date();
  const updatedAt = profile?.updated_at ?? auth?.updated_at ?? new Date();
  const status = profile?.status ?? (auth?.deleted_at ? "DELETED" : auth?.is_active === false ? "SUSPENDED" : "VISIBLE");

  return {
    id,
    email: profile?.email ?? auth.email,
    name: profile?.name ?? profile?.email ?? auth.email,
    username: profile?.username ?? null,
    gender: profile?.gender ?? null,
    phone: profile?.phone_number ?? null,
    avatarUrl: profile?.avatar_url ?? null,
    isEmailVerified: auth?.is_email_verified ?? false,
    isActive: status === "VISIBLE" && (auth?.is_active ?? true),
    isPrivate: profile?.private_profile ?? false,
    deletedAt: auth?.deleted_at ?? (status === "DELETED" ? updatedAt : null),
    lang: profile?.lang ?? "es",
    type: legacyType(auth?.role),
    acceptedTermsVersion: auth?.terms_version_accepted ?? null,
    acceptedTermsAt: auth?.terms_accepted_at ?? null,
    acceptedDataCommercializationAt: auth?.data_consent_accepted_at ?? null,
    createdAt,
    updatedAt,
  };
}
