"use client";

import React from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import styles from "./login.module.css";
import { useLogin } from "../../logic/useLogin";
import { useRouter } from "next/navigation";

export default function LoginPage() {
  const {
    username,
    setUsername,
    password,
    setPassword,
    error,
    loading,
    handleSubmit,
  } = useLogin();

  const router = useRouter();
  const googleRef = React.useRef<HTMLDivElement | null>(null);
  const [googleReady, setGoogleReady] = React.useState(false);
  const [gError, setGError] = React.useState<string>("");

  // Dynamically load Google Identity Services and render the button
  React.useEffect(() => {
    if (typeof window === "undefined") return;
    const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
    if (!clientId) {
      // No client id configured; keep custom button disabled
      return;
    }
    const ensureScript = () =>
      new Promise<void>((resolve, reject) => {
        if ((window as any).google?.accounts) return resolve();
        const s = document.createElement("script");
        s.src = "https://accounts.google.com/gsi/client";
        s.async = true;
        s.defer = true;
        s.onload = () => resolve();
        s.onerror = () => reject(new Error("Failed to load Google script"));
        document.head.appendChild(s);
      });

    const handleCredential = async (resp: any) => {
      try {
        setGError("");
        const credential: string | undefined = resp?.credential;
        if (!credential) {
          setGError("Google did not return a credential.");
          return;
        }
        // Call backend via Next rewrite to set refresh cookie
        const backend = process.env.NEXT_PUBLIC_BACKEND_URL || process.env.BACKEND_URL;
        const googleUrl = backend && typeof window !== "undefined" && window.location.hostname !== new URL(backend).hostname
          ? `${backend}/auth/google`
          : `/api/auth/google`;
        const res = await fetch(googleUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
          body: JSON.stringify({ idToken: credential }),
        });
        const data = await res.json().catch(() => ({} as any));
        if (!res.ok) {
          setGError(data?.message || "Google sign-in failed.");
          return;
        }
        // Store a short-lived access token as HttpOnly on Amplify/Next domain for middleware gating
        try {
          if (data?.accessToken) {
            await fetch("/api/session/set", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ accessToken: data.accessToken }),
            });
          }
        } catch {}
        const dest = data?.role === "admin" ? "/admin" : "/";
        try { router.replace(dest); } catch {}
        setTimeout(() => { if (window.location.pathname !== dest) window.location.assign(dest); }, 0);
      } catch (e: any) {
        setGError("Google sign-in error. Please try again.");
      }
    };

    let cancelled = false;
    ensureScript().then(() => {
      if (cancelled) return;
      const g = (window as any).google;
      if (!g?.accounts?.id) return;
      try {
        g.accounts.id.initialize({ client_id: clientId, callback: handleCredential });
        if (googleRef.current) {
          g.accounts.id.renderButton(googleRef.current, {
            theme: "outline",
            size: "large",
            width: 320,
            text: "signin_with",
            shape: "rectangular",
          });
          setGoogleReady(true);
        }
      } catch {
        // ignore
      }
    }).catch(() => {
      // ignore
    });
    return () => { cancelled = true; };
  }, []);

  return (
    <div className={styles.container}>
      <Card className="w-full max-w-md mx-auto">
        <CardHeader>
          <CardTitle className="text-center text-2xl font-bold">Tariff</CardTitle>
          <CardDescription className="text-center">
            Welcome back. Please enter your details.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form className={styles.form} onSubmit={handleSubmit}>
            <div className={styles.formGroup}>
              <label htmlFor="username">Email address</label>
              <input
                id="username"
                type="email"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            {error && (
              <div className={styles.error}>
                {error}
                {error.toLowerCase().includes("already registered") && (
                  <Link href="/signup" className={styles.link}>
                    {" "}
                    Sign up instead
                  </Link>
                )}
              </div>
            )}

            <div className={styles.options}>
              <label className={styles.checkbox}>
                <input type="checkbox" /> Remember for 30 days
              </label>
              <a href="#" className={styles.link}>
                Forgot password?
              </a>
            </div>

            <button type="submit" className={styles.signInBtn} disabled={loading}>
              {loading ? "Signing in..." : "Sign in"}
            </button>
            <div className={styles.googleBtn} style={{ padding: 0 }}>
              {/* Google renders its own button here */}
              <div ref={googleRef} style={{ width: "100%", display: "flex", justifyContent: "center" }} />
            </div>
            {gError && <div className={styles.error}>{gError}</div>}
          </form>

          <p className={styles.signup}>
            Dont have an account?{" "}
            <Link href="/signup" className={styles.link}>
              Sign Up Now!
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}