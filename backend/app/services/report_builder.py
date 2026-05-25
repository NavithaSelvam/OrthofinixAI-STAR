"""Maps AI engine output to STAR Summit report format."""
import json
import uuid
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from sqlalchemy.orm import Session

from app.db.orm_models import AnalysisReport
from app.services.ai_engine import ai_engine


def _extract_metrics(result: dict) -> Dict[str, Any]:
    details = result.get("details") or {}
    lateral = details.get("overjet_overbite") or {}
    andrews = details.get("andrews_details") or {}

    overjet = lateral.get("overjet_mm", lateral.get("overjet", 2.5))
    overbite = lateral.get("overbite_percent", lateral.get("overbite", 28.0))
    midline = andrews.get("midline_discrepancy_mm", 0.8)

    finishing = (
        result.get("andrews_score", 80) * 0.35
        + result.get("arch_symmetry_score", 80) * 0.25
        + result.get("root_angulation_score", 80) * 0.2
        + (100 - min(result.get("abo_score", 15), 40)) * 0.2
    )

    return {
        "midline_deviation_mm": round(float(midline), 1),
        "overjet_mm": round(float(overjet), 1),
        "overbite_percent": round(float(overbite), 1),
        "finishing_score": round(finishing, 1),
        "alignment_score": round(result.get("arch_symmetry_score", 82), 1),
        "molar_right": andrews.get("molar_right_class", "Class I"),
        "molar_left": andrews.get("molar_left_class", "Class I"),
        "occlusion_summary": lateral.get("summary", "Acceptable anterior relationship."),
        "warnings": details.get("warnings", []),
        "conflicts": details.get("conflicts", []),
    }


def build_report_from_ai(
    db: Session,
    user_id: str,
    image_bytes: bytes,
    patient_name: str,
    image_url: str,
    view_type: str = "frontal",
) -> AnalysisReport:
    result = ai_engine.analyze_image(image_bytes, view_type=view_type)
    metrics = _extract_metrics(result)
    report_id = str(uuid.uuid4())

    report = AnalysisReport(
        id=report_id,
        user_id=user_id,
        patient_name=patient_name,
        image_url=image_url,
        view_type=view_type,
        status="completed",
        finishing_score=metrics["finishing_score"],
        alignment_score=metrics["alignment_score"],
        confidence_score=float(result.get("confidence_score", 0.85)) * 100,
        midline_deviation_mm=metrics["midline_deviation_mm"],
        overjet_mm=metrics["overjet_mm"],
        overbite_percent=metrics["overbite_percent"],
        abo_score=float(result.get("abo_score", 12)),
        andrews_score=float(result.get("andrews_score", 84)),
        prediction=result.get("prediction", "Analysis complete."),
        recommendations_json=json.dumps(result.get("recommendations", [])),
        metrics_json=json.dumps({**metrics, "details": result.get("details", {})}),
        created_at=datetime.now(timezone.utc),
    )
    db.add(report)
    db.commit()
    db.refresh(report)
    return report


def build_demo_report(db: Session, user_id: str) -> AnalysisReport:
    """Deterministic demo report for STAR Summit offline showcase."""
    demo_metrics = {
        "midline_deviation_mm": 0.6,
        "overjet_mm": 2.1,
        "overbite_percent": 28.0,
        "finishing_score": 91.2,
        "alignment_score": 88.5,
        "molar_right": "Class I (0.8 mm)",
        "molar_left": "Class I (1.1 mm)",
        "occlusion_summary": "Finishing quality within acceptable clinical range.",
        "warnings": [],
        "conflicts": [],
    }
    recommendations = [
        "Tooth 11 torque inclination deviates by +4°.",
        "Overjet measured at 2.1 mm.",
        "Left molar relationship classified as Class I.",
        "Root uprighting suggested for tooth 23 (9° deviation).",
        "Curve of Spee depth: 1.4 mm — favorable flat plane.",
    ]
    report = AnalysisReport(
        id=str(uuid.uuid4()),
        user_id=user_id,
        patient_name="Demo Patient — STAR Summit",
        image_url="/demo/sample-intraoral.jpg",
        view_type="frontal",
        status="completed",
        finishing_score=91.2,
        alignment_score=88.5,
        confidence_score=94.0,
        midline_deviation_mm=0.6,
        overjet_mm=2.1,
        overbite_percent=28.0,
        abo_score=8.0,
        andrews_score=89.0,
        prediction="Demo analysis: orthodontic finishing assessment complete.",
        recommendations_json=json.dumps(recommendations),
        metrics_json=json.dumps(demo_metrics),
        created_at=datetime.now(timezone.utc),
    )
    db.add(report)
    db.commit()
    db.refresh(report)
    return report


def report_to_response(report: AnalysisReport):
    from app.models.summit_schemas import AnalysisReportResponse
    import json as _json

    return AnalysisReportResponse(
        id=report.id,
        patient_name=report.patient_name,
        image_url=report.image_url,
        view_type=report.view_type,
        status=report.status,
        finishing_score=report.finishing_score,
        alignment_score=report.alignment_score,
        confidence_score=report.confidence_score,
        midline_deviation_mm=report.midline_deviation_mm,
        overjet_mm=report.overjet_mm,
        overbite_percent=report.overbite_percent,
        abo_score=report.abo_score,
        andrews_score=report.andrews_score,
        prediction=report.prediction,
        recommendations=_json.loads(report.recommendations_json or "[]"),
        metrics=_json.loads(report.metrics_json or "{}"),
        created_at=report.created_at,
    )
