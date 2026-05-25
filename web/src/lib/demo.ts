import type { AnalysisReport } from './api';

/** Offline fallback for STAR Summit when backend is unavailable */
export const DEMO_REPORT: AnalysisReport = {
  id: 'demo-summit-001',
  patient_name: 'Demo Patient — STAR Summit',
  image_url: undefined,
  view_type: 'frontal',
  status: 'completed',
  finishing_score: 91.2,
  alignment_score: 88.5,
  confidence_score: 94,
  midline_deviation_mm: 0.6,
  overjet_mm: 2.1,
  overbite_percent: 28,
  abo_score: 8,
  andrews_score: 89,
  prediction: 'Demo analysis: orthodontic finishing assessment complete.',
  recommendations: [
    'Tooth 11 torque inclination deviates by +4°.',
    'Overjet measured at 2.1 mm.',
    'Left molar relationship classified as Class I.',
    'Root uprighting suggested for tooth 23 (9° deviation).',
    'Curve of Spee depth: 1.4 mm — favorable flat plane.',
  ],
  metrics: {
    molar_right: 'Class I (0.8 mm)',
    molar_left: 'Class I (1.1 mm)',
    occlusion_summary: 'Finishing quality within acceptable clinical range.',
  },
  created_at: new Date().toISOString(),
};

export const DEMO_HISTORY = [DEMO_REPORT];
