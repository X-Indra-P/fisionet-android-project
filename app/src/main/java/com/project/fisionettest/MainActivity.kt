package com.project.fisionettest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.project.fisionettest.navigation.Screen
import com.project.fisionettest.ui.auth.LoginScreen
import com.project.fisionettest.ui.auth.RegisterScreen
import com.project.fisionettest.ui.home.HomeScreen
import com.project.fisionettest.ui.patient.AddMedicalRecordScreen
import com.project.fisionettest.ui.patient.AddPatientScreen
import com.project.fisionettest.ui.patient.PatientDetailScreen
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
                    
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Login.route
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(navController)
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(navController)
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(navController)
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