package com.example.orthofinixai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lightbulb
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
fun RecommendationsScreen(
    viewModel: AnalysisViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Recommendations") },
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
                        Text("Compiling clinical AI recommendations list...", color = TextGray, fontSize = 14.sp)
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
                        Text("No Recommendations Loaded", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
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
                    
                    // Parse real backend recommendations dynamically
                    val liveRecs = report?.recommendations?.mapIndexed { index, recString ->
                        val parts = recString.split(":")
                        val title = if (parts.size > 1) parts[0].trim() else "AI Correction Step #${index + 1}"
                        val desc = if (parts.size > 1) parts[1].trim() else recString
                        val step = if (desc.contains("wire", ignoreCase = true) || desc.contains("elastic", ignoreCase = true)) {
                            "Incorporate finishing detail or mechanic in current wire sequence."
                        } else {
                            "Evaluate clinical bracket/band alignment on next office visit."
                        }
                        Recommendation(
                            title = title,
                            description = desc,
                            step = step,
                            reference = "AI Engine Output"
                        )
                    } ?: emptyList()

                    val displayRecommendations = liveRecs.ifEmpty { sampleRecommendations }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Personalized Correction Steps",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "Suggested mechanics based on AI assessment",
                            color = TextGray,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayRecommendations) { recommendation ->
                                RecommendationCard(recommendation)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(rec: Recommendation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(12.dp))
                Text(rec.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(rec.description, fontSize = 14.sp, color = Color.Black)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider(color = Color(0xFFF3F4F6))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Build, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Corrective Step:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryGreen)
                    Text(rec.step, fontSize = 13.sp, color = TextGray)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Reference: ${rec.reference}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryGreen.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

data class Recommendation(
    val title: String,
    val description: String,
    val step: String,
    val reference: String
)

val sampleRecommendations = listOf(
    Recommendation(
        "Open Contact UR2-UR3",
        "A small space is detected between the upper right lateral incisor and canine.",
        "Use sliding mechanics with an elastomeric chain or adjust bracket position to close space.",
        "ABO Interproximal Contacts"
    ),
    Recommendation(
        "Marginal Ridge Discrepancy",
        "The marginal ridges of UR6 and UR7 are not leveled in the occlusal plane.",
        "Incorporate a vertical finishing bend in the archwire or use vertical elastics.",
        "ABO OGS Criterion 2"
    ),
    Recommendation(
        "Insufficient Lower Incisor Torque",
        "Lower incisors show more lingual inclination than Andrews' ideal.",
        "Consider bracket repositioning or adding labial root torque to the finishing wire.",
        "Andrews Key 3"
    ),
    Recommendation(
        "Curve of Spee Leveling",
        "The mandibular arch still exhibits a 1.5mm deep curve of Spee.",
        "Continue with a reverse curve of Spee wire or use bite opening mechanics.",
        "Raleigh-Williams Key 4"
    )
)
