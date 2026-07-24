import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AlertTriangle, Download, ArrowLeft } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { analysisApi, AnalysisReport } from '../lib/api';
import { ScoreCircle } from '../components/ScoreCircle';

export default function ResultsPage() {
  const { id } = useParams();
  const [report, setReport] = useState<AnalysisReport | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const cached = sessionStorage.getItem('last_report');
    if (cached) {
      const parsed = JSON.parse(cached) as AnalysisReport;
      if (parsed.id === id) {
        setReport(parsed);
        setLoading(false);
        return;
      }
    }
    analysisApi
      .report(id!)
      .then(({ data }) => {
        // Map backend response to frontend interface
        const mappedReport: AnalysisReport = {
          id: data.id,
          patient_name: data.patient_name,
          image_url: data.image_url,
          view_type: data.view_type,
          status: data.status,
          finishing_score: data.finishing_score,
          alignment_score: data.alignment_score,
          confidence_score: data.confidence_score,
          midline_deviation_mm: data.midline_deviation_mm,
          overjet_mm: data.overjet_mm,
          overbite_percent: data.overbite_percent,
          abo_score: data.abo_score,
          andrews_score: data.andrews_score,
          prediction: data.prediction,
          recommendations: data.recommendations,
          metrics: data.metrics,
          created_at: data.created_at
        };
        setReport(mappedReport);
      })
      .catch(() => setReport(null))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="animate-pulse text-center py-20">Loading report…</div>;
  if (!report) return <div>Report not found</div>;

  const chartData = [
    { name: 'Finishing', value: report.finishing_score },
    { name: 'Alignment', value: report.alignment_score },
    { name: 'Andrews', value: report.andrews_score },
    { name: 'Confidence', value: report.confidence_score },
  ];

  const metrics = [
    { label: 'Midline Deviation', value: `${report.midline_deviation_mm} mm`, risk: report.midline_deviation_mm > 2 },
    { label: 'Overjet', value: `${report.overjet_mm} mm`, risk: report.overjet_mm > 4 },
    { label: 'Overbite', value: `${report.overbite_percent}%`, risk: report.overbite_percent > 40 },
    { label: 'ABO Penalty', value: `${report.abo_score}`, risk: report.abo_score > 20 },
  ];

  const exportPdf = () => {
    const text = [
      `OrthofinixAI Report — ${report.patient_name}`,
      `Finishing Score: ${report.finishing_score}`,
      ...report.recommendations,
    ].join('\n');
    const blob = new Blob([text], { type: 'text/plain' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `orthofinix-report-${report.id}.txt`;
    a.click();
    window.alert('Report downloaded');
  };

  return (
    <div className="space-y-8">
      <Link to="/history" className="inline-flex items-center gap-2 text-sm text-brand-blue">
        <ArrowLeft size={16} /> Back to history
      </Link>

      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="glass-card p-8">
        <div className="flex flex-wrap items-start justify-between gap-6">
          <div>
            <h1 className="text-2xl font-black text-brand-navy dark:text-white">{report.patient_name}</h1>
            <p className="text-slate-500">{report.prediction}</p>
            {report.confidence_score < 65 && (
              <div className="mt-3 flex items-center gap-2 rounded-lg bg-amber-50 px-3 py-2 text-amber-800 dark:bg-amber-900/30 dark:text-amber-200">
                <AlertTriangle size={16} />
                Detection confidence low. Please verify landmarks manually.
              </div>
            )}
          </div>
          <button onClick={exportPdf} className="btn-outline text-sm">
            <Download size={16} /> Export Report
          </button>
        </div>
      </motion.div>

      <div className="flex flex-wrap justify-center gap-8">
        <ScoreCircle score={report.finishing_score} label="Finishing Score" />
        <ScoreCircle score={report.confidence_score} label="AI Confidence" />
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        {metrics.map((m) => (
          <div
            key={m.label}
            className={`glass-card p-4 ${m.risk ? 'border-l-4 border-amber-500' : 'border-l-4 border-brand-teal'}`}
          >
            <p className="text-xs text-slate-500">{m.label}</p>
            <p className="text-xl font-bold">{m.value}</p>
          </div>
        ))}
      </div>

      <div className="glass-card p-6">
        <h3 className="font-bold">Metrics Overview</h3>
        <div className="mt-4 h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <XAxis dataKey="name" />
              <YAxis domain={[0, 100]} />
              <Tooltip />
              <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                {chartData.map((_, i) => (
                  <Cell key={i} fill={['#1A5296', '#2563EB', '#2BB673', '#76B82A'][i]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="glass-card p-6">
        <h3 className="font-bold text-brand-navy dark:text-white">Clinical Recommendations</h3>
        <ul className="mt-4 space-y-2">
          {report.recommendations.map((r, i) => (
            <li key={i} className="flex gap-2 text-sm">
              <span className="text-brand-teal">●</span> {r}
            </li>
          ))}
        </ul>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="glass-card p-6">
          <h3 className="font-bold">Landmark Visualization</h3>
          <div className="mt-4 flex h-40 items-center justify-center rounded-xl bg-slate-100 dark:bg-slate-800">
            <p className="text-sm text-slate-500">Occlusal plane • FDI landmarks • Segment overlay</p>
          </div>
        </div>
        <div className="glass-card p-6">
          <h3 className="font-bold">Clinical Notes</h3>
          <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
            Molar R: {(report.metrics as { molar_right?: string })?.molar_right || 'Class I'}
            <br />
            Molar L: {(report.metrics as { molar_left?: string })?.molar_left || 'Class I'}
            <br />
            {(report.metrics as { occlusion_summary?: string })?.occlusion_summary}
          </p>
        </div>
      </div>
    </div>
  );
}
