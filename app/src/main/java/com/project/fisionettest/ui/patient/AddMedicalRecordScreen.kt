package com.project.fisionettest.ui.patient

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.MedicalRecord
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddMedicalRecordScreen(navController: NavController, patientId: Int) {
    var notes by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    // Ideally use a DatePicker, but simple text or auto-date for MVP
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Tambah Rekam Medis", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Catatan / Keluhan") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = treatment,
            onValueChange = { treatment = it },
            label = { Text("Penanganan / Terapi") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        isLoading = true
                        val record = MedicalRecord(
                            patient_id = patientId,
                            date = currentDate,
                            notes = notes,
                            treatment = treatment
                        )
                        SupabaseClient.client.from("medical_records").insert(record)
                        Toast.makeText(context, "Rekam Medis Disimpan", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Simpan (Tanggal: $currentDate)")
        }
    }
}
