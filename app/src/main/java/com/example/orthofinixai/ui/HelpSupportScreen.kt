package com.example.orthofinixai.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orthofinixai.ui.theme.PrimaryGreen
import com.example.orthofinixai.ui.theme.TextGray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
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
                .verticalScroll(rememberScrollState())
        ) {
            Text("How can we help you?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            SupportActionCard(
                title = "Chat with Support", 
                description = "Real-time chat assistance.", 
                icon = Icons.Default.Chat,
                onClick = {
                    Toast.makeText(context, "Opening real-time chat support console...", Toast.LENGTH_SHORT).show()
                }
            )
            
            SupportActionCard(
                title = "Email Us", 
                description = "Send us a message at support@orthofinix.ai", 
                icon = Icons.Default.Email,
                onClick = {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@orthofinix.ai")
                        putExtra(Intent.EXTRA_SUBJECT, "Orthofinix.AI Support Request")
                    }
                    try {
                        context.startActivity(emailIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email client app found on your device.", Toast.LENGTH_LONG).show()
                    }
                }
            )
            
            SupportActionCard(
                title = "FAQs", 
                description = "Find answers to commonly asked questions below.", 
                icon = Icons.Default.QuestionAnswer,
                onClick = {
                    Toast.makeText(context, "Scroll down to browse standard clinical FAQs.", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("Frequently Asked Questions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            FAQItem("How accurate is the AI assessment?", "Our AI is trained on thousands of board-certified cases and achieves over 95% consistency with expert human graders.")
            FAQItem("Can I use this for final diagnosis?", "Orthofinix.ai is a decision support tool. Final clinical decisions should always be made by a qualified orthodontist.")
            FAQItem("What image formats are supported?", "We support high-resolution JPG, PNG, and DICOM formats for radiographs.")
        }
    }
}

@Composable
fun SupportActionCard(
    title: String, 
    description: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(description, fontSize = 14.sp, color = TextGray)
            }
        }
    }
}

@Composable
fun FAQItem(question: String, answer: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(question, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(answer, fontSize = 14.sp, color = TextGray)
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFFF3F4F6))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val page = onboardingPages[pageIndex]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Image placeholder
                Box(
                    modifier = Modifier.size(280.dp).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Illustration for ${page.title}",
                        textAlign = TextAlign.Companion.Center,
                        color = PrimaryGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = page.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Companion.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = page.description,
                    fontSize = 16.sp,
                    color = TextGray,
                    textAlign = TextAlign.Companion.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(onboardingPages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) PrimaryGreen else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (pagerState.currentPage == iteration) 12.dp else 8.dp)
                        .background(color, RoundedCornerShape(4.dp))
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage < onboardingPages.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text(
                if (pagerState.currentPage == onboardingPages.size - 1) "Get Started" else "Next",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(onClick = onFinish) {
            Text("Skip", color = TextGray)
        }
    }
}

data class OnboardingPage(val title: String, val description: String)

val onboardingPages = listOf(
    OnboardingPage("AI-Driven Finishing", "Leverage advanced AI to assess orthodontic finishing with precision and speed."),
    OnboardingPage("Standardized Metrics", "Automated ABO OGS, Andrews' Six Keys, and Raleigh-Williams assessment in seconds."),
    OnboardingPage("Actionable Insights", "Get evidence-based recommendations and visual overlays to perfect every case.")
)