import { useState } from "react";
import { useRouter } from "next/navigation";
import { decodeJwt, type JWTPayload } from "jose";

type AuthResponse = {
  accessToken?: string;
  role?: string;
  message?: string;
};

function rolesFromPayload(payload: JWTPayload | undefined): string[] {
  if (!payload) return [];
  const read = (k: string) => payload[k as keyof JWTPayload] as unknown;
  const candidates: unknown[] = [read("role"), read("roles"), read("authorities")];
  for (const val of candidates) {
    if (typeof val === "string") return [val.toLowerCase()];
    if (Array.isArray(val)) {
      const arr = val.filter((v) => typeof v === "string").map((v) => (v as string).toLowerCase());
      if (arr.length) return arr;
    }
  }
  return [];
}

export function useLogin() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    if (!username || !password) {
      setError("Please fill in both fields.");
      setLoading(false);
      return;
    }

    try {
      // In production on Amplify, calling the backend directly ensures the refresh cookie is set on the backend domain.
      // Locally, we keep using the Next.js rewrite to avoid CORS.
      const backend = process.env.NEXT_PUBLIC_BACKEND_URL || process.env.BACKEND_URL;
      const loginUrl = backend && typeof window !== "undefined" && window.location.hostname !== new URL(backend).hostname
        ? `${backend}/auth/login`
        : `/api/auth/login`;
      const res = await fetch(loginUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        // Send cookies across origins if backend sets refresh cookie
        credentials: "include",
        body: JSON.stringify({ email: username, password }),
      });

      const data: AuthResponse = await res.json().catch(() => ({} as AuthResponse)); // guard against non‑JSON

      if (!res.ok) {
        setError(data.message || "Invalid credentials.");
      } else {
        // Prefer explicit role from backend; fallback to decoding token
        let dest = "/";
        const roleResp = data?.role || undefined;
        if (roleResp) {
          dest = roleResp === "admin" ? "/admin" : "/";
        } else {
          const token: string | undefined = data?.accessToken;
          try {
            if (token) {
              const payload = decodeJwt(token) as JWTPayload;
              const roles = rolesFromPayload(payload);
              const isAdmin = roles.includes("admin");
              dest = isAdmin ? "/admin" : "/";
            }
          } catch {}
        }
        // Set an access_token cookie on the Amplify domain so middleware can gate protected routes
        try {
          if (data?.accessToken) {
            await fetch("/api/session/set", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ accessToken: data.accessToken }),
            });
          }
        } catch {}
        // Log decision for quick debugging
        if (typeof window !== "undefined") {
          console.log("login redirect:", { role: roleResp ?? "(decoded)", dest });
        }
        // Robust navigation: try SPA navigation, then hard redirect fallback
        try {
          // replace prevents going back to login on browser back
          // next/navigation router.replace is safe in client components
          router.replace(dest);
        } catch {}
        // Fallback in case SPA navigation is blocked by pending state
        setTimeout(() => {
          if (typeof window !== "undefined" && window.location.pathname !== dest) {
            window.location.assign(dest);
          }
        }, 0);
      }
    } catch (err) {
      console.error("Login error:", err);
      setError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return {
    username,
    setUsername,
    password,
    setPassword,
    error,
    loading,
    handleSubmit,
  };
}