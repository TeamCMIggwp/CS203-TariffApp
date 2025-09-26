import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { randomUUID } from "crypto";
import argon2 from "argon2";
import { getDBConnection } from "@/lib/db";
import { signAccessToken } from "@/lib/auth";

export async function POST(req: Request) {
  try {
    const { email, password } = await req.json();
    if (!email || !password) {
      return NextResponse.json({ message: "Missing credentials" }, { status: 400 });
    }

    const conn = await getDBConnection();
    const [rows] = await conn.execute(
      `SELECT u.id AS user_id, u.role, a.password_hash
       FROM users u
       JOIN accounts a ON u.id = a.user_id
       WHERE u.email = ? AND a.provider = 'credentials'`,
      [email]
    );
    const user = (rows as any[])[0];
    if (!user?.password_hash) {
      await conn.end();
      return NextResponse.json({ message: "Invalid email or password" }, { status: 401 });
    }

    const ok = await argon2.verify(user.password_hash, password);
    if (!ok) {
      await conn.end();
      return NextResponse.json({ message: "Invalid email or password" }, { status: 401 });
    }

    // Create opaque refresh token (session)
    const sessionId = randomUUID();
    const expiresAt = new Date(Date.now() + Number(process.env.REFRESH_TOKEN_TTL_SECONDS || 604800) * 1000);
    const ip = req.headers.get("x-forwarded-for") || null;
    const userAgent = req.headers.get("user-agent") || null;

    await conn.execute(
      `INSERT INTO sessions (id, user_id, expires_at, ip, user_agent)
       VALUES (?, ?, ?, ?, ?)`,
      [sessionId, user.user_id, expiresAt, ip, userAgent]
    );
    await conn.end();

    // Short-lived access token (JWT)
    const accessToken = await signAccessToken({ userId: user.user_id, role: user.role });

    // Set refresh token cookie
    cookies().set("refresh_token", sessionId, {
      httpOnly: true,
      secure: true,
      sameSite: "lax",
      path: "/",
      maxAge: Number(process.env.REFRESH_TOKEN_TTL_SECONDS || 604800),
    });

    return NextResponse.json({ accessToken });
  } catch (e) {
    console.error(e);
    return NextResponse.json({ message: "Login failed" }, { status: 500 });
  }
}