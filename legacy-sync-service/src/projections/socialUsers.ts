import { legacyDb, socialDb } from "../db.js";
import { config } from "../config.js";
import { logger } from "../logger.js";

export async function backfillSocialUsersFromLegacy(): Promise<number> {
  let offset = 0;
  let total = 0;

  while (true) {
    const result = await legacyDb.query(
      `
        select id, name, username, avatar_url, is_private, is_active,
               deleted_at, created_at, updated_at
        from users
        order by created_at asc
        limit $1 offset $2
      `,
      [config.backfillBatchSize, offset],
    );
    if (result.rows.length === 0) break;

    for (const user of result.rows) {
      const active = !user.deleted_at && user.is_active;
      await socialDb.query(
        `
          insert into social_users (
            user_id, name, username, avatar_url, private_profile, active,
            deleted_at, source_updated_at, synced_at, created_at, updated_at
          )
          values ($1, $2, $3, $4, $5, $6, $7, $8, now(), $9, $10)
          on conflict (user_id) do update set
            name = excluded.name,
            username = excluded.username,
            avatar_url = excluded.avatar_url,
            private_profile = excluded.private_profile,
            active = excluded.active,
            deleted_at = excluded.deleted_at,
            source_updated_at = excluded.source_updated_at,
            synced_at = excluded.synced_at,
            updated_at = excluded.updated_at
        `,
        [
          user.id,
          user.name,
          user.username,
          user.avatar_url,
          user.is_private,
          active,
          user.deleted_at,
          user.updated_at,
          user.created_at,
          user.updated_at,
        ],
      );
      total += 1;
    }

    offset += result.rows.length;
    logger.info("backfill social users batch complete", { offset, total });
  }

  return total;
}

export async function backfillFollowsFromLegacy(): Promise<number> {
  let offset = 0;
  let total = 0;

  while (true) {
    const result = await legacyDb.query(
      `
        select id, follower_id, following_id, status, created_at
        from user_follows
        order by created_at asc
        limit $1 offset $2
      `,
      [config.backfillBatchSize, offset],
    );
    if (result.rows.length === 0) break;

    for (const follow of result.rows) {
      await socialDb.query(
        `
          insert into follows (
            id, follower_id, following_id, status, created_at, accepted_at, updated_at
          )
          values ($1, $2, $3, $4, $5, case when $4::varchar = 'ACCEPTED' then $5::timestamptz else null end, $5)
          on conflict (follower_id, following_id) do update set
            status = excluded.status,
            accepted_at = excluded.accepted_at,
            updated_at = excluded.updated_at
        `,
        [
          follow.id,
          follow.follower_id,
          follow.following_id,
          follow.status,
          follow.created_at,
        ],
      );
      total += 1;
    }

    offset += result.rows.length;
    logger.info("backfill follows batch complete", { offset, total });
  }

  return total;
}

export async function backfillBlocksFromLegacy(): Promise<number> {
  let offset = 0;
  let total = 0;

  while (true) {
    const result = await legacyDb.query(
      `
        select id, blocker_id, blocked_id, created_at
        from user_blocks
        order by created_at asc
        limit $1 offset $2
      `,
      [config.backfillBatchSize, offset],
    );
    if (result.rows.length === 0) break;

    for (const block of result.rows) {
      await socialDb.query(
        `
          insert into blocks (id, blocker_id, blocked_id, created_at)
          values ($1, $2, $3, $4)
          on conflict (blocker_id, blocked_id) do update set
            created_at = excluded.created_at
        `,
        [block.id, block.blocker_id, block.blocked_id, block.created_at],
      );
      total += 1;
    }

    offset += result.rows.length;
    logger.info("backfill blocks batch complete", { offset, total });
  }

  return total;
}

export async function rebuildSocialCounters(): Promise<number> {
  const result = await socialDb.query(
    `
      insert into social_counters (
        user_id, followers_count, following_count, pending_requests_count,
        blocked_count, created_at, updated_at
      )
      select
        user_id,
        (
          select count(*)::int
          from follows
          where following_id = social_users.user_id and status = 'ACCEPTED'
        ),
        (
          select count(*)::int
          from follows
          where follower_id = social_users.user_id and status = 'ACCEPTED'
        ),
        (
          select count(*)::int
          from follows
          where following_id = social_users.user_id and status = 'PENDING'
        ),
        (
          select count(*)::int
          from blocks
          where blocker_id = social_users.user_id
        ),
        now(),
        now()
      from social_users
      on conflict (user_id) do update set
        followers_count = excluded.followers_count,
        following_count = excluded.following_count,
        pending_requests_count = excluded.pending_requests_count,
        blocked_count = excluded.blocked_count,
        updated_at = excluded.updated_at
    `,
  );

  return result.rowCount ?? 0;
}

export async function backfillSocialFromLegacy(): Promise<void> {
  const users = await backfillSocialUsersFromLegacy();
  const follows = await backfillFollowsFromLegacy();
  const blocks = await backfillBlocksFromLegacy();
  const counters = await rebuildSocialCounters();

  logger.info("backfill social-from-legacy complete", {
    users,
    follows,
    blocks,
    counters,
  });
}

export async function backfillLegacySocialFromServices(): Promise<void> {
  const follows = await backfillLegacyFollowsFromSocial();
  const blocks = await backfillLegacyBlocksFromSocial();

  logger.info("backfill legacy social-from-services complete", {
    follows,
    blocks,
  });
}

async function backfillLegacyFollowsFromSocial(): Promise<number> {
  let offset = 0;
  let total = 0;

  while (true) {
    const result = await socialDb.query(
      `
        select id, follower_id, following_id, status, created_at
        from follows
        order by created_at asc
        limit $1 offset $2
      `,
      [config.backfillBatchSize, offset],
    );
    if (result.rows.length === 0) break;

    for (const follow of result.rows) {
      await legacyDb.query(
        `
          insert into user_follows (id, follower_id, following_id, status, created_at)
          values ($1, $2, $3, $4, $5)
          on conflict (follower_id, following_id) do update set
            status = excluded.status,
            created_at = excluded.created_at
        `,
        [
          follow.id,
          follow.follower_id,
          follow.following_id,
          follow.status,
          follow.created_at,
        ],
      );
      total += 1;
    }

    offset += result.rows.length;
    logger.info("backfill legacy follows batch complete", { offset, total });
  }

  return total;
}

async function backfillLegacyBlocksFromSocial(): Promise<number> {
  let offset = 0;
  let total = 0;

  while (true) {
    const result = await socialDb.query(
      `
        select id, blocker_id, blocked_id, created_at
        from blocks
        order by created_at asc
        limit $1 offset $2
      `,
      [config.backfillBatchSize, offset],
    );
    if (result.rows.length === 0) break;

    for (const block of result.rows) {
      await legacyDb.query(
        `
          insert into user_blocks (id, blocker_id, blocked_id, created_at)
          values ($1, $2, $3, $4)
          on conflict (blocker_id, blocked_id) do update set
            created_at = excluded.created_at
        `,
        [block.id, block.blocker_id, block.blocked_id, block.created_at],
      );
      total += 1;
    }

    offset += result.rows.length;
    logger.info("backfill legacy blocks batch complete", { offset, total });
  }

  return total;
}
