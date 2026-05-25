package com.example.orthofinixai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.orthofinixai.ui.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orthofinixai.ui.viewmodel.*
import androidx.compose.runtime.remember

private fun NavHostController.navigateMainTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Dashboard.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun OrthofinixNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route,
    onGoogleSignInClick: () -> Unit = {}
) {
    val sharedCaseViewModel: SharedCaseViewModel = viewModel()
    val patientViewModel: PatientViewModel = viewModel()
    val analysisViewModel: AnalysisViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController = navController,
        modifier = modifier,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                isLoggedIn = authViewModel.uiState.value is AuthState.Authenticated,
                onTimeout = {
                    val dest = if (authViewModel.uiState.value is AuthState.Authenticated)
                        Screen.Dashboard.route else Screen.Onboarding.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinish = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onSignInClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSignUpClick = { navController.navigate(Screen.SignUp.route) },
                onForgotPasswordClick = { navController.navigate(Screen.ForgotPassword.route) },
                onGoogleSignInClick = onGoogleSignInClick,
                viewModel = authViewModel
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpClick = { navController.navigate(Screen.Verification.route) },
                onSignInClick = { navController.navigate(Screen.Login.route) },
                viewModel = authViewModel
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Verification.route) {
            VerificationScreen(
                onBack = { navController.popBackStack() },
                onVerify = { 
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onAddCaseClick = { 
                    sharedCaseViewModel.reset()
                    navController.navigate(Screen.AddCasePatientInfo.route) 
                },
                onCaseClick = { caseId ->
                    analysisViewModel.loadReport(caseId)
                    navController.navigate(Screen.AssessmentSummary.route) 
                },
                onProfileClick = { navController.navigateMainTab(Screen.Profile.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onSeeAllClick = { navController.navigateMainTab(Screen.CaseList.route) },
                onBottomNav = { navController.navigateMainTab(it) },
                onDemoClick = {
                    analysisViewModel.loadDemoReport()
                    navController.navigate(Screen.AssessmentSummary.route)
                },
                viewModel = patientViewModel,
                authViewModel = authViewModel
            )
        }
        composable(Screen.CaseList.route) {
            CaseListScreen(
                onCaseClick = { caseId ->
                    analysisViewModel.loadReport(caseId)
                    navController.navigate(Screen.AssessmentSummary.route)
                },
                onBottomNav = { navController.navigateMainTab(it) },
                viewModel = patientViewModel,
                caseViewModel = viewModel()
            )
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
        
        // Add Case Flow
        composable(Screen.AddCasePatientInfo.route) {
            PatientInfoScreen(
                onNext = { name, dob, gender ->
                    sharedCaseViewModel.patientName = name
                    sharedCaseViewModel.dob = dob
                    sharedCaseViewModel.gender = gender
                    navController.navigate(Screen.AddCaseUploadGuide.route) 
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddCaseUploadGuide.route) {
            UploadGuideScreen(
                onNext = { navController.navigate(Screen.AddCasePhotoUpload.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddCasePhotoUpload.route) {
            PhotoUploadScreen(
                viewModel = sharedCaseViewModel,
                onNext = { navController.navigate(Screen.AddCaseOPGUpload.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddCaseOPGUpload.route) {
            OPGUploadScreen(
                onNext = { uri ->
                    sharedCaseViewModel.opgPhoto = uri
                    navController.navigate(Screen.AIProcessing.route) 
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AIProcessing.route) {
            AIProcessingScreen(
                sharedViewModel = sharedCaseViewModel,
                analysisViewModel = analysisViewModel,
                patientViewModel = patientViewModel,
                onProcessingComplete = {
                    navController.navigate(Screen.AssessmentSummary.route) {
                        popUpTo(Screen.AddCasePatientInfo.route) { inclusive = true }
                    }
                }
            )
        }

        // Assessment
        composable(Screen.AssessmentSummary.route) {
            AssessmentSummaryScreen(
                viewModel = analysisViewModel,
                patientViewModel = patientViewModel,
                onBack = { navController.popBackStack() },
                onVisualOverlay = { navController.navigate(Screen.VisualOverlay.route) },
                onDetails = { route -> 
                    val target = when(route) {
                        "abo" -> Screen.ABOScoring.route
                        "andrews" -> Screen.AndrewsKeys.route
                        "symmetry" -> Screen.ArchSymmetry.route
                        "roots" -> Screen.RootAngulation.route
                        "recommendations" -> Screen.Recommendations.route
                        else -> Screen.Dashboard.route
                    }
                    navController.navigate(target)
                }
            )
        }
        composable(Screen.ABOScoring.route) { 
            ABOScoringScreen(viewModel = analysisViewModel, onBack = { navController.popBackStack() }) 
        }
        composable(Screen.AndrewsKeys.route) { 
            AndrewsKeysScreen(viewModel = analysisViewModel, onBack = { navController.popBackStack() }) 
        }
        composable(Screen.RolingConcepts.route) { 
            RolingConceptsScreen(viewModel = analysisViewModel, onBack = { navController.popBackStack() }) 
        }
        composable(Screen.RaleighWilliams.route) { 
            RaleighWilliamsKeysScreen(viewModel = analysisViewModel, onBack = { navController.popBackStack() }) 
        }
        composable(Screen.Recommendations.route) { 
            RecommendationsScreen(viewModel = analysisViewModel, onBack = { navController.popBackStack() }) 
        }
        composable(Screen.VisualOverlay.route) { 
            VisualOverlayScreen(onBack = { navController.popBackStack() }) 
        }
        composable(Screen.RootAngulation.route) { 
            RootAngulationScreen(viewModel = analysisViewModel, onBack = { navController.popBackStack() }) 
        }
        composable(Screen.ArchSymmetry.route) { 
            ArchSymmetryScreen(viewModel = analysisViewModel, onBack = { navController.popBackStack() }) 
        }

        // Others
        composable(Screen.GuidelinesLibrary.route) { 
            GuidelinesLibraryScreen(onBack = { navController.popBackStack() }) 
        }
        composable(Screen.Profile.route) { 
            ProfileScreen(
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSubscriptionClick = { navController.navigate(Screen.Subscription.route) },
                onSettingsClick = { navController.navigateMainTab(Screen.Settings.route) },
                onHelpSupportClick = { navController.navigate(Screen.HelpSupport.route) },
                onAboutClick = { navController.navigate(Screen.About.route) },
                onBottomNav = { navController.navigateMainTab(it) },
                viewModel = authViewModel
            ) 
        }
        composable(Screen.Settings.route) { 
            SettingsScreen(onBottomNav = { navController.navigateMainTab(it) }) 
        }
        composable(Screen.Subscription.route) { 
            SubscriptionScreen(onBack = { navController.popBackStack() }) 
        }
        composable(Screen.ExportReport.route) { 
            ExportReportScreen(onBack = { navController.popBackStack() }) 
        }
        composable(Screen.HelpSupport.route) { 
            HelpSupportScreen(onBack = { navController.popBackStack() }) 
        }
        composable(Screen.About.route) { 
            AboutScreen(onBack = { navController.popBackStack() }) 
        }
    }
}
