import { NextResponse } from "next/server";

export async function POST(req: Request) {
  try {
    const { accessToken, ttl } = (await req.json()) ?? {};
    if (!accessToken || typeof accessToken !== "string") {
      return NextResponse.json({ message: "accessToken required" }, { status: 400 });
    }
    const ttlSeconds = Number(ttl) || Number(process.env.ACCESS_TOKEN_TTL_SECONDS) || 900;
    const res = NextResponse.json({ ok: true });
    // HttpOnly cookie so it is not available to JS, but middleware can read it
    res.cookies.set("access_token", accessToken, {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: ttlSeconds,
    });
    return res;
  } catch (e) {
    return NextResponse.json({ message: "invalid request" }, { status: 400 });
  }
}
