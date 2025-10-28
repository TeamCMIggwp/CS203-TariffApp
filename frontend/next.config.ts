import type { NextConfig } from "next";

const BACKEND_URL = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

const nextConfig: NextConfig = {
  // Produce a standalone server build so Amplify can package SSR correctly
  output: "standalone",
  reactStrictMode: true,
  async rewrites() {
    return [
      {
        source: "/api/auth/:path*",
        destination: `${BACKEND_URL}/auth/:path*`,
      },
      {
        source: "/api/database/:path*",
        // Proxy frontend /api/database/* to backend /api/v1/* so middleware can inject Authorization
        destination: `${BACKEND_URL}/api/v1/:path*`,
      },
      {
        source: "/api/wits/:path*",
        destination: `${BACKEND_URL}/api/wits/:path*`,
      },
      {
        source: "/gemini/:path*",
        destination: `${BACKEND_URL}/gemini/:path*`,
      },
    ];
  },
};

export default nextConfig;
