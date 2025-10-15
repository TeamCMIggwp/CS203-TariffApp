import { NextResponse } from "next/server";
import { decodeJwt, type JWTPayload } from "jose";

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

export async function GET(req: Request) {
  const auth = buildAuthHeader(req);
  if (!auth) return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
  const backendEnv = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL;
  const targetMe = backendEnv
    ? `${backendEnv}/auth/me`
    : new URL('/backend/auth/me', req.url).toString();
  try {
    const r = await fetch(targetMe, { headers: { authorization: auth }, cache: "no-store" });
    if (r.ok) {
      const data = await r.json().catch(() => ({}));
      return NextResponse.json(data, { status: r.status });
    }
    // Fallback: if backend endpoint missing or returns 404/500 with 'No static resource', try to decode token for minimal profile
    const token = auth.startsWith("Bearer ") ? auth.slice(7) : undefined;
    if (token) {
      try {
        const payload = decodeJwt(token) as JWTPayload & Record<string, unknown>;
        const email = (payload.email as string) || (payload.preferred_username as string) || (payload.sub as string) || undefined;
        const name = (payload.name as string) || (payload.given_name as string) || undefined;
        if (email || name) {
          return NextResponse.json({ email, name, source: "token" }, { status: 200 });
        }
      } catch {}
    }
    // Do not propagate 5xx to the browser; return 200 with an error payload to keep UI flow smooth
  type MessagePayload = { message: string } | { message?: string };
  let payload: MessagePayload | undefined = { message: "Profile unavailable" };
    try {
      const ct = r.headers.get("content-type") || "";
      if (ct.includes("application/json")) {
  payload = (await r.json()) as MessagePayload;
      } else {
        const text = await r.text();
  payload = { message: text || (payload?.message ?? "Profile unavailable") };
      }
    } catch {}
  return NextResponse.json((payload as MessagePayload) ?? { message: "Profile unavailable" }, { status: 200 });
  } catch {
    return NextResponse.json({ message: "Profile service unavailable" }, { status: 502 });
  }
}

export async function PUT(req: Request) {
  const auth = buildAuthHeader(req);
  if (!auth) return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const backendEnv = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL;
  const targetProfile = backendEnv
    ? `${backendEnv}/auth/profile`
    : new URL('/backend/auth/profile', req.url).toString();
  try {
    const r = await fetch(targetProfile, {
      method: "PUT",
      headers: { "content-type": "application/json", authorization: auth },
      body: JSON.stringify({ name: body?.name, email: body?.email }),
    });
    // Some servers respond with 404/405 or HTML/text body like "No static resource auth/profile"
    if (!r.ok) {
      const ct = r.headers.get("content-type") || "";
      let payload: { message?: string } | undefined = undefined;
      if (ct.includes("application/json")) {
        payload = (await r.json().catch(() => undefined)) as { message?: string } | undefined;
      } else {
        const text = await r.text().catch(() => "");
        if (text && /no static resource/i.test(text)) {
          return NextResponse.json({ message: "Profile update not supported by backend" }, { status: 501 });
        }
        payload = text ? { message: text } : undefined;
      }
      if (r.status === 404 || r.status === 405) {
        return NextResponse.json({ message: "Profile update not supported by backend" }, { status: 501 });
      }
      // For other non-OK statuses, bubble up the message if present
      return NextResponse.json(payload || { message: "Profile update failed" }, { status: r.status });
    }
    const data = await r.json().catch(() => ({}));
    return NextResponse.json(data, { status: r.status });
  } catch {
    return NextResponse.json({ message: "Profile update service unavailable" }, { status: 502 });
  }
}
