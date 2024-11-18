"use server";

import { LOGIN_COOKIE_KEY } from "@/lib/constants";
import { cookies } from "next/headers";

export async function getAuthToken() {
  const cookiez = await cookies();
  const loginToken = cookiez.has(LOGIN_COOKIE_KEY)
    ? cookiez.get(LOGIN_COOKIE_KEY)?.value
    : null;
  return process.env.SESAME_USER_TOKEN ?? loginToken;
}

export async function authHeaders(): Promise<HeadersInit> {
  const token = await getAuthToken();
  if (!token) return {};
  return {
    Authorization: `Bearer ${token}`,
  };
}
