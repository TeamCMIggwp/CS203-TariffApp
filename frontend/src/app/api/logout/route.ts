import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { getDBConnection } from "@/lib/db";

export async function POST() {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get("refresh_token")?.value;
  if (refreshToken) {
    const conn = await getDBConnection();
    await conn.execute(`DELETE FROM sessions WHERE id = ?`, [refreshToken]);
    await conn.end();
    cookieStore.delete("refresh_token");
  }
  return NextResponse.json({ message: "Logged out" });
}