"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={null}>
      <ResetPasswordInner />
    </Suspense>
  );
}

function ResetPasswordInner() {
  const API_BASE = process.env.NEXT_PUBLIC_BACKEND_URL || process.env.BACKEND_URL || "http://localhost:8080";
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
      const resp = await fetch(`${API_BASE}/auth/reset`, {
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
    <div className="max-w-md mx-auto p-6">
      <h1 className="text-2xl font-semibold mb-4">Reset password</h1>
      <form onSubmit={submit} className="space-y-4">
        <div className="flex flex-col gap-1">
          <label htmlFor="password" className="text-sm">New password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="border rounded px-3 py-2"
            required
            minLength={8}
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="confirm" className="text-sm">Confirm password</label>
          <input
            id="confirm"
            type="password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            className="border rounded px-3 py-2"
            required
            minLength={8}
          />
        </div>
        <button
          type="submit"
          disabled={submitting || !token || password !== confirm}
          className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-60"
        >
          {submitting ? "Resetting…" : "Reset password"}
        </button>
      </form>
      {status && <p className="mt-4 text-sm text-gray-700">{status}</p>}
    </div>
  );
}
