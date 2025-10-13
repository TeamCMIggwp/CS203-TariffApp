import { NextResponse } from "next/server";

export async function GET() {
  const secret = process.env.JWT_SECRET || "";
  const envKeys = Object.keys(process.env || {}).filter((k) =>
    /^(JWT_|NEXT_PUBLIC_|BACKEND_URL|AMPLIFY_)/.test(k)
  );
  return NextResponse.json({
    hasSecret: !!secret,
    secretLen: secret.length,
    issuer: process.env.JWT_ISSUER || null,
    audience: process.env.JWT_AUDIENCE || null,
    backendUrl: process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || null,
    // Additional diagnostics (no secret values exposed)
    amplify: {
      branch: process.env.AMPLIFY_BRANCH || null,
      appId: process.env.AMPLIFY_APP_ID || null,
      monorepoAppRoot: process.env.AMPLIFY_MONOREPO_APP_ROOT || null,
    },
    presentEnvKeys: envKeys,
  });
}
