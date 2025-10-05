import { SignJWT, jwtVerify } from "jose";

const secret = new TextEncoder().encode(process.env.JWT_SECRET!);

export async function signAccessToken(payload: { userId: string; role?: string }) {
  return await new SignJWT(payload)
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setIssuer(process.env.JWT_ISSUER || "tariff")
    .setAudience(process.env.JWT_AUDIENCE || "tariff-web")
    .setExpirationTime(`${process.env.ACCESS_TOKEN_TTL_SECONDS || 900}s`)
    .sign(secret);
}

export async function verifyAccessToken(token: string) {
  const { payload } = await jwtVerify(token, secret, {
    issuer: process.env.JWT_ISSUER || "tariff",
    audience: process.env.JWT_AUDIENCE || "tariff-web",
  });
  return payload as { userId: string; role?: string; iat: number; exp: number };
}

export async function appLogout(): Promise<void> {
  try {
    await fetch("/api/auth/logout", { method: "POST", credentials: "include" });
  } catch {
    // ignore network errors during logout
  }
}