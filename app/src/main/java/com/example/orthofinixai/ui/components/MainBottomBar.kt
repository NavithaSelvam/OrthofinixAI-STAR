package com.example.orthofinixai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.orthofinixai.ui.navigation.Screen
import com.example.orthofinixai.ui.theme.ClinicalDeepNavy
import com.example.orthofinixai.ui.theme.ClinicalSkyBlue

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val mainNavItems = listOf(
    BottomNavItem(Screen.Dashboard.route, "Home", Icons.Default.Home),
    BottomNavItem(Screen.CaseList.route, "Cases", Icons.Default.Folder),
    BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings),
    BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
)

@Composable
fun MainBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        mainNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ClinicalSkyBlue,
                    selectedTextColor = ClinicalDeepNavy,
                    indicatorColor = ClinicalSkyBlue.copy(alpha = 0.12f)
                )
            )
        }
    }
}
