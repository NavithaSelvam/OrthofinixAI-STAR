import { motion, AnimatePresence } from 'framer-motion';

const STAGES = [
  'Uploading image…',
  'Detecting landmarks…',
  'Running clinical analysis…',
  'Generating orthodontic report…',
  'Completed',
];

export function AIProcessingOverlay({
  active,
  stage,
}: {
  active: boolean;
  stage: number;
}) {
  return (
    <AnimatePresence>
      {active && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-sm"
        >
          <div className="glass-card mx-4 max-w-md p-8 text-center">
            <div className="relative mx-auto mb-6 h-40 w-40 overflow-hidden rounded-2xl border-2 border-brand-blue/50">
              <div className="absolute inset-0 bg-gradient-to-b from-brand-blue/20 to-transparent" />
              <motion.div
                className="absolute left-0 right-0 h-1 bg-brand-teal shadow-lg shadow-brand-teal/50"
                animate={{ top: ['0%', '100%', '0%'] }}
                transition={{ duration: 2, repeat: Infinity }}
              />
              <div className="flex h-full items-center justify-center text-4xl">🦷</div>
            </div>
            <h3 className="text-lg font-bold text-brand-navy dark:text-white">AI Analysis in Progress</h3>
            <p className="mt-2 text-sm text-slate-500">{STAGES[Math.min(stage, STAGES.length - 1)]}</p>
            <div className="mt-4 flex justify-center gap-1">
              {STAGES.slice(0, -1).map((_, i) => (
                <div
                  key={i}
                  className={`h-2 w-8 rounded-full transition ${
                    i <= stage ? 'bg-brand-blue' : 'bg-slate-200 dark:bg-slate-700'
                  }`}
                />
              ))}
            </div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
