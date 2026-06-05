export type AuthRole = "MEMBER" | "ADMIN" | "SUPER_ADMIN";
export type UserStatus = "VISIBLE" | "HIDDEN" | "SUSPENDED" | "DELETED";

export interface AuthEventMessage<TPayload = unknown> {
  eventId: string;
  eventType: string;
  aggregateId: string;
  source: string;
  payload: TPayload;
  publishedAt: string;
}

export interface UserRegisteredPayload {
  id: string;
  email: string;
  name: string;
  username?: string | null;
  role?: AuthRole | null;
  acceptedTermsVersion?: string | null;
  acceptedDataCommercialization?: boolean | null;
  occurredAt?: string | null;
}

export interface UserDeletedPayload {
  id: string;
  email: string;
  occurredAt?: string | null;
}

export interface UserProfileCreatedPayload {
  id: string;
  email: string;
  name: string;
  username?: string | null;
  privateProfile?: boolean | null;
  status?: UserStatus | null;
  occurredAt?: string | null;
}

export interface LegacyUserProjection {
  id: string;
  email: string;
  name: string;
  username: string | null;
  gender: string | null;
  phone: string | null;
  avatarUrl: string | null;
  isEmailVerified: boolean;
  isActive: boolean;
  isPrivate: boolean;
  deletedAt: Date | null;
  lang: string;
  type: AuthRole;
  acceptedTermsVersion: string | null;
  acceptedTermsAt: Date | null;
  acceptedDataCommercializationAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
}
