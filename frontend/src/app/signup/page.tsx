"use client";

import React, { useState } from "react";
import Link from "next/link";
import styles from "./signup.module.css";
import countries from "world-countries";
import {
  Card,
  CardHeader,
  CardContent,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";

const countryOptions = countries.map(c => ({
  value: c.cca2,
  label: c.name.common,
}));

export default function SignupPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [country, setCountry] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    try {
      const res = await fetch("/api/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, email, country, password }),
      });

      const data = await res.json();
      if (res.status === 409) {
        setError("This email is already registered. Try logging in?");
      } else if (!res.ok) {
        setError(data.message || "Signup failed");
      } else {
        setSuccess("Signup successful! You can now log in.");
        setName("");
        setEmail("");
        setCountry("");
        setPassword("");
        setConfirmPassword("");
      }
    } catch (err) {
      console.error(err);
      setError("Something went wrong. Please try again.");
    }
  }

  return (
    <div className={styles.container}>
      <Card className="w-full max-w-md mx-auto backdrop-blur-sm bg-white/80">
        <CardHeader>
          <CardTitle className="text-center text-2xl font-bold">Tariff</CardTitle>
          <CardDescription className="text-center">
            Create your account to get started.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form className={styles.form} onSubmit={handleSubmit}>
            <div className={styles.formGroup}>
              <label htmlFor="name">Full Name</label>
              <input
                id="name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="email">Email address</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="country">Country</label>
              <select
                id="country"
                name="country"
                value={country}
                onChange={(e) => setCountry(e.target.value)}
                required
              >
                <option value="">Select your country</option>
                {countryOptions.map((c) => (
                  <option key={c.value} value={c.value}>
                    {c.label}
                  </option>
                ))}
              </select>
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

            <div className={styles.formGroup}>
              <label htmlFor="confirmPassword">Confirm Password</label>
              <input
                id="confirmPassword"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </div>

            {error && (
              <div className={styles.error}>
                {error}
                {error.includes("already registered") && (
                  <Link href="/login" className={styles.link}>
                    {" "}
                    Log in instead
                  </Link>
                )}
              </div>
            )}

            {success && <div className={styles.success}>{success}</div>}

            <button type="submit" className={styles.signUpBtn}>
              Sign up
            </button>
          </form>

          <p className={styles.loginLink}>
            Already have an account?{" "}
            <Link href="/login" className={styles.link}>
              Log in
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}