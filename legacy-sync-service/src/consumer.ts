import { Kafka } from "kafkajs";
import { config } from "./config.js";
import { logger } from "./logger.js";
import {
  getServicesUserProjection,
  projectAuthDeletedToLegacy,
  projectAuthRegisteredToLegacy,
  projectUserProfileCreatedToLegacy,
  upsertLegacyUser,
} from "./projections/legacyUsers.js";
import type {
  AuthEventMessage,
  UserDeletedPayload,
  UserProfileCreatedPayload,
  UserRegisteredPayload,
} from "./types.js";

function parseMessage(value: Buffer | null): AuthEventMessage {
  if (!value) throw new Error("Kafka message value is empty");
  return JSON.parse(value.toString("utf8")) as AuthEventMessage;
}

export async function startConsumer(): Promise<void> {
  const kafka = new Kafka({
    clientId: config.kafkaClientId,
    brokers: config.kafkaBrokers,
  });
  const consumer = kafka.consumer({ groupId: config.kafkaGroupId });

  await consumer.connect();
  await consumer.subscribe({ topic: config.authEventsTopic, fromBeginning: false });
  await consumer.subscribe({ topic: config.userEventsTopic, fromBeginning: false });

  logger.info("legacy sync consumer started", {
    topics: [config.authEventsTopic, config.userEventsTopic],
    groupId: config.kafkaGroupId,
  });

  await consumer.run({
    eachMessage: async ({ topic, partition, message }) => {
      const event = parseMessage(message.value);
      logger.info("processing event", {
        topic,
        partition,
        offset: message.offset,
        eventId: event.eventId,
        eventType: event.eventType,
        aggregateId: event.aggregateId,
      });

      if (topic === config.authEventsTopic) {
        if (event.eventType === "USER_REGISTERED" || event.eventType === "USER_RESTORED") {
          await projectAuthRegisteredToLegacy(event.payload as UserRegisteredPayload);
          return;
        }

        if (event.eventType === "USER_DELETED") {
          await projectAuthDeletedToLegacy(event.payload as UserDeletedPayload);
          return;
        }
      }

      if (topic === config.userEventsTopic && event.eventType === "USER_PROFILE_CREATED") {
        await projectUserProfileCreatedToLegacy(event.payload as UserProfileCreatedPayload);
        return;
      }

      if (
        topic === config.userEventsTopic &&
        (event.eventType === "USER_PROFILE_UPDATED" ||
          event.eventType === "USER_PROFILE_RESTORED" ||
          event.eventType === "USER_PROFILE_BECAME_PUBLIC")
      ) {
        const projection = await getServicesUserProjection(event.aggregateId);
        if (projection) {
          await upsertLegacyUser(projection);
        }
        return;
      }

      if (topic === config.userEventsTopic && event.eventType === "USER_PROFILE_DELETED") {
        await projectAuthDeletedToLegacy({
          id: event.aggregateId,
          email: "",
          occurredAt: event.publishedAt,
        });
        return;
      }

      logger.warn("ignored event", {
        topic,
        eventId: event.eventId,
        eventType: event.eventType,
      });
    },
  });
}
