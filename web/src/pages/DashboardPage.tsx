import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Upload, History, Sparkles, TrendingUp } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-8">
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="text-3xl font-black text-brand-navy dark:text-white">Clinical Dashboard</h1>
        <p className="text-slate-500">AI orthodontic finishing assessment — {user?.display_name}</p>
      </motion.div>

      <div className="grid gap-4 md:grid-cols-4">
        {[
          { label: 'Finishing Index', value: '91%', color: 'text-brand-teal' },
          { label: 'Cases Analyzed', value: '—', color: 'text-brand-blue' },
          { label: 'Avg Confidence', value: '94%', color: 'text-brand-green' },
          { label: 'Platform', value: 'Online', color: 'text-brand-navy dark:text-white' },
        ].map((s) => (
          <div key={s.label} className="glass-card p-5">
            <p className="text-xs text-slate-500">{s.label}</p>
            <p className={`text-2xl font-black ${s.color}`}>{s.value}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Link to="/upload" className="glass-card group p-8 transition hover:shadow-2xl">
          <Upload className="text-brand-blue" size={32} />
          <h3 className="mt-4 text-xl font-bold">New Analysis</h3>
          <p className="mt-2 text-sm text-slate-500">Upload intraoral or OPG images for AI assessment</p>
        </Link>
        <Link to="/upload?demo=1" className="glass-card group border-2 border-brand-teal/30 p-8">
          <Sparkles className="text-brand-teal" size={32} />
          <h3 className="mt-4 text-xl font-bold">Try Demo</h3>
          <p className="mt-2 text-sm text-slate-500">One-click STAR Summit showcase — works offline</p>
        </Link>
        <Link to="/history" className="glass-card p-8">
          <History className="text-brand-navy dark:text-white" size={32} />
          <h3 className="mt-4 text-xl font-bold">Report History</h3>
          <p className="mt-2 text-sm text-slate-500">View saved clinical reports</p>
        </Link>
        <div className="glass-card p-8">
          <TrendingUp className="text-brand-green" size={32} />
          <h3 className="mt-4 text-xl font-bold">Clinical Standards</h3>
          <p className="mt-2 text-sm text-slate-500">Andrews Six Keys • ABO grading • Explainable AI</p>
        </div>
      </div>
    </div>
  );
}
