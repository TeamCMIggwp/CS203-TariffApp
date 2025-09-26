import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { jwtVerify } from "jose";

const secret = new TextEncoder().encode(process.env.JWT_SECRET!);

// Paths that don’t require auth
const publicPaths = ["/login", "/signup", "/api/login", "/api/refresh"];

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;

  // Skip middleware for public paths and static assets
  if (
    publicPaths.some((path) => pathname.startsWith(path)) ||
    pathname.startsWith("/_next") ||
    pathname.startsWith("/favicon.ico")
  ) {
    return NextResponse.next();
  }

  // 1. Try Authorization header first
  const authHeader = req.headers.get("authorization");
  if (authHeader?.startsWith("Bearer ")) {
    const token = authHeader.split(" ")[1];
    try {
      await jwtVerify(token, secret, {
        issuer: process.env.JWT_ISSUER || "tariff",
        audience: process.env.JWT_AUDIENCE || "tariff-web",
      });
      return NextResponse.next(); // valid access token
    } catch {
      // fall through to cookie check
    }
  }

  // 2. If no valid access token, check refresh_token cookie
  const refreshToken = req.cookies.get("refresh_token")?.value;
  if (refreshToken) {
    // Instead of verifying here (opaque UUID), we just trust DB lookup
    // Redirect internally to your refresh API route
    const refreshUrl = req.nextUrl.clone();
    refreshUrl.pathname = "/api/refresh";
    return NextResponse.rewrite(refreshUrl);
  }

  // 3. No valid token or refresh cookie → redirect to login
  return NextResponse.redirect(new URL("/login", req.url));
}

// Apply middleware to all routes except static files and public APIs
export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|api/login|api/refresh|login|signup).*)",
  ],
};