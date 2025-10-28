"use client";

import { useState } from "react";
import { IconMail, IconCheck, IconAlertCircle } from "@tabler/icons-react";

export default function ForgotPasswordPage() {
  // Use Next proxy so we avoid CORS/env drift; this rewrites to BACKEND_URL/auth/forgot
  const FORGOT_URL = "/api/auth/forgot";
  const [email, setEmail] = useState("");
  const [status, setStatus] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;
    setSubmitting(true);
    setStatus(null);
    try {
      await fetch(FORGOT_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
        credentials: "include",
      });
      setStatus("If the email exists, a reset link has been sent.");
    } catch {
      setStatus("If the email exists, a reset link has been sent.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="py-20 min-h-screen relative z-10">
      <div className="max-w-md mx-auto px-4">
        <div className="text-center mb-8 bg-black/30 backdrop-blur-md p-6 rounded-2xl border border-white/30">
          <h1 className="text-4xl font-extrabold text-white mb-2">Forgot Password</h1>
          <p className="text-white/80">Enter your account email and we&apos;ll send you a reset link</p>
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
              <label htmlFor="email" className="text-white/90 text-sm font-medium flex items-center gap-2 mb-2">
                <IconMail className="w-5 h-5 text-cyan-300" /> Email
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-4 py-2 bg-black/70 border border-white/20 rounded text-white focus:outline-none focus:border-cyan-400/60"
                required
              />
            </div>
            <div className="flex items-center gap-3">
              <button
                type="submit"
                disabled={submitting}
                className="bg-cyan-500/30 hover:bg-cyan-500/40 text-cyan-100 font-bold px-6 py-2 rounded-xl border-2 border-cyan-400/40 disabled:opacity-60"
              >
                {submitting ? "Sending…" : "Send reset link"}
              </button>
              {!status && (
                <span className="text-white/70 text-sm flex items-center gap-2">
                  <IconAlertCircle className="w-4 h-4 text-white/50" />
                  We wont disclose whether an email exists.
                </span>
              )}
            </div>
          </form>
        </div>
      </div>
    </section>
  );
}
