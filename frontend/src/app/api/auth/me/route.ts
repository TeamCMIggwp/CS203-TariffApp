import { NextResponse } from "next/server";
import { decodeJwt, type JWTPayload } from "jose";

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

    // Best-effort roles from token (decode only; UI hint, not an auth gate)
    const rolesFromPayload = (payload: JWTPayload | undefined): string[] => {
      if (!payload) return [];
      const read = (k: string) => payload[k as keyof JWTPayload] as unknown;
      const raw: unknown[] = [read("role"), read("roles"), read("authorities")];
      const out: string[] = [];
      for (const v of raw) {
        if (typeof v === "string") out.push(v);
        if (Array.isArray(v)) {
          for (const i of v) {
            if (typeof i === "string") out.push(i);
            else if (i && typeof i === "object") {
              // handle { authority: "ROLE_ADMIN" } or similar
              const auth = (i as any).authority ?? (i as any).role ?? (i as any).name;
              if (typeof auth === "string") out.push(auth);
            }
          }
        }
      }
      return out
        .map((s) => String(s).toLowerCase())
        .flatMap((s) => {
          const list: string[] = [];
          if (s.includes("admin")) list.push("admin");
          if (s.includes("user")) list.push("user");
          return list.length ? list : [s];
        })
        .filter((v, idx, arr) => arr.indexOf(v) === idx);
    };

    let tokenRoles: string[] = [];
    try {
      tokenRoles = rolesFromPayload(decodeJwt(accessToken));
    } catch {}

    // Ask backend for user info; do not validate roles on the client
    const res = await fetch(`${BACKEND_URL}/auth/me`, {
      headers: { authorization: `Bearer ${accessToken}` },
      cache: "no-store",
    });

    if (!res.ok) {
      // Fall back to decoded roles if backend cannot be reached or denies
      return NextResponse.json({ authenticated: true, roles: tokenRoles }, { status: 200 });
    }

    const me = await res.json();

    const normalizeRoles = (src: any): string[] => {
      const out: string[] = [];
      if (!src) return out;
      const pushVal = (val: any) => {
        if (typeof val === "string") out.push(val);
        else if (val && typeof val === "object") {
          const a = val.authority ?? val.role ?? val.name;
          if (typeof a === "string") out.push(a);
        }
      };
      if (typeof src.role === "string") pushVal(src.role);
      if (Array.isArray(src.roles)) src.roles.forEach(pushVal);
      if (Array.isArray(src.authorities)) src.authorities.forEach(pushVal);
      return out
        .map((s) => String(s).toLowerCase())
        .flatMap((s) => {
          const list: string[] = [];
          if (s.includes("admin")) list.push("admin");
          if (s.includes("user")) list.push("user");
          return list.length ? list : [s];
        })
        .filter((v, i, arr) => arr.indexOf(v) === i);
    };

    const roles = normalizeRoles(me);
    // Merge with token-derived roles to be forgiving about backend shape
    const merged = Array.from(new Set([...(roles || []), ...(tokenRoles || [])]));
    return NextResponse.json({ authenticated: true, roles: merged }, { status: 200 });
  } catch (e) {
    // Do not fail hard; if we had a token, try to show at least token-derived roles
    try {
      const authHeader = (req.headers.get("authorization") || "").toString();
      const token = authHeader.startsWith("Bearer ") ? authHeader.split(" ")[1] : undefined;
      const roles = token ? rolesFromPayloadSafe(token) : [];
      return NextResponse.json({ authenticated: Boolean(token), roles }, { status: 200 });
    } catch {
      return NextResponse.json({ authenticated: false, roles: [] }, { status: 200 });
    }
  }
}

function rolesFromPayloadSafe(token: string): string[] {
  try {
    const payload = decodeJwt(token);
    const read = (k: string) => (payload as any)[k];
    const raw: unknown[] = [read("role"), read("roles"), read("authorities")];
    const out: string[] = [];
    for (const v of raw) {
      if (typeof v === "string") out.push(v);
      if (Array.isArray(v)) {
        for (const i of v) {
          if (typeof i === "string") out.push(i);
          else if (i && typeof i === "object") {
            const auth = (i as any).authority ?? (i as any).role ?? (i as any).name;
            if (typeof auth === "string") out.push(auth);
          }
        }
      }
    }
    return out
      .map((s) => String(s).toLowerCase())
      .flatMap((s) => (s.includes("admin") ? ["admin"] : s.includes("user") ? ["user"] : [s]))
      .filter((v, i, arr) => arr.indexOf(v) === i);
  } catch {
    return [];
  }
}
