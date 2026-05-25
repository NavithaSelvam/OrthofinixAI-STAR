package com.example.orthofinixai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orthofinixai.data.model.SavedCase
import com.example.orthofinixai.ui.components.BrandedTopBar
import com.example.orthofinixai.ui.components.MainBottomBar
import com.example.orthofinixai.ui.navigation.Screen
import com.example.orthofinixai.ui.theme.*
import com.example.orthofinixai.ui.viewmodel.CaseListState
import com.example.orthofinixai.ui.viewmodel.CaseViewModel
import com.example.orthofinixai.ui.viewmodel.PatientViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseListScreen(
    onBack: (() -> Unit)? = null,
    onCaseClick: (String) -> Unit,
    onBottomNav: (String) -> Unit = {},
    viewModel: PatientViewModel = viewModel(),
    caseViewModel: CaseViewModel = viewModel()
) {
    val caseState by caseViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var caseToDelete by remember { mutableStateOf<SavedCase?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchPatients()
        caseViewModel.loadCases()
    }

    LaunchedEffect(searchQuery) {
        caseViewModel.search(searchQuery)
    }

    if (caseToDelete != null) {
        AlertDialog(
            onDismissRequest = { caseToDelete = null },
            title = { Text("Delete Case?") },
            text = { Text("This will permanently delete ${caseToDelete!!.patientName}'s analysis and images.") },
            confirmButton = {
                TextButton(onClick = {
                    caseViewModel.deleteCase(caseToDelete!!.id)
                    caseToDelete = null
                }) { Text("Delete", color = StatusError) }
            },
            dismissButton = {
                TextButton(onClick = { caseToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = { BrandedTopBar("Saved Cases", onBack = onBack) },
        bottomBar = {
            MainBottomBar(currentRoute = Screen.CaseList.route, onNavigate = onBottomNav)
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search by patient or case ID") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            when (val state = caseState) {
                is CaseListState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandGreen)
                    }
                }
                is CaseListState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = StatusError)
                    }
                }
                is CaseListState.Success -> {
                    if (state.cases.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No saved cases yet", fontWeight = FontWeight.Bold, color = BrandNavy)
                                Text("Start a new analysis to save cases here.", color = ClinicalSlate, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.cases, key = { it.id }) { case ->
                                SavedCaseCard(
                                    case = case,
                                    onOpen = { onCaseClick(case.id) },
                                    onDelete = { caseToDelete = case }
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
private fun SavedCaseCard(case: SavedCase, onOpen: () -> Unit, onDelete: () -> Unit) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(case.createdAt))
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(case.patientName, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = BrandNavy)
                Text("Case #${case.id.takeLast(8)}", fontSize = 12.sp, color = ClinicalSlate)
                Spacer(modifier = Modifier.height(4.dp))
                Text(dateStr, fontSize = 11.sp, color = BrandGray)
                Text(
                    "Confidence ${(case.confidenceScore * 100).toInt()}% • Andrews ${case.andrewsScore.toInt()}",
                    fontSize = 11.sp,
                    color = BrandGreen
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError)
            }
        }
    }
}
