import { NextResponse } from "next/server";
import { getDBConnection } from "../../../lib/db";
import argon2 from "argon2";

export async function POST(req: Request) {
  try {
    const { email, password } = await req.json();

    if (!email || !password) {
      return NextResponse.json({ message: "Missing credentials" }, { status: 400 });
    }

    const conn = await getDBConnection();

    const [rows] = await conn.execute(
      "SELECT users.id, accounts.password_hash FROM users JOIN accounts ON users.id = accounts.user_id WHERE users.email = ?",
      [email]
    );

    await conn.end();

    const user = (rows as any[])[0];
    if (!user) {
      return NextResponse.json({ message: "Invalid email or password" }, { status: 401 });
    }

    const isValid = await argon2.verify(user.password_hash, password);
    if (!isValid) {
      return NextResponse.json({ message: "Invalid email or password" }, { status: 401 });
    }

    // TODO: Set session or return token
    return NextResponse.json({ message: "Login successful", userId: user.id });
  } catch (err) {
    console.error("Login error:", err);
    return NextResponse.json({ message: "Login failed" }, { status: 500 });
  }
}