import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { randomUUID } from "crypto";
import { getDBConnection } from "@/lib/db";
import { signAccessToken } from "@/lib/auth";

export async function POST() {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get("refresh_token")?.value;
  if (!refreshToken) {
    return NextResponse.json({ message: "Missing refresh token" }, { status: 401 });
  }

  const conn = await getDBConnection();
  const [rows] = await conn.execute(
    `SELECT s.user_id, u.role, s.expires_at
     FROM sessions s
     JOIN users u ON u.id = s.user_id
     WHERE s.id = ?`,
    [refreshToken]
  );
  const session = (rows as any[])[0];

  if (!session || new Date(session.expires_at) <= new Date()) {
    await conn.end();
    cookieStore.delete("refresh_token");
    return NextResponse.json({ message: "Session expired" }, { status: 401 });
  }

  // Rotate refresh token
  const newSessionId = randomUUID();
  const newExpiresAt = new Date(Date.now() + Number(process.env.REFRESH_TOKEN_TTL_SECONDS || 604800) * 1000);

  await conn.execute(`DELETE FROM sessions WHERE id = ?`, [refreshToken]);
  await conn.execute(
    `INSERT INTO sessions (id, user_id, expires_at) VALUES (?, ?, ?)`,
    [newSessionId, session.user_id, newExpiresAt]
  );
  await conn.end();

  const accessToken = await signAccessToken({ userId: session.user_id, role: session.role });

  cookieStore.set("refresh_token", newSessionId, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: Number(process.env.REFRESH_TOKEN_TTL_SECONDS || 604800),
  });

  return NextResponse.json({ accessToken });
}

// Support GET for middleware redirect flow; rotate session and redirect back to original page.
export async function GET(req: Request) {
  const url = new URL(req.url);
  const returnTo = url.searchParams.get("returnTo") || "/";

  const cookieStore = await cookies();
  const refreshToken = cookieStore.get("refresh_token")?.value;
  if (!refreshToken) {
    // No session; go to login
    return NextResponse.redirect(new URL("/login", req.url));
  }

  const conn = await getDBConnection();
  const [rows] = await conn.execute(
    `SELECT s.user_id, u.role, s.expires_at
     FROM sessions s
     JOIN users u ON u.id = s.user_id
     WHERE s.id = ?`,
    [refreshToken]
  );
  const session = (rows as any[])[0];

  if (!session || new Date(session.expires_at) <= new Date()) {
    await conn.end();
    cookieStore.delete("refresh_token");
    return NextResponse.redirect(new URL("/login", req.url));
  }

  // Rotate refresh token
  const newSessionId = randomUUID();
  const newExpiresAt = new Date(Date.now() + Number(process.env.REFRESH_TOKEN_TTL_SECONDS || 604800) * 1000);

  await conn.execute(`DELETE FROM sessions WHERE id = ?`, [refreshToken]);
  await conn.execute(
    `INSERT INTO sessions (id, user_id, expires_at) VALUES (?, ?, ?)`,
    [newSessionId, session.user_id, newExpiresAt]
  );
  await conn.end();

  // Issue short-lived access token (if your client needs it via JSON, it's not used on redirect)
  await signAccessToken({ userId: session.user_id, role: session.role });

  cookieStore.set("refresh_token", newSessionId, {
    httpOnly: true,
    secure: true,
    sameSite: "lax",
    path: "/",
    maxAge: Number(process.env.REFRESH_TOKEN_TTL_SECONDS || 604800),
  });

  // Set a tiny flag so middleware allows next request through and clears it
  cookieStore.set("just_refreshed", "1", {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 5, // seconds
  });

  // Redirect back to original path; 303 ensures GET
  return NextResponse.redirect(new URL(returnTo, req.url), { status: 303 });
}