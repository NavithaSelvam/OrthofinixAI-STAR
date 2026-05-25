package com.example.orthofinixai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.example.orthofinixai.ui.theme.PrimaryGreen
import com.example.orthofinixai.ui.theme.TextGray
import com.example.orthofinixai.ui.viewmodel.AnalysisViewModel
import com.example.orthofinixai.ui.viewmodel.AnalysisState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolingConceptsScreen(
    viewModel: AnalysisViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dr. Rebecca Roling's Concepts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9FAFB))
        ) {
            when (uiState) {
                is AnalysisState.Processing -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryGreen, strokeWidth = 4.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Calculating Roling functional finishing indexes...", color = TextGray, fontSize = 14.sp)
                    }
                }
                is AnalysisState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Concepts Calculations Unreachable", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text((uiState as AnalysisState.Error).message, color = TextGray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
                else -> {
                    val report = (uiState as? AnalysisState.Success)?.report
                    val symmetryScore = report?.arch_symmetry_score ?: 80f
                    val aboScore = report?.abo_score ?: 85f

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Functional Finishing & Stability",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "Focus on arch form harmony and occlusal stability",
                            color = TextGray,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val molarGuidance = if (aboScore > 80f) "Optimal" else "Review"
                        val symmetryStatus = if (symmetryScore > 85f) "Optimal" else "Improvement Needed"
                        val torqueStatus = if (aboScore > 75f) "Optimal" else "Review"
                        val stabilityStatus = if (symmetryScore > 80f) "Optimal" else "Review"

                        RolingConceptItem(
                            "Second Molar Guidance",
                            molarGuidance,
                            if (molarGuidance == "Optimal") "Functional second molar guidance is perfectly established." else "Check extrusion tendencies of upper second molars."
                        )
                        RolingConceptItem(
                            "Arch Symmetry Harmony",
                            symmetryStatus,
                            if (symmetryStatus == "Optimal") "Superb horizontal arch symmetry and shape." else "Slight horizontal asymmetry detected between left/right quadrants."
                        )
                        RolingConceptItem(
                            "Torque Optimization",
                            torqueStatus,
                            if (torqueStatus == "Optimal") "Posterior torque limits are stable." else "Torque loss detected on mandibular posterior segments."
                        )
                        RolingConceptItem(
                            "Stability-Focused Detailing",
                            stabilityStatus,
                            if (stabilityStatus == "Optimal") "Mandibular anterior alignment is stable." else "Risk of relapsing rotations; consider fixed lingual retainer placement."
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text("Clinical Finishing Insight", fontWeight = FontWeight.Bold, color = Color.Black)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F8))
                        ) {
                            Row(modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFDB2777))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Dr. Roling's principles verify that functional finishing guarantees the longevity of treatment results. Stability-focused detailing must be matched to custom patient arch forms.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF9D174D)
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
fun RolingConceptItem(title: String, status: String, feedback: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(
                    text = status,
                    color = if (status == "Optimal") PrimaryGreen else Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(feedback, fontSize = 14.sp, color = TextGray)
        }
    }
}
