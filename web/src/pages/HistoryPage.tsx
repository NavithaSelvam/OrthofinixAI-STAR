import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { analysisApi, HistoryItem } from '../lib/api';
import { DEMO_HISTORY } from '../lib/demo';

export default function HistoryPage() {
  const [items, setItems] = useState<HistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const load = () => {
    setLoading(true);
    analysisApi
      .history()
      .then(({ data }) => setItems(data))
      .catch(() => {
        setError(true);
        setItems(
          DEMO_HISTORY.map((r) => ({
            id: r.id,
            patient_name: r.patient_name,
            finishing_score: r.finishing_score,
            confidence_score: r.confidence_score,
            created_at: r.created_at,
            image_url: r.image_url,
          }))
        );
      })
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-black text-brand-navy dark:text-white">Report History</h1>
        {error && (
          <button onClick={load} className="btn-outline text-sm">
            Retry
          </button>
        )}
      </div>

      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-20 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="glass-card py-16 text-center">
          <p className="text-slate-500">No reports yet. Run your first analysis.</p>
          <Link to="/upload" className="btn-primary mt-4 inline-flex">
            New Analysis
          </Link>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((item, i) => (
            <motion.div
              key={item.id}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: i * 0.05 }}
            >
              <Link
                to={`/results/${item.id}`}
                onClick={() => sessionStorage.setItem('last_report', JSON.stringify(item))}
                className="glass-card flex items-center justify-between p-5 transition hover:shadow-lg"
              >
                <div>
                  <p className="font-bold">{item.patient_name}</p>
                  <p className="text-xs text-slate-500">
                    {new Date(item.created_at).toLocaleString()} • Score {item.finishing_score?.toFixed?.(0) ?? item.finishing_score}%
                  </p>
                </div>
                <span className="rounded-full bg-brand-teal/10 px-3 py-1 text-sm font-semibold text-brand-teal">
                  {item.confidence_score?.toFixed?.(0) ?? item.confidence_score}% conf.
                </span>
              </Link>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
