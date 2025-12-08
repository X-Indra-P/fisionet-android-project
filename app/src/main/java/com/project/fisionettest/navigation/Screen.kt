package com.project.fisionettest.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object AddPatient : Screen("add_patient")
    object PatientDetail : Screen("patient_detail/{patientId}") {
        fun createRoute(patientId: Int) = "patient_detail/$patientId"
    }
    object AddMedicalRecord : Screen("add_record/{patientId}") {
        fun createRoute(patientId: Int) = "add_record/$patientId"
    }
}
