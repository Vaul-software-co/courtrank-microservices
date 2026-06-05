import { backfillLegacyFromServices, backfillServicesFromLegacy } from "./backfill.js";
import { startConsumer } from "./consumer.js";
import { closeDbs } from "./db.js";
import { logger } from "./logger.js";

const command = process.argv[2] ?? "consume";

async function main(): Promise<void> {
  if (command === "consume") {
    await startConsumer();
    return;
  }

  if (command === "backfill:legacy-from-services") {
    await backfillLegacyFromServices();
    return;
  }

  if (command === "backfill:services-from-legacy") {
    await backfillServicesFromLegacy();
    return;
  }

  throw new Error(`Unknown command: ${command}`);
}

main()
  .catch((error) => {
    logger.error("legacy sync failed", {
      command,
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined,
    });
    process.exitCode = 1;
  })
  .finally(async () => {
    if (command !== "consume") {
      await closeDbs();
    }
  });
