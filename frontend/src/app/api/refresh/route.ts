import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { randomUUID } from "crypto";
import { getDBConnection } from "@/lib/db";
import { signAccessToken } from "@/lib/auth";

export async function POST() {
  const refreshToken = cookies().get("refresh_token")?.value;
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
    cookies().delete("refresh_token");
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

  cookies().set("refresh_token", newSessionId, {
    httpOnly: true,
    secure: true,
    sameSite: "lax",
    path: "/",
    maxAge: Number(process.env.REFRESH_TOKEN_TTL_SECONDS || 604800),
  });

  return NextResponse.json({ accessToken });
}