import { authDb, legacyDb, userDb } from "./db.js";
import { config } from "./config.js";
import { logger } from "./logger.js";
import { getServicesUserProjection, upsertLegacyUser } from "./projections/legacyUsers.js";
import { backfillLegacySocialFromServices, backfillSocialFromLegacy } from "./projections/socialUsers.js";

async function availableUsernameForUserService(userId: string, username: string | null): Promise<string | null> {
  if (!username) return null;

  const existing = await userDb.query<{ id: string }>(
    "select id from users where lower(username) = lower($1) and id <> $2 limit 1",
    [username, userId],
  );

  if (existing.rows.length === 0) return username;

  logger.warn("dropping duplicate username during services backfill", {
    userId,
    username,
    conflictingUserId: existing.rows[0]?.id,
  });
  return null;
}

export async function backfillLegacyFromServices(): Promise<void> {
  let offset = 0;
  let total = 0;

  while (true) {
    const result = await userDb.query<{ id: string }>(
      "select id from users order by created_at asc limit $1 offset $2",
      [config.backfillBatchSize, offset],
    );
    if (result.rows.length === 0) break;

    for (const row of result.rows) {
      const projection = await getServicesUserProjection(row.id);
      if (projection) {
        await upsertLegacyUser(projection);
        total += 1;
      }
    }

    offset += result.rows.length;
    logger.info("backfill legacy-from-services batch complete", { offset, total });
  }

  logger.info("backfill legacy-from-services complete", { total });
  await backfillLegacySocialFromServices();
}

export async function backfillServicesFromLegacy(): Promise<void> {
  let offset = 0;
  let total = 0;

  while (true) {
    const result = await legacyDb.query(
      `
        select id, name, username, email, gender, password_hash, type, phone, avatar_url,
               is_email_verified, is_active, is_private, deleted_at, lang,
               accepted_terms_at, accepted_terms_version, accepted_data_commercialization_at,
               created_at, updated_at
        from users
        order by created_at asc
        limit $1 offset $2
      `,
      [config.backfillBatchSize, offset],
    );
    if (result.rows.length === 0) break;

    for (const user of result.rows) {
      await authDb.query(
        `
          insert into authentications (
            id, email, password_hash, role, is_email_verified, is_active,
            terms_version_accepted, terms_accepted_at, data_consent_accepted_at,
            deleted_at, created_at, updated_at
          )
          values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
          on conflict (id) do update set
            email = excluded.email,
            role = excluded.role,
            is_email_verified = excluded.is_email_verified,
            is_active = excluded.is_active,
            terms_version_accepted = excluded.terms_version_accepted,
            terms_accepted_at = excluded.terms_accepted_at,
            data_consent_accepted_at = excluded.data_consent_accepted_at,
            deleted_at = excluded.deleted_at,
            updated_at = excluded.updated_at
        `,
        [
          user.id,
          user.email,
          user.password_hash,
          user.type,
          user.is_email_verified,
          user.is_active,
          user.accepted_terms_version,
          user.accepted_terms_at,
          user.accepted_data_commercialization_at,
          user.deleted_at,
          user.created_at,
          user.updated_at,
        ],
      );

      const status = user.deleted_at ? "DELETED" : user.is_active ? "VISIBLE" : "SUSPENDED";
      const username = await availableUsernameForUserService(user.id, user.username);
      await userDb.query(
        `
          insert into users (
            id, name, username, email, email_verified, gender, phone_number, avatar_url,
            username_changed_at, username_prev_changed_at, lang,
            private_profile, status, created_at, updated_at
          )
          values ($1, $2, $3, $4, $5, $6, $7, $8, null, null, $9, $10, $11, $12, $13)
          on conflict (id) do update set
            name = excluded.name,
            username = excluded.username,
            email = excluded.email,
            email_verified = excluded.email_verified,
            gender = excluded.gender,
            phone_number = excluded.phone_number,
            avatar_url = excluded.avatar_url,
            lang = excluded.lang,
            private_profile = excluded.private_profile,
            status = excluded.status,
            updated_at = excluded.updated_at
        `,
        [
          user.id,
          user.name,
          username,
          user.email,
          user.is_email_verified,
          user.gender,
          user.phone,
          user.avatar_url,
          user.lang ?? "es",
          user.is_private,
          status,
          user.created_at,
          user.updated_at,
        ],
      );

      total += 1;
    }

    offset += result.rows.length;
    logger.info("backfill services-from-legacy batch complete", { offset, total });
  }

  logger.info("backfill services-from-legacy complete", { total });
  await backfillSocialFromLegacy();
}
