import { useState } from "react";

export function useLogin() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!username || !password) {
      setError("Please fill in both fields.");
      return;
    }

    // Example: replace with real auth call
    if (username === "test@example.com" && password === "password") {
      alert("Login successful!");
      setError("");
    } else {
      setError("Invalid credentials.");
    }
  };

  return {
    username,
    setUsername,
    password,
    setPassword,
    error,
    handleSubmit,
  };
}