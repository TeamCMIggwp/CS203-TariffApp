"use client";

import React from "react";

type SessionInfo = {
  authenticated: boolean;
  exp: number | null;
  now: number;
  remainingSeconds: number;
};

type RefreshResponse = {
  accessToken?: string;
  ttl?: number;
};

/**
 * SessionKeeper
 * - Polls a tiny server API for access-token remaining time
 * - When < warnSeconds, shows a sticky prompt to stay signed in
 * - On confirm, silently refreshes via backend proxy and resets the access_token cookie
 * - If refresh fails or expires, guides the user to login
 */
export default function SessionKeeper({ warnSeconds = 120, showBadge = false }: { warnSeconds?: number; showBadge?: boolean }) {
  const [remaining, setRemaining] = React.useState<number | null>(null);
  const [showPrompt, setShowPrompt] = React.useState(false);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const poll = React.useCallback(async () => {
    try {
      const res = await fetch("/api/session/info", { credentials: "include", cache: "no-store" });
      if (!res.ok) {
        setRemaining(null);
        setShowPrompt(false);
        return;
      }
      const info: SessionInfo = await res.json();
      if (!info.authenticated) {
        setRemaining(null);
        setShowPrompt(false);
        return;
      }
      setRemaining(info.remainingSeconds);
      setShowPrompt((info.remainingSeconds ?? 0) > 0 && info.remainingSeconds <= warnSeconds);
    } catch {
      // Ignore intermittent errors
    }
  }, [warnSeconds]);

  React.useEffect(() => {
    let stop = false;
    const tick = async () => {
      if (stop) return;
      await poll();
      if (stop) return;
      setTimeout(tick, 15000); // poll every 15s
    };
    tick();
    return () => {
      stop = true;
    };
  }, [poll]);

  const onStaySignedIn = React.useCallback(async () => {
    setBusy(true);
    setError(null);
    try {
      // Attempt refresh via proxy (works when backend and frontend are same-site)
      const refreshRes = await fetch("/api/auth/refresh", { method: "POST", credentials: "include" });
      if (!refreshRes.ok) throw new Error("Refresh failed");
      const data = (await refreshRes.json()) as RefreshResponse;
      const accessToken = data.accessToken;
      const ttl = data.ttl;
      if (!accessToken) throw new Error("No token returned");
      const setRes = await fetch("/api/session/set", {
        method: "POST",
        credentials: "include",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ accessToken, ttl }),
      });
      if (!setRes.ok) throw new Error("Failed to set access token");
      // Hide prompt and repoll
      setShowPrompt(false);
      setBusy(false);
      poll();
    } catch (_err: unknown) {
      console.error("Token refresh error:", _err);
      // Fallback: cross-site refresh bridge (requires NEXT_PUBLIC_BACKEND_URL)
      setBusy(false);
      try {
        const backend = process.env.NEXT_PUBLIC_BACKEND_URL;
        if (backend && typeof window !== "undefined") {
          const returnTo = window.location.href;
          const url = `${backend.replace(/\/$/, "")}/auth/refresh-bridge?returnTo=${encodeURIComponent(returnTo)}`;
          window.location.href = url;
          return;
        }
      } catch {}
      setError(
        "We couldn't extend your session automatically. This can happen when the backend runs on a different domain. Please log in again."
      );
    }
  }, [poll]);

  const badge = showBadge && remaining !== null ? (
    <div className="fixed bottom-24 right-6 z-[60]">
      <div className="rounded-full bg-slate-900/70 text-white text-xs px-3 py-1 border border-cyan-400/40">
        {Math.max(0, remaining!)}s
      </div>
    </div>
  ) : null;

  if (!showPrompt) return badge;

  const minutes = remaining ? Math.floor(remaining / 60) : 0;
  const seconds = remaining ? remaining % 60 : 0;

  return (
    <>
    <div className="fixed bottom-24 left-1/2 -translate-x-1/2 z-[60] w-[min(92vw,640px)]">
      <div className="rounded-xl border border-cyan-400/40 bg-white/80 backdrop-blur-md shadow-lg p-4 text-slate-900">
        <div className="flex flex-col gap-3">
          <div className="font-semibold text-slate-800">
            Your session is about to expire
          </div>
          <div className="text-sm text-slate-700">
            It will expire in {minutes}m {seconds}s. Stay signed in?
          </div>
          {error && <div className="text-sm text-red-600">{error}</div>}
          <div className="flex gap-2 justify-end">
            <a
              href="/login"
              className="rounded-md px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-200/60"
            >
              Log in again
            </a>
            <button
              onClick={onStaySignedIn}
              disabled={busy}
              className="rounded-md bg-cyan-500 hover:bg-cyan-600 text-white px-3 py-2 text-sm font-medium disabled:opacity-60"
            >
              {busy ? "Refreshing…" : "Stay signed in"}
            </button>
          </div>
        </div>
      </div>
    </div>
    {badge}
    </>
  );
}
