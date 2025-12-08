package com.project.fisionettest.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Dashboard : BottomNavItem("dashboard", Icons.Default.Home, "Dashboard")
    object Patients : BottomNavItem("home", Icons.Default.List, "Pasien")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profil")
    
    companion object {
        fun items() = listOf(Dashboard, Patients, Profile)
    }
}
