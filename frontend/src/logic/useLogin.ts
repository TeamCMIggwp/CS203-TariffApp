import { useState } from "react";
import { useRouter } from "next/navigation";
import { decodeJwt } from "jose";

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
      // use Next.js rewrite to proxy to backend, avoiding browser CORS entirely
      const res = await fetch(`/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        // Send cookies across origins if backend sets refresh cookie
        credentials: "include",
        body: JSON.stringify({ email: username, password }),
      });

  const data = await res.json().catch(() => ({})); // guard against non‑JSON

      if (!res.ok) {
        setError(data.message || "Invalid credentials.");
      } else {
        // Prefer explicit role from backend; fallback to decoding token
        let dest = "/";
        const roleResp = (data?.role as string | undefined) || undefined;
        if (roleResp) {
          dest = roleResp === "admin" ? "/admin" : "/";
        } else {
          const token: string | undefined = data?.accessToken;
          try {
            if (token) {
              const payload: any = decodeJwt(token);
              const role = (payload?.role || payload?.roles || payload?.authorities) as string | string[] | undefined;
              const isAdmin = Array.isArray(role) ? role.includes("admin") : role === "admin";
              dest = isAdmin ? "/admin" : "/";
            }
          } catch {}
        }
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