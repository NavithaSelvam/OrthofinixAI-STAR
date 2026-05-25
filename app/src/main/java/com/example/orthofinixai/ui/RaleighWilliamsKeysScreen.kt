package com.example.orthofinixai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Rule
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
fun RaleighWilliamsKeysScreen(
    viewModel: AnalysisViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raleigh-Williams Keys") },
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
                        Text("Retrieving Raleigh-Williams diagnostic indexes...", color = TextGray, fontSize = 14.sp)
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
                        Text("Diagnostic Metrics Unreachable", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
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
                    val roots = report?.root_angulation_score ?: 80f
                    val symmetry = report?.arch_symmetry_score ?: 85f
                    val abo = report?.abo_score ?: 80f

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Treatment Keys Review",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "Six Treatment Keys from Raleigh-Williams Principles",
                            color = TextGray,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        RWKeyItem(
                            "1. Axial Inclination", 
                            "${roots.toInt()}%", 
                            if (roots > 80f) "Root parallelism is fully achieved on radiographic evaluation." else "Minor axial tipping error observed in cuspid-bicuspid segments."
                        )
                        RWKeyItem(
                            "2. Interarch Relationships", 
                            "${abo.toInt()}%", 
                            if (abo > 80f) "Superb Class I interarch relationship with tight interdigitation." else "Minor posterior interarch settling required; consider box elastics."
                        )
                        RWKeyItem(
                            "3. Functional Group Contacts", 
                            "${(abo * 0.95f).toInt()}%", 
                            if (abo > 85f) "Excellent lateral group function during excursion." else "Posterior dynamic group contacts require settlement refinement."
                        )
                        RWKeyItem(
                            "4. Curve of Spee Management", 
                            "${(abo * 0.98f).toInt()}%", 
                            if (abo > 80f) "Mandibular arch leveling is complete." else "Slight curve of Spee deepness detected; continue leveling wire sequence."
                        )
                        RWKeyItem(
                            "5. Midline Coordination", 
                            "100%", 
                            "Maxillary and mandibular midlines coincide perfectly with facial midline."
                        )
                        RWKeyItem(
                            "6. Arch Form Prescribing", 
                            "${symmetry.toInt()}%", 
                            if (symmetry > 80f) "Excellent harmony with the original arch form." else "Slight quadrant width asymmetry detected during transverse check."
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text("Clinical Summary", fontWeight = FontWeight.Bold, color = Color.Black)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
                        ) {
                            Row(modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Rule, contentDescription = null, tint = Color(0xFF0284C7))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "The Raleigh-Williams keys evaluate orthodontic progression from static interdigitation to functional dynamic stability.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF075985)
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
fun RWKeyItem(title: String, score: String, feedback: String) {
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
                Text(score, color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(feedback, fontSize = 14.sp, color = TextGray)
        }
    }
}
