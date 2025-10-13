import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { jwtVerify, type JWTPayload } from "jose";

// Secret may be unavailable in some hosting setups; handle gracefully
const _rawSecret = process.env.JWT_SECRET;
const secret = _rawSecret ? new TextEncoder().encode(_rawSecret) : null;
const BACKEND_URL = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
const backendHost = (() => {
  try {
    return new URL(BACKEND_URL).host;
  } catch {
    return "";
  }
})();

function rolesFromPayload(payload: JWTPayload | undefined): string[] {
  if (!payload) return [];
  const get = (k: string) => payload[k as keyof JWTPayload] as unknown;
  const candidates: unknown[] = [get("role"), get("roles"), get("authorities")];
  for (const v of candidates) {
    if (typeof v === "string") return [v.toLowerCase()];
    if (Array.isArray(v)) {
      const arr = v.filter((x) => typeof x === "string").map((x) => (x as string).toLowerCase());
      if (arr.length) return arr;
    }
  }
  return [];
}

// Paths that don’t require auth (frontend API routes removed)
const publicPaths = ["/login", "/signup"];

export async function middleware(req: NextRequest) {
  // Let CORS preflight pass through untouched
  if (req.method === "OPTIONS") {
    return NextResponse.next();
  }

  // Skip middleware for Next.js internal fetches (RSC/data requests)
  // Next adds `rsc` during Server Components data fetches (not `_rsc`).
  if (
    req.nextUrl.searchParams.has("_rsc") ||
    req.nextUrl.searchParams.has("rsc") ||
    req.headers.get("x-middleware-subrequest") === "1"
  ) {
    return NextResponse.next();
  }


  const { pathname } = req.nextUrl;
  const isCrossSite = backendHost && backendHost !== req.nextUrl.host;

  // Skip middleware for public paths and static assets 
  if (
    publicPaths.some((path) => pathname.startsWith(path)) ||
    pathname.startsWith("/_next") ||
    pathname.startsWith("/favicon.ico") ||
    // Never intercept proxied backend API endpoints; let rewrites proxy to backend directly
    pathname.startsWith("/api/auth/") ||
    pathname.startsWith("/api/database/") ||
    pathname.startsWith("/api/wits/") ||
    pathname.startsWith("/gemini/") ||
    pathname.startsWith("/api/debug/") ||
    // Allow internal session endpoint to set access_token cookie
    pathname.startsWith("/api/session/")
  ) {
    // If user is already authenticated and hits /login or /signup, redirect them based on role
  if (pathname === "/login" || pathname === "/signup") {
      const authHeader2 = req.headers.get("authorization");
      // 1) Try an existing Authorization header
      if (authHeader2?.startsWith("Bearer ")) {
        try {
          const token = authHeader2.split(" ")[1];
          if (secret) {
            const { payload } = await jwtVerify(token, secret, {
              issuer: process.env.JWT_ISSUER || "tariff",
              audience: process.env.JWT_AUDIENCE || "tariff-web",
            });
            const roles = rolesFromPayload(payload);
            const isAdmin = roles.includes("admin");
            return NextResponse.redirect(new URL(isAdmin ? "/admin" : "/", req.url));
          } else {
            // Without secret, don't auto-redirect; let page render
          }
        } catch {
          // fall through
        }
      }
      // 2) Try access_token cookie on Amplify domain
      const accessCookiePublic = req.cookies.get("access_token")?.value;
      if (accessCookiePublic) {
        try {
          if (secret) {
            const { payload } = await jwtVerify(accessCookiePublic, secret, {
              issuer: process.env.JWT_ISSUER || "tariff",
              audience: process.env.JWT_AUDIENCE || "tariff-web",
            });
            const roles = rolesFromPayload(payload);
            const isAdmin = roles.includes("admin");
            return NextResponse.redirect(new URL(isAdmin ? "/admin" : "/", req.url));
          }
        } catch {
          // fall through
        }
      }
      // 3) Try refreshing with cookie
      const refreshCookie2 = req.cookies.get("refresh_token")?.value;
      if (refreshCookie2 && !isCrossSite) {
        try {
          const backendRes = await fetch(`${BACKEND_URL}/auth/refresh`, {
            method: "POST",
            headers: { Cookie: `refresh_token=${refreshCookie2}` },
          });
          if (backendRes.ok) {
            const data = await backendRes.json();
            const accessToken = data?.accessToken as string | undefined;
            if (accessToken) {
              let isAdmin = false;
              if (secret) {
                const { payload } = await jwtVerify(accessToken, secret, {
                  issuer: process.env.JWT_ISSUER || "tariff",
                  audience: process.env.JWT_AUDIENCE || "tariff-web",
                });
                const roles = rolesFromPayload(payload);
                isAdmin = roles.includes("admin");
              }
              const resp = NextResponse.redirect(new URL(isAdmin ? "/admin" : "/", req.url));
              const setCookie = backendRes.headers.get("set-cookie");
              if (setCookie) resp.headers.append("set-cookie", setCookie);
              return resp;
            }
          }
        } catch {
          // ignore
        }
      }
      // If cross-site, do NOT redirect from /login or /signup to avoid loops.
      // Let the login/signup pages render and perform direct backend calls from the client.
    }
    return NextResponse.next();
  }

  // 1. Try Authorization header first
  const authHeader = req.headers.get("authorization");
  if (authHeader?.startsWith("Bearer ")) {
    const token = authHeader.split(" ")[1];
    try {
      if (secret) {
        const { payload } = await jwtVerify(token, secret, {
          issuer: process.env.JWT_ISSUER || "tariff",
          audience: process.env.JWT_AUDIENCE || "tariff-web",
        });
        // Role-based gate for admin routes
        if (pathname.startsWith("/admin")) {
          const roles = rolesFromPayload(payload);
          const isAdmin = roles.includes("admin");
          if (!isAdmin) {
            return NextResponse.redirect(new URL("/", req.url));
          }
        }
        return NextResponse.next(); // valid token and allowed
      } else {
        // Without secret, rely on backend for auth; allow through, admin gated below
        if (pathname.startsWith("/admin")) {
          const check = await fetch(`${BACKEND_URL}/auth/me`, {
            headers: { authorization: `Bearer ${token}` },
          });
          if (!check.ok) return NextResponse.redirect(new URL("/", req.url));
          try {
            const me = await check.json();
            const roles = Array.isArray(me?.roles) ? me.roles.map((r: string) => r.toLowerCase()) : [];
            if (!roles.includes("admin")) return NextResponse.redirect(new URL("/", req.url));
          } catch {
            return NextResponse.redirect(new URL("/", req.url));
          }
        }
        return NextResponse.next();
      }
    } catch {
      // fall through to cookie check
    }
  }

  // 2. Try an access token stored as a cookie on the Amplify (frontend) domain.
  //    This enables auth gating in cross-site deployments where refresh cookies are not visible to Amplify.
  const accessCookie = req.cookies.get("access_token")?.value;
  if (accessCookie) {
    try {
      if (secret) {
        const { payload } = await jwtVerify(accessCookie, secret, {
          issuer: process.env.JWT_ISSUER || "tariff",
          audience: process.env.JWT_AUDIENCE || "tariff-web",
        });
        if (pathname.startsWith("/admin")) {
          const roles = rolesFromPayload(payload);
          if (!roles.includes("admin")) {
            return NextResponse.redirect(new URL("/", req.url));
          }
        }
      } else if (pathname.startsWith("/admin")) {
        // No secret: confirm role with backend
        const check = await fetch(`${BACKEND_URL}/auth/me`, {
          headers: { authorization: `Bearer ${accessCookie}` },
        });
        if (!check.ok) return NextResponse.redirect(new URL("/", req.url));
        try {
          const me = await check.json();
          const roles = Array.isArray(me?.roles) ? me.roles.map((r: string) => r.toLowerCase()) : [];
          if (!roles.includes("admin")) return NextResponse.redirect(new URL("/", req.url));
        } catch {
          return NextResponse.redirect(new URL("/", req.url));
        }
      }
      // Inject Authorization header for downstream handlers and API proxies
      const reqHeaders = new Headers(req.headers);
      reqHeaders.set("authorization", `Bearer ${accessCookie}`);
      return NextResponse.next({ request: { headers: reqHeaders } });
    } catch {
      // fall through to refresh logic
    }
  }

  // 3. No valid access token: attempt server-side refresh on the backend directly when same-site only.
    // const url = req.nextUrl; // not needed; avoid unused var warning
  const refreshCookie = req.cookies.get("refresh_token")?.value;
  if (!refreshCookie) {
    // If backend is on a different host, avoid redirect loops — allow request through;
    // page-level code should handle 401s and route to /login.
    if (isCrossSite) {
      // For protected routes (non-public, not static and not API proxies), require login
      const isProtected =
        !publicPaths.some((p) => pathname.startsWith(p)) &&
        !pathname.startsWith("/_next") &&
        !pathname.startsWith("/favicon.ico") &&
        !pathname.startsWith("/api/auth/") &&
        !pathname.startsWith("/api/database/") &&
        !pathname.startsWith("/api/wits/") &&
        !pathname.startsWith("/gemini/");
      if (isProtected) {
        return NextResponse.redirect(new URL("/login", req.url));
      }
      return NextResponse.next();
    }
    return NextResponse.redirect(new URL("/login", req.url));
  }

  try {
    // Server-side call; not subject to browser CORS and we only forward the single cookie we need
    // Only possible to forward cookie from middleware when same-site; otherwise we redirected above
    const backendRes = await fetch(`${BACKEND_URL}/auth/refresh`, {
      method: "POST",
      headers: {
        Cookie: `refresh_token=${refreshCookie}`,
      },
    });

    if (!backendRes.ok) {
      return NextResponse.redirect(new URL("/login", req.url));
    }

  const data = await backendRes.json();
  const accessToken = data?.accessToken as string | undefined;
    const resp = NextResponse.next();

    // Forward refresh cookie from backend if present
    const setCookie = backendRes.headers.get("set-cookie");
    if (setCookie) {
      resp.headers.append("set-cookie", setCookie);
    }

    // Inject Authorization for the downstream request if we received a token
    if (accessToken) {
      const reqHeaders = new Headers(req.headers);
      reqHeaders.set("authorization", `Bearer ${accessToken}`);
      // If accessing admin path, enforce role from freshly issued token
      if (pathname.startsWith("/admin")) {
        if (secret) {
          try {
            const { payload } = await jwtVerify(accessToken, secret, {
              issuer: process.env.JWT_ISSUER || "tariff",
              audience: process.env.JWT_AUDIENCE || "tariff-web",
            });
            const roles = rolesFromPayload(payload);
            const isAdmin = roles.includes("admin");
            if (!isAdmin) return NextResponse.redirect(new URL("/", req.url));
          } catch {
            return NextResponse.redirect(new URL("/login", req.url));
          }
        } else {
          const check = await fetch(`${BACKEND_URL}/auth/me`, {
            headers: { authorization: `Bearer ${accessToken}` },
          });
          if (!check.ok) return NextResponse.redirect(new URL("/", req.url));
          try {
            const me = await check.json();
            const roles = Array.isArray(me?.roles) ? me.roles.map((r: string) => r.toLowerCase()) : [];
            if (!roles.includes("admin")) return NextResponse.redirect(new URL("/", req.url));
          } catch {
            return NextResponse.redirect(new URL("/", req.url));
          }
        }
      }
      return NextResponse.next({ request: { headers: reqHeaders }, headers: resp.headers });
    }

    return resp;
  } catch {
    return NextResponse.redirect(new URL("/login", req.url));
  }
}

// Apply middleware to all routes except static files and public APIs
export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};