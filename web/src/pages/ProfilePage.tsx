import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
  const { user } = useAuth();

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <h1 className="text-2xl font-black text-brand-navy dark:text-white">Profile</h1>
      <div className="glass-card p-8 text-center">
        <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-brand-navy text-2xl font-bold text-white">
          {user?.display_name?.[0]?.toUpperCase() ?? 'D'}
        </div>
        <h2 className="mt-4 text-xl font-bold">{user?.display_name}</h2>
        <p className="text-slate-500">{user?.email}</p>
        <p className="mt-4 text-sm text-slate-400">OrthofinixAI • STAR Summit Edition</p>
      </div>
      <div className="glass-card space-y-3 p-6 text-sm">
        <p><strong>Role:</strong> Clinician / Student</p>
        <p><strong>Platform:</strong> Web + Android</p>
        <p><strong>AI Engine:</strong> On-device + Cloud hybrid</p>
      </div>
    </div>
  );
}
