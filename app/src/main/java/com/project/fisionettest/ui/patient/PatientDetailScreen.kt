package com.project.fisionettest.ui.patient

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                    title = { Text(patient!!.name) }
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
                     Card(
                        modifier = Modifier.fillMaxWidth(),
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Tanggal: ${record.date}", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Catatan: ${record.notes}")
                            Text(text = "Penanganan: ${record.treatment}")
                        }
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
