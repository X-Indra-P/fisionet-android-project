package com.project.fisionettest.ui.patient

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.MedicalRecord
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.navigation.Screen
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(navController: NavController, patientId: Int) {
    var patient by remember { mutableStateOf<Patient?>(null) }
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(patientId) {
        try {
            patient = SupabaseClient.client.from("patients").select {
                filter { eq("id", patientId) }
            }.decodeSingle()
            
            records = SupabaseClient.client.from("medical_records").select {
                filter { eq("patient_id", patientId) }
            }.decodeList()
        } catch (e: Exception) {
             Toast.makeText(context, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    if (patient == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
             CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(patient!!.name) },
                    actions = {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Patient Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Informasi Pasien", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            InfoRow("Nama", patient!!.name)
                            InfoRow("Diagnosis", patient!!.diagnosis)
                            patient!!.phone?.let { InfoRow("Telepon", it) }
                            patient!!.date_of_birth?.let { InfoRow("Tanggal Lahir", it) }
                            patient!!.gender?.let { 
                                InfoRow("Jenis Kelamin", if (it == "L") "Laki-laki" else "Perempuan") 
                            }
                            patient!!.address?.let { InfoRow("Alamat", it) }
                            InfoRow("Status", patient!!.status)
                        }
                    }
                }
                
                // Emergency Contact Card
                if (patient!!.emergency_contact != null || patient!!.emergency_phone != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Kontak Darurat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                patient!!.emergency_contact?.let { InfoRow("Nama", it) }
                                patient!!.emergency_phone?.let { InfoRow("Telepon", it) }
                            }
                        }
                    }
                }
                
                // Notes Card
                patient!!.notes?.let { notes ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Catatan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                
                // Add Medical Record Button
                item {
                    Button(
                        onClick = { navController.navigate(Screen.AddMedicalRecord.createRoute(patientId)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tambah Rekam Medis")
                    }
                }
                
                // Medical Records Section
                item {
                    Text("Riwayat Rekam Medis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                items(records) { record ->
                    var showDeleteRecordDialog by remember { mutableStateOf(false) }
                    
                     Card(
                        modifier = Modifier.fillMaxWidth(),
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Tanggal: ${record.date}", fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = { showDeleteRecordDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Catatan: ${record.notes}")
                            Text(text = "Penanganan: ${record.treatment}")
                        }
                    }
                    
                    if (showDeleteRecordDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteRecordDialog = false },
                            title = { Text("Hapus Rekam Medis") },
                            text = { Text("Apakah Anda yakin ingin menghapus rekam medis ini?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                SupabaseClient.client.from("medical_records").delete {
                                                    filter { eq("id", record.id!!) }
                                                }
                                                // Refresh records
                                                records = SupabaseClient.client.from("medical_records").select {
                                                    filter { eq("patient_id", patientId) }
                                                }.decodeList()
                                                Toast.makeText(context, "Rekam medis dihapus", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        showDeleteRecordDialog = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Hapus")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteRecordDialog = false }) {
                                    Text("Batal")
                                }
                            }
                        )
                    }
                }
                
                if (records.isEmpty()) {
                    item {
                        Text(
                            "Belum ada rekam medis",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Hapus Pasien") },
                text = { Text("Apakah Anda yakin ingin menghapus pasien ${patient!!.name}? Semua rekam medis akan ikut terhapus.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    // Delete medical records first
                                    SupabaseClient.client.from("medical_records").delete {
                                        filter { eq("patient_id", patientId) }
                                    }
                                    // Then delete patient
                                    SupabaseClient.client.from("patients").delete {
                                        filter { eq("id", patientId) }
                                    }
                                    Toast.makeText(context, "Pasien berhasil dihapus", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
        
        // Edit Modal Sheet
        if (showEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false }
            ) {
                EditPatientScreen(navController, patient!!)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
