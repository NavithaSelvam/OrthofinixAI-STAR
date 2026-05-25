import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authApi, User } from '../lib/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string) => Promise<void>;
  logout: () => void;
  setDemoUser: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const t = localStorage.getItem('orthofinix_token');
    const u = localStorage.getItem('orthofinix_user');
    if (t && u) {
      setToken(t);
      setUser(JSON.parse(u));
    }
    setLoading(false);
  }, []);

  const persist = (t: string, u: User) => {
    localStorage.setItem('orthofinix_token', t);
    localStorage.setItem('orthofinix_user', JSON.stringify(u));
    setToken(t);
    setUser(u);
  };

  const login = async (email: string, password: string) => {
    const { data } = await authApi.login(email, password);
    persist(data.access_token, data.user);
  };

  const register = async (email: string, password: string, name: string) => {
    const { data } = await authApi.register(email, password, name);
    persist(data.access_token, data.user);
  };

  const logout = () => {
    localStorage.removeItem('orthofinix_token');
    localStorage.removeItem('orthofinix_user');
    setToken(null);
    setUser(null);
  };

  const setDemoUser = () => {
    const demo: User = { id: 'demo', email: 'demo@orthofinix.ai', display_name: 'Demo Clinician' };
    persist('demo-token', demo);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, setDemoUser, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
