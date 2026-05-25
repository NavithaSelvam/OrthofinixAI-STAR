package com.example.orthofinixai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orthofinixai.ui.theme.PrimaryGreen
import com.example.orthofinixai.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadGuideScreen(onNext: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Upload Guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                "Step 2 of 4",
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                "Standardize your clinical photos for best AI results",
                color = TextGray,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(guideItems) { item ->
                    GuideItemRow(item)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Start Uploading", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GuideItemRow(item: GuideItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(item.description, fontSize = 14.sp, color = TextGray)
        }
    }
}

data class GuideItem(val title: String, val description: String)

val guideItems = listOf(
    GuideItem("Controlled Lighting", "Use ring flash or bright diffused light to avoid shadows."),
    GuideItem("Standard Orientation", "Keep the occlusal plane horizontal in all views."),
    GuideItem("Full Visibility", "Ensure all teeth and gingival margins are clearly visible."),
    GuideItem("Dry Field", "Use air to dry teeth to avoid reflections on enamel."),
    GuideItem("Retraction", "Use cheek retractors for clear buccal and occlusal views.")
)
