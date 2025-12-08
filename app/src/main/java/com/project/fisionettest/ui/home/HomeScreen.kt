package com.project.fisionettest.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Semua") }
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
    
    // Filter and search logic
    val filteredPatients = patients
        .filter { patient ->
            // Filter by search query
            val matchesSearch = searchQuery.isBlank() || 
                patient.name.contains(searchQuery, ignoreCase = true) ||
                patient.diagnosis.contains(searchQuery, ignoreCase = true)
            
            // Filter by status
            val matchesStatus = selectedStatus == "Semua" || patient.status == selectedStatus
            
            matchesSearch && matchesStatus
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Cari nama atau diagnosis...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            // Status Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("Semua", "Aktif", "Selesai", "Tidak Aktif")) { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Patient List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(filteredPatients) { patient ->
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = patient.name, 
                                    style = MaterialTheme.typography.titleMedium
                                )
                                // Status Badge
                                Surface(
                                    color = when (patient.status) {
                                        "Aktif" -> MaterialTheme.colorScheme.primaryContainer
                                        "Selesai" -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.errorContainer
                                    },
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = patient.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Diagnosis: ${patient.diagnosis}", 
                                style = MaterialTheme.typography.bodyMedium
                            )
                            patient.phone?.let {
                                Text(
                                    text = "Telepon: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                if (filteredPatients.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                "Tidak ada pasien ditemukan",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
