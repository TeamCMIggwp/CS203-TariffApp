import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { jwtVerify, type JWTPayload } from "jose";

const secret = new TextEncoder().encode(process.env.JWT_SECRET!);
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
    pathname.startsWith("/gemini/")
  ) {
    // If user is already authenticated and hits /login or /signup, redirect them based on role
  if (pathname === "/login" || pathname === "/signup") {
      const authHeader2 = req.headers.get("authorization");
      // 1) Try an existing Authorization header
      if (authHeader2?.startsWith("Bearer ")) {
        try {
          const token = authHeader2.split(" ")[1];
          const { payload } = await jwtVerify(token, secret, {
            issuer: process.env.JWT_ISSUER || "tariff",
            audience: process.env.JWT_AUDIENCE || "tariff-web",
          });
          const roles = rolesFromPayload(payload);
          const isAdmin = roles.includes("admin");
          return NextResponse.redirect(new URL(isAdmin ? "/admin" : "/", req.url));
        } catch {
          // fall through
        }
      }
      // 2) Try refreshing with cookie
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
              const { payload } = await jwtVerify(accessToken, secret, {
                issuer: process.env.JWT_ISSUER || "tariff",
                audience: process.env.JWT_AUDIENCE || "tariff-web",
              });
              const roles = rolesFromPayload(payload);
              const isAdmin = roles.includes("admin");
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
      // If we are on a different domain than the backend, we cannot read/send the backend cookie from middleware.
      // Delegate refresh to the backend via browser redirect so the browser sends the cookie to BACKEND_URL.
      if (isCrossSite) {
        const url = `${BACKEND_URL}/auth/refresh?returnTo=${encodeURIComponent(req.url)}`;
        return NextResponse.redirect(url);
      }
    }
    return NextResponse.next();
  }

  // 1. Try Authorization header first
  const authHeader = req.headers.get("authorization");
  if (authHeader?.startsWith("Bearer ")) {
    const token = authHeader.split(" ")[1];
    try {
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
    } catch {
      // fall through to cookie check
    }
  }

  // 2. No valid access token: attempt server-side refresh on the backend directly.
    // const url = req.nextUrl; // not needed; avoid unused var warning
  const refreshCookie = req.cookies.get("refresh_token")?.value;
  if (!refreshCookie) {
    // If backend is on a different host, redirect the browser to backend refresh so it can use its own cookie.
    if (isCrossSite) {
      const url = `${BACKEND_URL}/auth/refresh?returnTo=${encodeURIComponent(req.url)}`;
      return NextResponse.redirect(url);
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
      try {
        const { payload } = await jwtVerify(accessToken, secret, {
          issuer: process.env.JWT_ISSUER || "tariff",
          audience: process.env.JWT_AUDIENCE || "tariff-web",
        });
        if (pathname.startsWith("/admin")) {
          const roles = rolesFromPayload(payload);
          const isAdmin = roles.includes("admin");
          if (!isAdmin) {
            return NextResponse.redirect(new URL("/", req.url));
          }
        }
      } catch {
        return NextResponse.redirect(new URL("/login", req.url));
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