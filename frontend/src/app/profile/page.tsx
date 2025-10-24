'use client';

import { useEffect, useState } from 'react';
import { IconUser, IconMail, IconLock, IconAlertCircle, IconCheck } from '@tabler/icons-react';

interface ProfileResponse {
  userId?: string;
  email?: string;
  name?: string | null;
  role?: string;
  message?: string;
}

export default function ProfilePage() {
  const [, setProfile] = useState<ProfileResponse | null>(null);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [authenticated, setAuthenticated] = useState<boolean | null>(null);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [pwdMsg, setPwdMsg] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        // Step 1: check session
        const me = await fetch('/api/auth/me', { credentials: 'include', cache: 'no-store' });
        const meJson = await me.json().catch(() => ({ authenticated: false }));
        const authed = !!meJson?.authenticated;
        setAuthenticated(authed);

        if (!authed) {
          setError('Please log in to view your profile.');
          return;
        }

        // Step 2: fetch profile details
        const r = await fetch('/api/profile', { credentials: 'include', cache: 'no-store' });
        if (!r.ok) {
          // Do not treat as unauthenticated; session may exist but token missing for profile fetch
          const msg = r.status === 401 ? 'Unable to load profile details. Please logout and login again.' : 'Failed to load profile.';
          setError(msg);
          return;
        }
        const data: ProfileResponse = await r.json();
        setProfile(data);
        setName(data.name || '');
        setEmail(data.email || '');
        setError(null);
      } catch (e) {
        setError('Failed to load profile.');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const saveProfile = async () => {
    setSuccess(null); setError(null);
    const r = await fetch('/api/profile', { method: 'PUT', headers: { 'content-type': 'application/json' }, credentials: 'include', body: JSON.stringify({ name, email }) });
    const data = await r.json().catch(() => ({}));
    if (!r.ok || data?.message) { setError(data?.message || 'Failed to update profile'); return; }
    setSuccess('Profile updated');
    setProfile((p) => ({ ...(p || {}), name, email }));
  };

  const changePassword = async () => {
    setPwdMsg(null);
    const r = await fetch('/api/profile/password', { method: 'PUT', headers: { 'content-type': 'application/json' }, credentials: 'include', body: JSON.stringify({ currentPassword, newPassword }) });
    const data = await r.json().catch(() => ({}));
    if (!r.ok || data?.message) { setPwdMsg(data?.message || 'Failed to change password'); return; }
    setPwdMsg('Password changed');
    setCurrentPassword(''); setNewPassword('');
  };

  if (loading) {
    return (
      <section className="py-20 min-h-screen relative z-10">
        <div className="flex items-center justify-center min-h-[60vh]">
          <div className="text-center bg-black/40 backdrop-blur-lg p-8 rounded-2xl border border-white/30">
            <div className="w-8 h-8 border-2 border-cyan-300/30 border-t-cyan-300 rounded-full animate-spin mx-auto mb-4" />
            <p className="text-white/90">Loading profile...</p>
          </div>
        </div>
      </section>
    );
  }

  if (authenticated === false) {
    return (
      <section className="py-20 min-h-screen relative z-10">
        <div className="flex items-center justify-center min-h-[60vh]">
          <div className="text-center bg-black/40 backdrop-blur-lg p-8 rounded-2xl border border-white/30">
            <IconAlertCircle className="w-10 h-10 text-red-400 mx-auto mb-2" />
            <p className="text-white/90">Please log in to view your profile.</p>
            <div className="mt-4">
              <a href="/login" className="inline-block px-4 py-2 bg-cyan-500/30 hover:bg-cyan-500/40 text-cyan-100 font-bold rounded-xl border-2 border-cyan-400/40">Go to Login</a>
            </div>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="py-20 min-h-screen relative z-10">
      <div className="max-w-3xl mx-auto px-4">
        <div className="text-center mb-8 bg-black/30 backdrop-blur-md p-6 rounded-2xl border border-white/30">
          <h1 className="text-4xl font-extrabold text-white mb-2">My Profile</h1>
          <p className="text-white/80">View and update your account information</p>
        </div>

        {/* Profile form */}
        <div className="bg-black/50 backdrop-blur-xl rounded-2xl p-8 border-2 border-white/30 mb-8">
          {error && (
            <div className="mb-4 text-sm text-red-300">{error}</div>
          )}
          <div className="grid md:grid-cols-2 gap-6">
            <div>
              <label className="text-white/90 text-sm font-medium flex items-center gap-2 mb-2"><IconUser className="w-5 h-5 text-cyan-300" />Name</label>
              <input value={name} onChange={(e) => setName(e.target.value)} className="w-full px-4 py-2 bg-black/70 border border-white/20 rounded text-white focus:outline-none focus:border-cyan-400/60" />
            </div>
            <div>
              <label className="text-white/90 text-sm font-medium flex items-center gap-2 mb-2"><IconMail className="w-5 h-5 text-cyan-300" />Email</label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="w-full px-4 py-2 bg-black/70 border border-white/20 rounded text-white focus:outline-none focus:border-cyan-400/60" />
            </div>
          </div>
          <div className="mt-6 flex gap-3">
            <button onClick={saveProfile} className="bg-cyan-500/30 hover:bg-cyan-500/40 text-cyan-100 font-bold px-6 py-2 rounded-xl border-2 border-cyan-400/40">Save</button>
            {success && <span className="text-green-300 flex items-center gap-2"><IconCheck className="w-5 h-5" /> {success}</span>}
            {success == null && error && authenticated && <span className="text-red-300">{error}</span>}
          </div>
        </div>

        {/* Password */}
        <div className="bg-black/50 backdrop-blur-xl rounded-2xl p-8 border-2 border-white/30">
          <h2 className="text-white font-bold text-xl mb-4 flex items-center gap-2"><IconLock className="w-5 h-5 text-cyan-300" />Change Password</h2>
          <div className="grid md:grid-cols-2 gap-6">
            <div>
              <label className="text-white/90 text-sm font-medium mb-2 block">Current Password</label>
              <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} className="w-full px-4 py-2 bg-black/70 border border-white/20 rounded text-white focus:outline-none focus:border-cyan-400/60" />
            </div>
            <div>
              <label className="text-white/90 text-sm font-medium mb-2 block">New Password</label>
              <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} className="w-full px-4 py-2 bg-black/70 border border-white/20 rounded text-white focus:outline-none focus:border-cyan-400/60" />
            </div>
          </div>
          <div className="mt-6 flex gap-3">
            <button onClick={changePassword} className="bg-cyan-500/30 hover:bg-cyan-500/40 text-cyan-100 font-bold px-6 py-2 rounded-xl border-2 border-cyan-400/40">Update Password</button>
            {pwdMsg && <span className="text-white/90">{pwdMsg}</span>}
          </div>
        </div>
      </div>
    </section>
  );
}
