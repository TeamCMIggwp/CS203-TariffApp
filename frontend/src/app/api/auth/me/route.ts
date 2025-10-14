import { NextResponse } from "next/server";

const BACKEND_URL = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

export async function GET(req: Request) {
  try {
    // Prefer Authorization header (set by middleware when available)
    const auth = (req.headers.get("authorization") || "").toString();
    let accessToken: string | undefined;
    if (auth.startsWith("Bearer ")) {
      accessToken = auth.split(" ")[1];
    } else {
      // Fallback to access_token cookie for cross-site deployments
      const cookieHeader = req.headers.get("cookie") || "";
      const match = cookieHeader.match(/(?:^|;\s*)access_token=([^;]+)/);
      if (match) accessToken = decodeURIComponent(match[1]);
    }

    if (!accessToken) {
      return NextResponse.json({ authenticated: false, roles: [] }, { status: 200 });
    }

    // Ask backend for user info; do not validate roles on the client
    const res = await fetch(`${BACKEND_URL}/auth/me`, {
      headers: { authorization: `Bearer ${accessToken}` },
      cache: "no-store",
    });
    if (!res.ok) return NextResponse.json({ authenticated: false, roles: [] }, { status: 200 });
    const me = await res.json();
    const roles = Array.isArray(me?.roles) ? me.roles : [];
    return NextResponse.json({ authenticated: true, roles }, { status: 200 });
  } catch (e) {
    return NextResponse.json({ authenticated: false, roles: [] }, { status: 200 });
  }
}
