import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_URL || 'https://orthofinixai-backend.onrender.com';

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('orthofinix_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export interface User {
  id: string;
  email: string;
  display_name: string;
}

export interface HistoryItem {
  id: string;
  patient_name: string;
  finishing_score: number;
  confidence_score: number;
  created_at: string;
  image_url?: string;
  user_id?: string;
}

export interface AnalysisReport {
  id: string;
  patient_name: string;
  image_url?: string;
  view_type: string;
  status: string;
  finishing_score: number;
  alignment_score: number;
  confidence_score: number;
  midline_deviation_mm: number;
  overjet_mm: number;
  overbite_percent: number;
  abo_score: number;
  andrews_score: number;
  prediction: string;
  recommendations: string[];
  metrics: Record<string, unknown>;
  created_at: string;
}

export const authApi = {
  login: (email: string, password: string) =>
    api.post<{ access_token: string; user: User }>('/auth/login', { email, password }),
  register: (email: string, password: string, display_name: string) =>
    api.post<{ access_token: string; user: User }>('/auth/register', {
      email,
      password,
      display_name,
    }),
};

export const analysisApi = {
  upload: (file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    return api.post<{ upload_id: string; image_url: string }>('/analysis/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  analyze: (uploadId: string, patientName: string, viewType: string) => {
    const fd = new FormData();
    fd.append('upload_id', uploadId);
    fd.append('patient_name', patientName);
    fd.append('view_type', viewType);
    return api.post<AnalysisReport>('/analysis/analyze', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  history: () => api.get<HistoryItem[]>('/analysis/history'),
  report: (id: string) => api.get<AnalysisReport>(`/analysis/report/${id}`),
};
