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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Nama: ${patient!!.name}", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Diagnosis: ${patient!!.diagnosis}", style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { navController.navigate(Screen.AddMedicalRecord.createRoute(patientId)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tambah Rekam Medis")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Riwayat Rekam Medis:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            LazyColumn {
                items(records) { record ->
                     Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Tanggal: ${record.date}", fontWeight = FontWeight.Bold)
                            Text(text = "Catatan: ${record.notes}")
                            Text(text = "Penanganan: ${record.treatment}")
                        }
                    }
                }
            }
        }
    }
}
