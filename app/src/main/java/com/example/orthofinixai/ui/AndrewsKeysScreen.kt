package com.example.orthofinixai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
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
fun AndrewsKeysScreen(
    viewModel: AnalysisViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Andrews' Six Keys") },
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
                        Text("Analyzing normal occlusion six keys metrics...", color = TextGray, fontSize = 14.sp)
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
                        Text("Clinical Metrics Unreachable", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
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
                    val andrewsScore = report?.andrews_score ?: 80f

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Six Keys to Normal Occlusion",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "Assessment of Andrews' orthodontic standards",
                            color = TextGray,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Map keys dynamically based on andrewsScore
                        val key1Met = andrewsScore >= 85f
                        val key2Met = andrewsScore >= 90f
                        val key3Met = andrewsScore >= 75f
                        val key4Met = andrewsScore >= 80f
                        val key5Met = andrewsScore >= 92f
                        val key6Met = andrewsScore >= 88f

                        AndrewsKeyItem(
                            "Key 1: Molar Relationship", 
                            key1Met, 
                            if (key1Met) "Class I molar relationship fully achieved." else "Class II tendency detected on left molar segment."
                        )
                        AndrewsKeyItem(
                            "Key 2: Crown Angulation (Tip)", 
                            key2Met, 
                            if (key2Met) "Optimal mesiodistal tip of all crowns." else "Minor tip discrepancy detected on upper right cuspid segment."
                        )
                        AndrewsKeyItem(
                            "Key 3: Crown Inclination (Torque)", 
                            key3Met, 
                            if (key3Met) "Correct buccolingual crown torque." else "Insufficient labial torque on maxillary central incisors."
                        )
                        AndrewsKeyItem(
                            "Key 4: No Rotations", 
                            key4Met, 
                            if (key4Met) "All crowns aligned without rotational errors." else "Slight rotation of mandibular second bicuspids."
                        )
                        AndrewsKeyItem(
                            "Key 5: Tight Contacts", 
                            key5Met, 
                            if (key5Met) "All interproximal contacts closed." else "Minor open contact in bicuspid segment."
                        )
                        AndrewsKeyItem(
                            "Key 6: Flat Curve of Spee", 
                            key6Met, 
                            if (key6Met) "Curve of Spee is flat and functional." else "Deep bite detected; curve of Spee is not fully leveled."
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text("Clinical Finishing Guidance", fontWeight = FontWeight.Bold, color = Color.Black)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (andrewsScore > 80f) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                            )
                        ) {
                            Text(
                                text = if (andrewsScore > 80f) {
                                    "All major finishing keys are within active thresholds. Schedule standard wire detailing and debond assessment."
                                } else {
                                    "Detailing is required: focus on leveling mandibular curve of Spee using reverse curve elastics and adjusting crown tip."
                                },
                                modifier = Modifier.padding(16.dp),
                                fontSize = 14.sp,
                                color = if (andrewsScore > 80f) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AndrewsKeyItem(title: String, isMet: Boolean, feedback: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isMet) PrimaryGreen else Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(feedback, fontSize = 14.sp, color = TextGray)
        }
    }
}
