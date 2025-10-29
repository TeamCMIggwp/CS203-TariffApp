"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

export default function BridgeRefreshPage() {
  const search = useSearchParams();
  const router = useRouter();
  const accessToken = search.get("accessToken");
  const ttl = search.get("ttl");
  const returnTo = search.get("returnTo") || "/";
  const [error, setError] = useState<string | null>(null);

  const ttlNum = useMemo(() => {
    const n = Number(ttl);
    return Number.isFinite(n) && n > 0 ? n : undefined;
  }, [ttl]);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (!accessToken) {
        setError("Missing access token");
        router.replace(`/login?reason=session_expired&returnTo=${encodeURIComponent(returnTo)}`);
        return;
      }
      try {
        const res = await fetch("/api/session/set", {
          method: "POST",
          credentials: "include",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({ accessToken, ttl: ttlNum }),
        });
        if (!res.ok) throw new Error("Failed to set session cookie");
        if (cancelled) return;
        // Clean up the URL by replacing state without query params
        if (typeof window !== "undefined" && window.history?.replaceState) {
          window.history.replaceState({}, "", returnTo || "/");
        }
        router.replace(returnTo || "/");
      } catch (e: any) {
        setError(e?.message || "Failed to refresh session");
        router.replace(`/login?reason=session_expired&returnTo=${encodeURIComponent(returnTo)}`);
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [accessToken, ttlNum, returnTo, router]);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="rounded-xl border border-cyan-400/40 bg-white/80 backdrop-blur-md shadow-lg p-6 text-slate-900">
        <div className="font-semibold text-slate-800">Refreshing your session…</div>
        {error && <div className="text-sm text-red-600 mt-2">{error}</div>}
      </div>
    </div>
  );
}
