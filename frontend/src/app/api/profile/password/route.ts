import { NextResponse } from "next/server";

const BACKEND_URL = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

function buildAuthHeader(req: Request): string | undefined {
  const hdr = req.headers.get("authorization");
  if (hdr && hdr.startsWith("Bearer ")) return hdr;
  const cookieHeader = req.headers.get("cookie") || "";
  const m = cookieHeader.match(/(?:^|;\s*)access_token=([^;]+)/);
  if (m) {
    let token = m[1];
    try {
      token = decodeURIComponent(token);
    } catch {
      // ignore decode errors; use raw token
    }
    return `Bearer ${token}`;
  }
  return undefined;
}

export async function PUT(req: Request) {
  const auth = buildAuthHeader(req);
  if (!auth) return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  try {
    const r = await fetch(`${BACKEND_URL}/auth/password`, {
      method: "PUT",
      headers: { "content-type": "application/json", authorization: auth },
      body: JSON.stringify({ currentPassword: body?.currentPassword, newPassword: body?.newPassword }),
    });
    const data = await r.json().catch(() => ({}));
    return NextResponse.json(data, { status: r.status });
  } catch (e) {
    return NextResponse.json({ message: "Password service unavailable" }, { status: 502 });
  }
}
