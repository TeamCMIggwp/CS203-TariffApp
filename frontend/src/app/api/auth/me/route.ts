import { NextResponse } from "next/server";
import { decodeJwt, type JWTPayload } from "jose";

const BACKEND_URL = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

type PrimitiveRole = string;
type RoleLikeObject = { authority?: unknown; role?: unknown; name?: unknown };

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === "object" && v !== null;
}

function asString(v: unknown): string | undefined {
  return typeof v === "string" ? v : undefined;
}

function extractRoleFromObject(o: unknown): string | undefined {
  if (!isRecord(o)) return undefined;
  const keys: Array<keyof RoleLikeObject> = ["authority", "role", "name"];
  for (const k of keys) {
    const val = o[k as keyof typeof o];
    if (typeof val === "string") return val;
  }
  return undefined;
}

function normalizeRoles(values: string[]): string[] {
  const out: string[] = [];
  for (const s of values) {
    const lower = s.toLowerCase().trim();
    if (lower.startsWith("role_")) out.push(lower.replace(/^role_/, ""));
    else if (lower.includes("administrator")) out.push("admin");
    else if (lower.includes("admin")) out.push("admin");
    else if (lower.includes("user")) out.push("user");
    else out.push(lower);
  }
  return Array.from(new Set(out));
}

function extractRolesFromUnknown(val: unknown): string[] {
  const out: string[] = [];
  if (!val) return out;
  if (typeof val === "string") return [val];
  if (Array.isArray(val)) {
    for (const item of val) {
      if (typeof item === "string") out.push(item);
      else {
        const maybe = extractRoleFromObject(item);
        if (maybe) out.push(maybe);
      }
    }
    return out;
  }
  const maybeOne = extractRoleFromObject(val);
  if (maybeOne) out.push(maybeOne);
  return out;
}

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
      const rawRole = payload["role"];
      const rawRoles = payload["roles"];
      const rawAuthorities = payload["authorities"];
      const collected: string[] = [
        ...extractRolesFromUnknown(rawRole),
        ...extractRolesFromUnknown(rawRoles),
        ...extractRolesFromUnknown(rawAuthorities),
      ];
      return normalizeRoles(collected);
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

    const me: unknown = await res.json();
    const meRecord = isRecord(me) ? me : undefined;
    const roles = normalizeRoles([
      ...extractRolesFromUnknown(meRecord?.role),
      ...extractRolesFromUnknown(meRecord?.roles),
      ...extractRolesFromUnknown(meRecord?.authorities),
    ]);
    // Merge with token-derived roles to be forgiving about backend shape
    const merged = Array.from(new Set([...(roles || []), ...(tokenRoles || [])]));
    return NextResponse.json({ authenticated: true, roles: merged }, { status: 200 });
  } catch {
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
    const payload = decodeJwt(token) as JWTPayload;
    const collected: string[] = [
      ...extractRolesFromUnknown(payload["role"]),
      ...extractRolesFromUnknown(payload["roles"]),
      ...extractRolesFromUnknown(payload["authorities"]),
    ];
    return normalizeRoles(collected);
  } catch {
    return [];
  }
}
