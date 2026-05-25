import { useCallback, useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { analysisApi } from '../lib/api';
import { DEMO_REPORT } from '../lib/demo';
import { AIProcessingOverlay } from '../components/AIProcessingOverlay';

// Simple dropzone without extra package - use native
function useFileDrop(onFile: (f: File) => void) {
  const onDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      const f = e.dataTransfer.files[0];
      if (f?.type.startsWith('image/')) onFile(f);
    },
    [onFile]
  );
  return onDrop;
}

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [patientName, setPatientName] = useState('Patient');
  const [viewType, setViewType] = useState('frontal');
  const [processing, setProcessing] = useState(false);
  const [stage, setStage] = useState(0);
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const isDemo = params.get('demo') === '1';

  useEffect(() => {
    if (isDemo) runDemo();
  }, [isDemo]);

  const onFile = (f: File) => {
    setFile(f);
    setPreview(URL.createObjectURL(f));
  };

  const onDragOver = useFileDrop(onFile);

  const runDemo = async () => {
    setProcessing(true);
    for (let i = 0; i < 5; i++) {
      setStage(i);
      await new Promise((r) => setTimeout(r, 600));
    }
    try {
      const { data } = await analysisApi.analyzeDemo();
      sessionStorage.setItem('last_report', JSON.stringify(data));
      navigate(`/results/${data.id}`);
    } catch {
      sessionStorage.setItem('last_report', JSON.stringify(DEMO_REPORT));
      navigate(`/results/${DEMO_REPORT.id}`);
    } finally {
      setProcessing(false);
    }
  };

  const runAnalysis = async () => {
    if (!file && !isDemo) {
      toast.error('Please upload an image');
      return;
    }
    setProcessing(true);
    try {
      setStage(0);
      await new Promise((r) => setTimeout(r, 400));
      setStage(1);
      const { data: upload } = await analysisApi.upload(file!);
      setStage(2);
      await new Promise((r) => setTimeout(r, 500));
      setStage(3);
      const { data: report } = await analysisApi.analyze(upload.upload_id, patientName, viewType);
      setStage(4);
      sessionStorage.setItem('last_report', JSON.stringify(report));
      toast.success('Analysis complete!');
      navigate(`/results/${report.id}`);
    } catch {
      toast.error('Backend unavailable — using demo report');
      sessionStorage.setItem('last_report', JSON.stringify(DEMO_REPORT));
      navigate(`/results/${DEMO_REPORT.id}`);
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-black text-brand-navy dark:text-white">Upload & Analyze</h1>
      <AIProcessingOverlay active={processing} stage={stage} />

      <div
        onDragOver={(e) => e.preventDefault()}
        onDrop={onDragOver}
        className="glass-card flex min-h-[220px] cursor-pointer flex-col items-center justify-center border-2 border-dashed border-brand-blue/30 p-8 transition hover:border-brand-blue"
        onClick={() => document.getElementById('file-input')?.click()}
      >
        <input id="file-input" type="file" accept="image/*" className="hidden" onChange={(e) => e.target.files?.[0] && onFile(e.target.files[0])} />
        {preview ? (
          <img src={preview} alt="Preview" className="max-h-48 rounded-xl object-contain" />
        ) : (
          <>
            <p className="text-lg font-semibold">Drag & drop clinical image</p>
            <p className="text-sm text-slate-500">Intraoral photo or OPG radiograph</p>
          </>
        )}
      </div>

      <div className="glass-card space-y-4 p-6">
        <input className="input-field" placeholder="Patient name" value={patientName} onChange={(e) => setPatientName(e.target.value)} />
        <select className="input-field" value={viewType} onChange={(e) => setViewType(e.target.value)}>
          <option value="frontal">Frontal View</option>
          <option value="lateral">Lateral View</option>
          <option value="opg">OPG Radiograph</option>
        </select>
        <div className="flex gap-3">
          <button onClick={runAnalysis} className="btn-primary flex-1" disabled={processing}>
            Run AI Analysis
          </button>
          <button onClick={runDemo} className="btn-outline flex-1" disabled={processing}>
            Try Demo
          </button>
        </div>
      </div>
    </div>
  );
}
