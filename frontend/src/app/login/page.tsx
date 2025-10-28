"use client";

import React from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import styles from "./login.module.css";
import { useLogin } from "../../logic/useLogin";
import { useRouter } from "next/navigation";

// Minimal types for Google Identity Services and auth responses
interface GoogleCredentialResponse { credential?: string; select_by?: string }
interface GoogleAccountsId {
  initialize(args: { client_id: string; callback: (resp: GoogleCredentialResponse) => void }): void;
  renderButton(el: HTMLElement, opts: Record<string, unknown>): void;
}
interface WithGoogleWindow extends Window {
  google?: { accounts?: { id?: GoogleAccountsId } };
}
type AuthResponse = { accessToken?: string; role?: string; message?: string };

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
        if ((window as unknown as WithGoogleWindow).google?.accounts) return resolve();
        const s = document.createElement("script");
        s.src = "https://accounts.google.com/gsi/client";
        s.async = true;
        s.defer = true;
        s.onload = () => resolve();
        s.onerror = () => reject(new Error("Failed to load Google script"));
        document.head.appendChild(s);
      });

    const handleCredential = async (resp: GoogleCredentialResponse) => {
      try {
        setGError("");
        const credential: string | undefined = resp?.credential;
        if (!credential) {
          setGError("Google did not return a credential.");
          return;
        }
        // Call backend via Next rewrite to set refresh cookie
        // Always route via Next.js rewrite to avoid CORS and ensure cookies land on the frontend domain
        const googleUrl = `/api/auth/google`;
        const res = await fetch(googleUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
          body: JSON.stringify({ idToken: credential }),
        });
        const data = (await res.json().catch(() => ({}))) as AuthResponse;
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
        } catch {
          // no-op
        }
        const dest = data?.role === "admin" ? "/admin" : "/";
        try { router.replace(dest); } catch {
          // no-op
        }
        setTimeout(() => { if (window.location.pathname !== dest) window.location.assign(dest); }, 0);
      } catch {
        setGError("Google sign-in error. Please try again.");
      }
    };

    let cancelled = false;
    ensureScript().then(() => {
      if (cancelled) return;
      const g = (window as unknown as WithGoogleWindow).google;
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
        }
      } catch {
        // ignore
      }
    }).catch(() => {
      // ignore
    });
    return () => { cancelled = true; };
  }, [router]);

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
              <Link href="/forgot-password" className={styles.link}>
                Forgot password?
              </Link>
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