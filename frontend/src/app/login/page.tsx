"use client";

import React from "react";
import Link from "next/link";
import styles from "./login.module.css";
import { useLogin } from "../../logic/useLogin";

export default function LoginPage() {
  const {
    username,
    setUsername,
    password,
    setPassword,
    error,
    success,
    loading,
    handleSubmit,
  } = useLogin();

  return (
    <div className={styles.container}>
      <div className={styles.formSection}>
        <h1 className={styles.logo}>Tariff</h1>
        <p className={styles.welcome}>Welcome back. Please enter your details.</p>

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

          {/* Error + success messages */}
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
          {success && <div className={styles.success}>{success}</div>}

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

          <button type="button" className={styles.googleBtn}>
            <img
              src="/google.svg"
              alt="Google"
              className={styles.googleIcon}
            />
            Sign in with Google
          </button>
        </form>

        <p className={styles.signup}>
          Don’t have an account?{" "}
          <Link href="/signup" className={styles.link}>
            Sign Up Now!
          </Link>
        </p>
      </div>

      <div className={styles.illustration}>
        <div className={styles.placeholderArt}>[ Illustration ]</div>
      </div>
    </div>
  );
}