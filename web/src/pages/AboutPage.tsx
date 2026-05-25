import { motion } from 'framer-motion';

export default function AboutPage() {
  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="text-3xl font-black text-brand-navy dark:text-white">About OrthofinixAI</h1>
        <p className="mt-2 text-slate-500">Final Year Project — STAR Summit 2026</p>
      </motion.div>

      <div className="glass-card space-y-4 p-8">
        <h2 className="text-xl font-bold">Mission</h2>
        <p className="text-slate-600 dark:text-slate-300">
          OrthofinixAI is an AI-powered orthodontic finishing assessment platform that helps clinicians
          and students evaluate case completion quality using explainable clinical metrics aligned with
          Andrews&apos; Six Keys and ABO grading concepts.
        </p>
        <h2 className="text-xl font-bold">Technology</h2>
        <ul className="list-inside list-disc space-y-1 text-slate-600 dark:text-slate-300">
          <li>React + TypeScript web application</li>
          <li>FastAPI cloud backend with JWT authentication</li>
          <li>Android app with on-device TensorFlow Lite inference</li>
          <li>Clinical rule engine with landmark geometry</li>
          <li>Hybrid offline demo mode for live presentations</li>
        </ul>
        <h2 className="text-xl font-bold">Clinical Outputs</h2>
        <p className="text-slate-600 dark:text-slate-300">
          FDI tooth-specific findings, millimeter measurements, degree deviations, molar classification,
          midline discrepancy, curve of Spee, root uprighting suggestions, and confidence-validated reports.
        </p>
      </div>
    </div>
  );
}
