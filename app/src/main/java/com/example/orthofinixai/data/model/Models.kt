package com.example.orthofinixai.data.model

data class Case(
    val id: String,
    val patient_id: String,
    val notes: String? = null,
    val status: String,
    val created_at: String
)

data class ClinicalFindingDto(
    val category: String,
    val toothFdi: Int? = null,
    val measurement: String,
    val value: String,
    val ideal: String,
    val severity: String,
    val explanation: String
)

data class AIReport(
    val id: String,
    val case_id: String,
    val abo_score: Float,
    val arch_symmetry_score: Float,
    val root_angulation_score: Float,
    val andrews_score: Float,
    val recommendations: List<String>,
    val created_at: String,
    val confidence_score: Float = 0.85f,
    val overjet_mm: Float = 2.5f,
    val overbite_percent: Float = 30f,
    val overjet_status: String = "Normal",
    val overbite_status: String = "Normal",
    val molar_right_class: String = "Class I",
    val molar_left_class: String = "Class I",
    val midline_discrepancy_mm: Float = 0f,
    val curve_of_spee_mm: Float = 0f,
    val clinical_findings: List<ClinicalFindingDto> = emptyList(),
    val andrews_violations: List<String> = emptyList(),
    val low_confidence_warning: String? = null,
    val detected_teeth_count: Int = 0,
    val view_type: String = "frontal"
)
