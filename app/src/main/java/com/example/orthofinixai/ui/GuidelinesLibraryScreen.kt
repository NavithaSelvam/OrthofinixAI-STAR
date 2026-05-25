package com.example.orthofinixai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
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
fun GuidelinesLibraryScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guidelines Library") },
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
                .padding(16.dp)
        ) {
            Text(
                "Clinical References",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Access standard orthodontic indices and principles",
                color = TextGray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(guidelines) { guideline ->
                    GuidelineCard(guideline)
                }
            }
        }
    }
}

@Composable
fun GuidelineCard(guideline: GuidelineInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to detail */ },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PrimaryGreen.copy(alpha = 0.1f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Book, contentDescription = null, tint = PrimaryGreen)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(guideline.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(guideline.description, fontSize = 12.sp, color = TextGray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray)
        }
    }
}

data class GuidelineInfo(val name: String, val description: String)

val guidelines = listOf(
    GuidelineInfo("ABO Objective Grading System", "The gold standard for board-level finishing assessment."),
    GuidelineInfo("Andrews' Six Keys", "Fundamental requirements for normal occlusion."),
    GuidelineInfo("Dr. Rebecca Roling's Concepts", "Functional stability and arch form harmony principles."),
    GuidelineInfo("Raleigh-Williams Keys", "Six keys to orthodontic treatment excellence."),
    GuidelineInfo("Ricketts/Merrifield Analysis", "Cephalometric and skeletal finishing criteria."),
    GuidelineInfo("Roth/Williams Philosophy", "Functional occlusion and gnathological finishing.")
)
