import { NextResponse } from "next/server";

export async function GET() {
  // Do NOT leak secrets; only indicate presence/length
  const jwtSecret = process.env.JWT_SECRET || "";
  const BACKEND_URL = process.env.BACKEND_URL || "";
  const NEXT_PUBLIC_BACKEND_URL = process.env.NEXT_PUBLIC_BACKEND_URL || "";
  const computedBackend = BACKEND_URL || NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

  const envKeys = Object.keys(process.env || {}).filter((k) =>
    /^(JWT_|NEXT_PUBLIC_|BACKEND_URL|AMPLIFY_)/.test(k)
  );

  return NextResponse.json({
    time: new Date().toISOString(),
    nodeEnv: process.env.NODE_ENV || "",
    hasJWT_SECRET: !!jwtSecret,
    JWT_SECRET_LEN: jwtSecret.length || 0,
    JWT_ISSUER: process.env.JWT_ISSUER || "",
    JWT_AUDIENCE: process.env.JWT_AUDIENCE || "",
    BACKEND_URL,
    NEXT_PUBLIC_BACKEND_URL,
    computedBackend,
    amplify: {
      branch: process.env.AMPLIFY_BRANCH || null,
      appId: process.env.AMPLIFY_APP_ID || null,
      monorepoAppRoot: process.env.AMPLIFY_MONOREPO_APP_ROOT || null,
    },
    presentEnvKeys: envKeys,
  });
}
