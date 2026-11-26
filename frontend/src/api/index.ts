import { useEffect, useState } from "react";

export type Role = "student" | "organiser" | "teacher";
export interface User {
  userId: string;
  universityId: string;
  displayName: string;
  role: Role;
  roles: Role[];
}
export const hasRole = (user: User, role: Role) =>
  user.role === role || user.roles?.includes(role);
export interface Fields {
  title: string;
  description: string;
  startsAt: string;
  location: string;
  requirements: string;
  capacity: number;
  registrationDeadline: string;
  visibility: string;
}
export interface Activity extends Partial<Fields> {
  activityId: string;
  clubId: string;
  clubName: string;
  status: string;
  remaining: number;
  confirmedCount: number;
  reservedCount: number;
  registrationState: string;
  registrationClosesAt?: string | null;
  myRegistration?: { registrationId: string; status: string } | null;
  registrationLink?: string | null;
  qrToken?: string | null;
  feedback?: { reason: string; reviewerName: string; decidedAt: string } | null;
  pendingEdit?: { reviewId: string } | null;
  cancelReason?: string | null;
}
export interface Booking {
  registrationId: string;
  status: string;
  registeredAt: string;
  reminderAt: string | null;
  reservationDeadline?: string;
  activity: Activity;
}
export interface Review {
  reviewId: string;
  activityId: string;
  clubName: string;
  title: string;
  version: number;
  status: string;
  kind: string;
  submittedBy: string;
  submittedAt: string;
  reviewDeadline: string;
  isUrgent: boolean;
  proposed: Fields;
  previous: Fields | null;
  previousFeedback?: { reason: string } | null;
  changedFields: (keyof Fields)[];
  canApprove: boolean;
  decision?: {
    reviewerId: string | null;
    reviewerName: string;
    reason: string | null;
    decidedAt: string;
  } | null;
}
export interface Club {
  clubId: string;
  clubName: string;
}
export interface Participant {
  registrationId: string;
  studentName: string;
  universityId: string;
  status: string;
}
export interface Notice {
  noticeId: string;
  type: string;
  message: string;
  createdAt: string;
  activityId: string;
  activityTitle: string | null;
  registrationId: string | null;
  readAt: string | null;
}
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public fields: Record<string, string> = {},
  ) {
    super(message);
  }
}
export async function api<T>(
  path: string,
  method = "GET",
  body?: unknown,
  signal?: AbortSignal,
): Promise<T> {
  const res = await fetch("/api" + path, {
    method,
    signal,
    headers: body === undefined ? {} : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await res.json().catch(() => null);
  if (!res.ok)
    throw new ApiError(
      data?.message || "The request could not be completed.",
      res.status,
      data?.fields || {},
    );
  return data as T;
}
export function useData<T>(
  path: string,
  refresh = 0,
  pollMs = 0,
  retainOnRefresh = false,
) {
  const [state, set] = useState<{ key: string; data?: T; error?: string }>({
    key: "",
  });
  const key = retainOnRefresh ? path : path + "#" + refresh;
  useEffect(() => {
    const c = new AbortController();
    let timer: number;
    async function load() {
      try {
        const data = await api<T>(path, "GET", undefined, c.signal);
        if (!c.signal.aborted) set({ key, data });
      } catch (e) {
        if (!c.signal.aborted)
          set({
            key,
            error: e instanceof Error ? e.message : "Unable to load this page.",
          });
      } finally {
        if (pollMs && !c.signal.aborted)
          timer = window.setTimeout(load, pollMs);
      }
    }
    void load();
    return () => {
      c.abort();
      window.clearTimeout(timer);
    };
  }, [path, key, refresh, pollMs]);
  return state.key === key ? state : { key };
}
