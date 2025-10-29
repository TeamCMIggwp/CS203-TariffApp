import { NextResponse } from "next/server";

function decodeJwtExp(token: string): number | null {
  try {
    const parts = token.split(".");
    if (parts.length < 2) return null;
    const payloadB64 = parts[1]
      .replace(/-/g, "+")
      .replace(/_/g, "/");
    const json = Buffer.from(payloadB64, "base64").toString("utf8");
    const payload = JSON.parse(json) as { exp?: number };
    if (typeof payload.exp === "number") return payload.exp;
    return null;
  } catch {
    return null;
  }
}

export async function GET(req: Request) {
  try {
    // Prefer Authorization header (middleware may inject it)
    const auth = (req.headers.get("authorization") || "").toString();
    let accessToken: string | undefined;
    if (auth.startsWith("Bearer ")) {
      accessToken = auth.split(" ")[1];
    } else {
      // Fallback to access_token cookie
      const cookieHeader = req.headers.get("cookie") || "";
      const match = cookieHeader.match(/(?:^|;\s*)access_token=([^;]+)/);
      if (match) {
        try {
          accessToken = decodeURIComponent(match[1]);
        } catch {
          accessToken = match[1];
        }
      }
    }

    if (!accessToken) {
      return NextResponse.json(
        { authenticated: false, exp: null, now: Math.floor(Date.now() / 1000), remainingSeconds: 0 },
        { status: 200 }
      );
    }

    const exp = decodeJwtExp(accessToken);
    const now = Math.floor(Date.now() / 1000);
    const remainingSeconds = exp ? Math.max(0, exp - now) : 0;
    return NextResponse.json({ authenticated: true, exp, now, remainingSeconds }, { status: 200 });
  } catch {
    return NextResponse.json(
      { authenticated: false, exp: null, now: Math.floor(Date.now() / 1000), remainingSeconds: 0 },
      { status: 200 }
    );
  }
}
