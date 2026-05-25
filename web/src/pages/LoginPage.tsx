import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import { DEMO_REPORT } from '../lib/demo';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, setDemoUser } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await login(email, password);
      toast.success('Welcome back!');
      navigate('/dashboard');
    } catch {
      toast.error('Login failed. Check credentials or use Demo Mode.');
    } finally {
      setLoading(false);
    }
  };

  const tryDemo = () => {
    setDemoUser();
    sessionStorage.setItem('demo_report', JSON.stringify(DEMO_REPORT));
    toast.success('Demo mode activated');
    navigate('/dashboard');
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-hero-gradient p-4">
      <div className="glass-card w-full max-w-md p-8 text-slate-900 dark:text-white">
        <h1 className="text-2xl font-black text-brand-navy dark:text-white">Sign In</h1>
        <p className="mt-1 text-sm text-slate-500">OrthofinixAI Clinical Platform</p>
        <form onSubmit={handleLogin} className="mt-8 space-y-4">
          <input className="input-field" type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <input className="input-field" type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>
        <button onClick={tryDemo} className="btn-outline mt-4 w-full border-brand-teal text-brand-teal">
          Try Demo (STAR Summit)
        </button>
        <p className="mt-6 text-center text-sm">
          No account? <Link to="/register" className="font-semibold text-brand-blue">Register</Link>
        </p>
        <Link to="/" className="mt-4 block text-center text-sm text-slate-500 hover:underline">← Back to home</Link>
      </div>
    </div>
  );
}
