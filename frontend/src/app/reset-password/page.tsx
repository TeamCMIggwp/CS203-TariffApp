"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { IconLock, IconCheck, IconAlertCircle } from "@tabler/icons-react";

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={null}>
      <ResetPasswordInner />
    </Suspense>
  );
}

function ResetPasswordInner() {
  // Use Next proxy for auth reset
  const RESET_URL = "/api/auth/reset";
  const params = useSearchParams();
  const router = useRouter();
  const [token, setToken] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [status, setStatus] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const t = params.get("token") || "";
    setToken(t);
  }, [params]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token || !password || password !== confirm) return;
    setSubmitting(true);
    setStatus(null);
    try {
      const resp = await fetch(RESET_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, newPassword: password }),
        credentials: "include",
      });
      const data = await resp.json().catch(() => ({}));
      if (data && data.updated === 1) {
        setStatus("Password updated. Redirecting to login…");
        setTimeout(() => router.push("/login"), 1200);
      } else {
        setStatus(data?.message || "Could not reset password.");
      }
    } catch {
      setStatus("Could not reset password.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="py-20 min-h-screen relative z-10">
      <div className="max-w-md mx-auto px-4">
        <div className="text-center mb-8 bg-black/30 backdrop-blur-md p-6 rounded-2xl border border-white/30">
          <h1 className="text-4xl font-extrabold text-white mb-2">Reset Password</h1>
          <p className="text-white/80">Choose a strong new password for your account</p>
        </div>

        <div className="bg-black/50 backdrop-blur-xl rounded-2xl p-8 border-2 border-white/30">
          {status && (
            <div className="mb-4 flex items-center gap-2 text-sm text-cyan-200">
              <IconCheck className="w-5 h-5 text-cyan-300" />
              <span>{status}</span>
            </div>
          )}
          <form onSubmit={submit} className="space-y-5">
            <div>
              <label htmlFor="password" className="text-white/90 text-sm font-medium mb-2 flex items-center gap-2">
                <IconLock className="w-5 h-5 text-cyan-300" /> New password
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-4 py-2 bg-black/70 border border-white/20 rounded text-white focus:outline-none focus:border-cyan-400/60"
                required
                minLength={8}
              />
            </div>
            <div>
              <label htmlFor="confirm" className="text-white/90 text-sm font-medium mb-2 block">Confirm password</label>
              <input
                id="confirm"
                type="password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                className="w-full px-4 py-2 bg-black/70 border border-white/20 rounded text-white focus:outline-none focus:border-cyan-400/60"
                required
                minLength={8}
              />
            </div>
            <div className="flex items-center gap-3">
              <button
                type="submit"
                disabled={submitting || !token || password !== confirm}
                className="bg-cyan-500/30 hover:bg-cyan-500/40 text-cyan-100 font-bold px-6 py-2 rounded-xl border-2 border-cyan-400/40 disabled:opacity-60"
              >
                {submitting ? "Resetting…" : "Reset password"}
              </button>
              {!status && (
                <span className="text-white/70 text-sm flex items-center gap-2">
                  <IconAlertCircle className="w-4 h-4 text-white/50" />
                  Password must be at least 8 characters.
                </span>
              )}
            </div>
          </form>
        </div>
      </div>
    </section>
  );
}
