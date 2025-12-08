package com.project.fisionettest.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.navigation.Screen
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    fun fetchPatients() {
        scope.launch {
            try {
                 val result = SupabaseClient.client.from("patients").select().decodeList<Patient>()
                 patients = result
            } catch (e: Exception) {
                // Log or toast
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchPatients()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Pasien") },
                actions = {
                    IconButton(onClick = { fetchPatients() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddPatient.route) }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pasien")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(patients) { patient ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { 
                            patient.id?.let { id ->
                                navController.navigate(Screen.PatientDetail.createRoute(id)) 
                            }
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = patient.name, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Diagnosis: ${patient.diagnosis}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
