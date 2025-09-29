import { NextResponse } from "next/server";
import { getDBConnection } from "../../../lib/db";
import argon2 from "argon2";
import { randomUUID } from "crypto";

export async function POST(req: Request) {
  try {
    const { name, email, country, password } = await req.json();

    if (!name || !email || !country || !password) {
      return NextResponse.json({ message: "Missing required fields" }, { status: 400 });
    }

    console.log("Signup request received:", { name, email, country });

    const hash = await argon2.hash(password, {
      type: argon2.argon2id,
      memoryCost: 2 ** 16,
      timeCost: 3,
      parallelism: 1,
    });

    const conn = await getDBConnection();

    // Check for existing user
    const [existing] = await conn.execute("SELECT id FROM users WHERE email = ?", [email]);
    if ((existing as any[]).length > 0) {
      await conn.end();
      return NextResponse.json({ message: "Email already registered" }, { status: 409 });
    }

    // Generate UUID manually
    const userId = randomUUID();

    // Insert into users (with role defaulting to 'user')
    await conn.execute(
      "INSERT INTO users (id, email, name, country_code, role) VALUES (?, ?, ?, ?, ?)",
      [userId, email, name, country, "user"]
    );

    // Insert into accounts
    await conn.execute(
      `INSERT INTO accounts 
        (id, user_id, provider, provider_account_id, password_hash, password_algorithm) 
       VALUES (UUID(), ?, 'credentials', ?, ?, 'argon2id')`,
      [userId, email, hash]
    );

    await conn.end();

    console.log(`✅ User ${email} signed up successfully`);
    return NextResponse.json({ message: "Signup successful" });
  } catch (err: any) {
    console.error("❌ Signup error:", err.message, err.stack);
    return NextResponse.json({ message: "Signup failed", error: err.message }, { status: 500 });
  }
}