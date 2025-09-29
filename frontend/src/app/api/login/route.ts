import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { randomUUID } from "crypto";
import { accountsDb } from "@/lib/db";
import argon2 from "argon2";
import { signAccessToken } from "@/lib/auth";

export async function POST(req: Request) {
  try {
    const { email, password } = await req.json();
    if (!email || !password) {
      return NextResponse.json({ message: "Missing credentials" }, { status: 400 });
    }

    const conn = await accountsDb.getConnection();

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

    await conn.release();

    type UserRow = {
      id: string;
      password_hash: string;
    };

    const user = (rows as UserRow[])[0];
    if (!user) {
      return NextResponse.json({ message: "Invalid email or password" }, { status: 401 });
    }

    // Set refresh token cookie
    const cookieStore = await cookies();
    cookieStore.set("refresh_token", sessionId, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/",
      maxAge: 60 * 60 * 24 * 7,
    });

    return NextResponse.json({ accessToken });
  } catch (e) {
    console.error(e);
    return NextResponse.json({ message: "Login failed" }, { status: 500 });
  }
}