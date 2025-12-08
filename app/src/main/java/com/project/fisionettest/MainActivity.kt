package com.project.fisionettest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.project.fisionettest.navigation.BottomNavItem
import com.project.fisionettest.navigation.Screen
import com.project.fisionettest.ui.auth.LoginScreen
import com.project.fisionettest.ui.auth.RegisterScreen
import com.project.fisionettest.ui.appointment.AppointmentScreen
import com.project.fisionettest.ui.dashboard.DashboardScreen
import com.project.fisionettest.ui.home.HomeScreen
import com.project.fisionettest.ui.patient.AddMedicalRecordScreen
import com.project.fisionettest.ui.patient.AddPatientScreen
import com.project.fisionettest.ui.patient.PatientDetailScreen
import com.project.fisionettest.ui.profile.ProfileScreen
import com.project.fisionettest.ui.theme.FisioNetTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FisioNetTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    
                    // Routes that should show bottom navigation
                    val bottomNavRoutes = listOf(
                        BottomNavItem.Dashboard.route,
                        BottomNavItem.Patients.route,
                        BottomNavItem.Appointments.route,
                        BottomNavItem.Profile.route
                    )
                    
                    Scaffold(
                        bottomBar = {
                            if (currentRoute in bottomNavRoutes) {
                                NavigationBar {
                                    BottomNavItem.items().forEach { item ->
                                        NavigationBarItem(
                                            icon = { Icon(item.icon, contentDescription = item.label) },
                                            label = { Text(item.label) },
                                            selected = currentRoute == item.route,
                                            onClick = {
                                                if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Login.route,
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable(Screen.Login.route) {
                                LoginScreen(navController)
                            }
                            composable(Screen.Register.route) {
                                RegisterScreen(navController)
                            }
                            composable(BottomNavItem.Dashboard.route) {
                                DashboardScreen(navController)
                            }
                            composable(BottomNavItem.Patients.route) {
                                HomeScreen(navController)
                            }
                            composable(BottomNavItem.Appointments.route) {
                                AppointmentScreen(navController)
                            }
                            composable(BottomNavItem.Profile.route) {
                                ProfileScreen(navController)
                            }
                            composable(Screen.AddPatient.route) {
                                AddPatientScreen(navController)
                            }
                            composable(
                                route = Screen.PatientDetail.route,
                                arguments = listOf(navArgument("patientId") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val patientId = backStackEntry.arguments?.getInt("patientId") ?: 0
                                PatientDetailScreen(navController, patientId)
                            }
                            composable(
                                route = Screen.AddMedicalRecord.route,
                                arguments = listOf(navArgument("patientId") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val patientId = backStackEntry.arguments?.getInt("patientId") ?: 0
                                AddMedicalRecordScreen(navController, patientId)
                            }
                        }
                    }
                }
            }
        }
    }
}