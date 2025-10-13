import { NextResponse } from "next/server";

export async function GET() {
  const secret = process.env.JWT_SECRET || "";
  return NextResponse.json({
    hasSecret: Boolean(secret),
    secretLen: secret.length,
    issuer: process.env.JWT_ISSUER || null,
    audience: process.env.JWT_AUDIENCE || null,
    backendUrl: process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || null,
  });
}
