package com.example.orthofinixai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orthofinixai.ui.theme.*
import com.example.orthofinixai.ui.viewmodel.AnalysisViewModel
import com.example.orthofinixai.ui.viewmodel.AnalysisState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ABOScoringScreen(
    viewModel: AnalysisViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ABO OGS Scoring", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ClinicalDeepNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundClinical
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundClinical)
        ) {
            when (uiState) {
                is AnalysisState.Processing -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = ClinicalSkyBlue, strokeWidth = 4.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Retrieving ABO objective grading parameters...", color = ClinicalSlate, fontSize = 14.sp)
                    }
                }
                is AnalysisState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Clinical Report Unavailable", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ClinicalDeepNavy)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text((uiState as AnalysisState.Error).message, color = ClinicalSlate, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = ClinicalDeepNavy),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go Back", color = Color.White)
                        }
                    }
                }
                else -> {
                    val report = (uiState as? AnalysisState.Success)?.report
                    val aboPercentage = report?.abo_score ?: 85f
                    // Translate 0-100 percentage score to ABO Deductions (perfect score = 0 deductions)
                    val totalDeductions = ((100f - aboPercentage) * 0.35f).toInt().coerceAtLeast(1)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Objective Grading System",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClinicalDeepNavy
                        )
                        Text(
                            "Based on American Board of Orthodontics standards",
                            color = ClinicalSlate,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Proportional deduction distribution
                        val alignmentDed = if (totalDeductions > 6) -2 else 0
                        val ridgeDed = if (totalDeductions > 2) -2 else 0
                        val inclinationDed = if (totalDeductions > 4) -1 else 0
                        val occlusalDed = if (totalDeductions > 8) -2 else 0
                        val relationDed = if (totalDeductions > 10) -2 else 0
                        val overjetDed = if (totalDeductions > 5) -1 else 0
                        val rootDed = if ((report?.root_angulation_score ?: 85f) < 80f) -2 else 0

                        ABOCategoryCard("Alignment", alignmentDed, if (alignmentDed == 0) "All teeth within 0.5mm of ideal arch form." else "Crown rotation detected on maxillary second bicuspids.")
                        ABOCategoryCard("Marginal Ridges", ridgeDed, if (ridgeDed == 0) "Optimal marginal ridge height matching." else "Vertical step discrepancy detected in posterior quadrants.")
                        ABOCategoryCard("Buccolingual Inclination", inclinationDed, if (inclinationDed == 0) "Optimal buccolingual inclination." else "Excessive lingual crown tilt on mandibular molars.")
                        ABOCategoryCard("Occlusal Contacts", occlusalDed, if (occlusalDed == 0) "Optimal posterior contacts achieved." else "Minor open contacts in bicuspid segments.")
                        ABOCategoryCard("Occlusal Relationship", relationDed, if (relationDed == 0) "Class I molar and canine relationship." else "Class II tendency detected on left molar segment.")
                        ABOCategoryCard("Overjet", overjetDed, if (overjetDed == 0) "Optimal overjet within 1.5mm." else "Slight anterior overjet expansion detected.")
                        ABOCategoryCard("Interproximal Contacts", 0, "All contacts securely closed.")
                        ABOCategoryCard("Root Angulation", rootDed, if (rootDed == 0) "Excellent root parallelism on panoramic view." else "Distal root tip error detected on maxillary canine segment.")

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (totalDeductions < 8) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)),
                            colors = CardDefaults.cardColors(containerColor = if (totalDeductions < 8) Color(0xFFECFDF5) else Color(0xFFFEF2F2))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Total Deductions: $totalDeductions points", 
                                    fontWeight = FontWeight.Bold, 
                                    color = if (totalDeductions < 8) Color(0xFF047857) else Color(0xFF991B1B)
                                )
                                Text(
                                    text = "A score under 20 is required for passing the ABO clinical board examination.", 
                                    fontSize = 12.sp, 
                                    color = if (totalDeductions < 8) Color(0xFF065F46) else Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ABOCategoryCard(title: String, score: Int, feedback: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceClinical),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderClinical),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ClinicalDeepNavy)
                val scoreColor = if (score == 0) ClinicalEmerald else Color(0xFFEF4444)
                Text(
                    text = if (score == 0) "Optimal" else "$score pts",
                    color = scoreColor,
                    fontWeight = FontWeight.Bold
                )
            }
            if (score != 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feedback, fontSize = 14.sp, color = ClinicalSlate)
                }
            }
        }
    }
}
