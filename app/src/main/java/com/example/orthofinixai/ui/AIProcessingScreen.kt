package com.example.orthofinixai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orthofinixai.R
import com.example.orthofinixai.ui.theme.*
import com.example.orthofinixai.ui.viewmodel.AnalysisState
import com.example.orthofinixai.ui.viewmodel.AnalysisViewModel
import com.example.orthofinixai.ui.viewmodel.SharedCaseViewModel

@Composable
fun AIProcessingScreen(
    sharedViewModel: SharedCaseViewModel,
    analysisViewModel: AnalysisViewModel,
    patientViewModel: com.example.orthofinixai.ui.viewmodel.PatientViewModel,
    onProcessingComplete: () -> Unit
) {
    val context = LocalContext.current
    val uiState by analysisViewModel.uiState.collectAsState()
    var analysisStarted by remember { mutableStateOf(false) }

    val progressValue = when (val state = uiState) {
        is AnalysisState.Processing -> state.progress
        is AnalysisState.Success -> 1f
        else -> 0.05f
    }

    val progressText = when (val state = uiState) {
        is AnalysisState.Processing -> state.message
        is AnalysisState.Success -> "Clinical report generated"
        is AnalysisState.Error -> "Error: ${state.message}"
        else -> "Preparing clinical data..."
    }

    LaunchedEffect(sharedViewModel.opgPhoto, sharedViewModel.patientName) {
        analysisViewModel.reset()
        analysisStarted = false

        val patientName = sharedViewModel.patientName ?: "Patient"
        val dob = sharedViewModel.dob ?: "01/01/2010"
        val gender = sharedViewModel.gender ?: "Male"
        val imageUri = sharedViewModel.opgPhoto

        val imageBytes = try {
            imageUri?.let { context.contentResolver.openInputStream(it)?.readBytes() }
        } catch (_: Exception) {
            null
        } ?: ByteArray(0)

        val viewType = if (imageBytes.isNotEmpty()) "opg" else "frontal"

        val caseId = "case_${System.currentTimeMillis()}"

        analysisViewModel.startAnalysis(
            caseId = caseId,
            patientName = patientName,
            dob = dob,
            gender = gender,
            imageUri = imageUri,
            imageBytes = imageBytes,
            viewType = viewType
        )
        analysisStarted = true
    }

    LaunchedEffect(uiState, analysisStarted) {
        if (!analysisStarted) return@LaunchedEffect
        when (uiState) {
            is AnalysisState.Success -> {
                kotlinx.coroutines.delay(400)
                onProcessingComplete()
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF0F7FF))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.orthofinix_logo),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                progress = { progressValue },
                modifier = Modifier.size(120.dp),
                color = BrandGreen,
                strokeWidth = 6.dp,
                trackColor = Color(0xFFE5E7EB),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${(progressValue * 100).toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "AI Clinical Analysis",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                progressText,
                fontSize = 15.sp,
                color = ClinicalSlate,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = BrandGreen,
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Secure Cloud AI Pipeline • Accurate Clinical Metrics",
                fontSize = 12.sp,
                color = BrandGray,
                textAlign = TextAlign.Center
            )

            if (uiState is AnalysisState.Error) {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = { analysisViewModel.reset() }) {
                    Text("Retry Analysis", color = BrandGreen)
                }
            }
        }
    }
}
