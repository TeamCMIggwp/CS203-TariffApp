import { NextResponse } from "next/server";

const BACKEND_URL = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

export async function POST() {
  // Call backend to clear refresh cookie, ignore result
  try {
    await fetch(`${BACKEND_URL}/auth/logout`, { method: "GET", credentials: "include" });
  } catch {}
  const res = NextResponse.json({ ok: true });
  // Clear frontend access token cookie
  res.cookies.set("access_token", "", { httpOnly: true, path: "/", maxAge: 0, sameSite: "lax", secure: process.env.NODE_ENV === "production" });
  return res;
}
