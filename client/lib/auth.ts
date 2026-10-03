import { api, type User } from "@/lib/api";

export const authQueryKey = ["auth", "me"] as const;

export async function fetchCurrentUser(): Promise<User | null> {
  try {
    return await api.me();
  } catch {
    return null;
  }
}
