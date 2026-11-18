import { useEffect, useState } from "react";

export type Role = "student" | "organiser" | "teacher";
export interface User {
  userId: string;
  universityId: string;
  displayName: string;
  role: Role;
}
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
  myRegistration?: { registrationId: string; status: string } | null;
  registrationLink?: string | null;
  qrToken?: string | null;
  feedback?: { reason: string } | null;
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
  proposed: Fields;
  previous: Fields | null;
  previousFeedback?: { reason: string } | null;
  changedFields: (keyof Fields)[];
  canApprove: boolean;
  blockReason?: string;
  decision?: { reviewerName: string; reason: string; decidedAt: string };
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
  message: string;
  createdAt: string;
  activityId: string;
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
    credentials: "same-origin",
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
export function useData<T>(path: string, refresh = 0) {
  const [state, set] = useState<{ key: string; data?: T; error?: string }>({
    key: "",
  });
  const key = path + "#" + refresh;
  useEffect(() => {
    const c = new AbortController();
    api<T>(path, "GET", undefined, c.signal)
      .then((data) => set({ key, data }))
      .catch((e) => {
        if (!c.signal.aborted) set({ key, error: e.message });
      });
    return () => c.abort();
  }, [path, key]);
  return state.key === key ? state : { key };
}
