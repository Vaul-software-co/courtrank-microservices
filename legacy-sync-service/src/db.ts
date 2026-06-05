import pg from "pg";
import { config } from "./config.js";

const { Pool } = pg;

export const legacyDb = new Pool({ connectionString: config.legacyDatabaseUrl });
export const authDb = new Pool({ connectionString: config.authDatabaseUrl });
export const userDb = new Pool({ connectionString: config.userDatabaseUrl });

export async function closeDbs(): Promise<void> {
  await Promise.all([legacyDb.end(), authDb.end(), userDb.end()]);
}
