package com.example.orthofinixai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orthofinixai.R
import com.example.orthofinixai.ui.theme.*
import com.example.orthofinixai.data.AnalysisSession
import com.example.orthofinixai.ui.viewmodel.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun AIProcessingScreen(
    sharedViewModel: SharedCaseViewModel,
    analysisViewModel: AnalysisViewModel,
    patientViewModel: PatientViewModel,
    onProcessingComplete: () -> Unit
) {
    val context = LocalContext.current
    val uiState by analysisViewModel.uiState.collectAsState()

    var progressText by remember { mutableStateOf("Preparing clinical data...") }
    var progressValue by remember { mutableFloatStateOf(0.05f) }

    LaunchedEffect(Unit) {
        val patientName = sharedViewModel.patientName ?: "Patient"
        val dob = sharedViewModel.dob ?: "01/01/2010"
        val gender = sharedViewModel.gender ?: "Male"

        progressText = "Registering case profile..."
        progressValue = 0.1f

        patientViewModel.addPatient(patientName, dob, gender, "On-device analysis") { patientId ->
            val imageUri = sharedViewModel.opgPhoto
            val imageBytes = try {
                imageUri?.let { context.contentResolver.openInputStream(it)?.readBytes() }
            } catch (_: Exception) { null }

            AnalysisSession.imageUri = imageUri
            progressText = "Running on-device AI (TensorFlow Lite)..."
            progressValue = 0.35f
            analysisViewModel.uploadAndAnalyze(
                caseId = patientId,
                imageBytes = imageBytes ?: ByteArray(0),
                patientName = patientName,
                dob = dob,
                gender = gender,
                imageUri = imageUri,
                viewType = if (imageBytes != null) "opg" else "frontal"
            )
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AnalysisState.Processing -> {
                progressText = "Segmenting teeth • Detecting landmarks • Computing Andrews Keys..."
                progressValue = 0.7f
            }
            is AnalysisState.Success -> {
                progressText = "Clinical report generated"
                progressValue = 1f
                kotlinx.coroutines.delay(500)
                onProcessingComplete()
            }
            is AnalysisState.Error -> progressText = "Error: ${state.message}"
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF0F7FF))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
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
            Text("${(progressValue * 100).toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandNavy)

            Spacer(modifier = Modifier.height(32.dp))
            Text("AI Clinical Analysis", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
            Spacer(modifier = Modifier.height(8.dp))
            Text(progressText, fontSize = 15.sp, color = ClinicalSlate, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = BrandGreen,
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "100% on-device • No backend required • FDI numbering • mm & degree measurements",
                fontSize = 12.sp,
                color = BrandGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
