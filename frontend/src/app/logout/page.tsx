'use client';

import { useEffect, useState } from 'react';

export default function LogoutPage() {
  const [status, setStatus] = useState('Signing you out...');
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const r = await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
        const ok = r.ok;
        if (!cancelled) setStatus(ok ? 'Signed out. Redirecting...' : 'Sign out failed. Redirecting...');
      } catch {
        if (!cancelled) setStatus('Sign out failed. Redirecting...');
      } finally {
        setTimeout(() => {
          if (!cancelled) window.location.href = '/';
        }, 800);
      }
    })();
    return () => { cancelled = true; };
  }, []);
  return (
    <section className="py-20 min-h-screen relative z-10">
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="text-center bg-black/40 backdrop-blur-lg p-8 rounded-2xl border border-white/30">
          <p className="text-white/90">{status}</p>
        </div>
      </div>
    </section>
  );
}
