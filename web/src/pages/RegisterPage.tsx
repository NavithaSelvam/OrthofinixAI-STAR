import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await register(email, password, name || 'Doctor');
      toast.success('Account created!');
      navigate('/dashboard');
    } catch {
      toast.error('Registration failed. Email may already exist.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-hero-gradient p-4">
      <div className="glass-card w-full max-w-md p-8">
        <h1 className="text-2xl font-black text-brand-navy dark:text-white">Create Account</h1>
        <form onSubmit={handleRegister} className="mt-8 space-y-4">
          <input className="input-field" placeholder="Full Name" value={name} onChange={(e) => setName(e.target.value)} />
          <input className="input-field" type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <input className="input-field" type="password" placeholder="Password (min 6)" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={6} />
          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? 'Creating…' : 'Register'}
          </button>
        </form>
        <p className="mt-6 text-center text-sm">
          Have an account? <Link to="/login" className="font-semibold text-brand-blue">Sign In</Link>
        </p>
      </div>
    </div>
  );
}
