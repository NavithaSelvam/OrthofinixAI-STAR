import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Sparkles, Shield, Zap, ArrowRight } from 'lucide-react';

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-hero-gradient text-white">
      <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-6">
        <div className="flex items-center gap-2 font-bold text-xl">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/20">O</div>
          OrthofinixAI
        </div>
        <div className="flex gap-3">
          <Link to="/login" className="rounded-xl px-4 py-2 text-sm font-medium hover:bg-white/10">
            Sign In
          </Link>
          <Link to="/register" className="rounded-xl bg-white px-4 py-2 text-sm font-bold text-brand-navy">
            Get Started
          </Link>
        </div>
      </nav>

      <section className="mx-auto max-w-6xl px-6 py-20 text-center">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1 text-sm">
            <Sparkles size={14} /> STAR Summit 2026 Ready
          </span>
          <h1 className="mt-6 text-4xl font-black leading-tight md:text-6xl">
            AI-Powered Orthodontic
            <br />
            Finishing Assessment
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-lg text-blue-100">
            Upload intraoral & radiograph images. Receive explainable clinical reports with Andrews Keys,
            overjet/overbite metrics, and treatment recommendations — in seconds.
          </p>
          <div className="mt-10 flex flex-wrap justify-center gap-4">
            <Link to="/register" className="inline-flex items-center gap-2 rounded-xl bg-white px-8 py-4 font-bold text-brand-navy shadow-xl">
              Start Free <ArrowRight size={18} />
            </Link>
            <Link to="/login" className="inline-flex items-center gap-2 rounded-xl border-2 border-white/40 px-8 py-4 font-bold hover:bg-white/10">
              Try Demo
            </Link>
          </div>
        </motion.div>

        <div className="mt-20 grid gap-6 md:grid-cols-3">
          {[
            { icon: Zap, title: 'Real-time AI', desc: 'Landmark detection & clinical rule engine' },
            { icon: Shield, title: 'Explainable', desc: 'FDI tooth numbers, mm, degrees, severity' },
            { icon: Sparkles, title: 'Demo Ready', desc: 'One-click showcase for live presentations' },
          ].map(({ icon: Icon, title, desc }, i) => (
            <motion.div
              key={title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 + i * 0.1 }}
              className="glass-card rounded-2xl p-6 text-left text-slate-900 dark:text-white"
            >
              <Icon className="text-brand-blue" />
              <h3 className="mt-3 font-bold">{title}</h3>
              <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{desc}</p>
            </motion.div>
          ))}
        </div>
      </section>
    </div>
  );
}
