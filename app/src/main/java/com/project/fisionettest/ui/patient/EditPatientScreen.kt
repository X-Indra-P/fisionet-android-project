package com.project.fisionettest.ui.patient

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Patient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPatientScreen(navController: NavController, patient: Patient) {
    var name by remember { mutableStateOf(patient.name) }
    var diagnosis by remember { mutableStateOf(patient.diagnosis) }
    var phone by remember { mutableStateOf(patient.phone ?: "") }
    var address by remember { mutableStateOf(patient.address ?: "") }
    var dateOfBirth by remember { mutableStateOf(patient.date_of_birth ?: "") }
    var gender by remember { mutableStateOf(patient.gender ?: "L") }
    var emergencyContact by remember { mutableStateOf(patient.emergency_contact ?: "") }
    var emergencyPhone by remember { mutableStateOf(patient.emergency_phone ?: "") }
    var notes by remember { mutableStateOf(patient.notes ?: "") }
    var status by remember { mutableStateOf(patient.status) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Edit Pasien") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Basic Info Section
            Text("Informasi Dasar", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Pasien *") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text("Diagnosis *") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Nomor Telepon") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = it },
                label = { Text("Tanggal Lahir (YYYY-MM-DD)") },
                placeholder = { Text("2000-01-31") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Gender Selection
            Text("Jenis Kelamin", style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = gender == "L",
                    onClick = { gender = "L" },
                    label = { Text("Laki-laki") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = gender == "P",
                    onClick = { gender = "P" },
                    label = { Text("Perempuan") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Alamat") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Status Selection
            Text("Status Pasien", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = status == "Aktif",
                    onClick = { status = "Aktif" },
                    label = { Text("Aktif") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = status == "Selesai",
                    onClick = { status = "Selesai" },
                    label = { Text("Selesai") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = status == "Tidak Aktif",
                    onClick = { status = "Tidak Aktif" },
                    label = { Text("Tidak Aktif") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Emergency Contact Section
            Text("Kontak Darurat", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = emergencyContact,
                onValueChange = { emergencyContact = it },
                label = { Text("Nama Kontak Darurat") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = emergencyPhone,
                onValueChange = { emergencyPhone = it },
                label = { Text("Nomor Kontak Darurat") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Tambahan") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            isLoading = true
                            val updatedPatient = Patient(
                                id = patient.id,
                                created_at = patient.created_at,
                                name = name,
                                diagnosis = diagnosis,
                                therapist_id = patient.therapist_id,
                                phone = phone.ifBlank { null },
                                address = address.ifBlank { null },
                                date_of_birth = dateOfBirth.ifBlank { null },
                                gender = gender,
                                emergency_contact = emergencyContact.ifBlank { null },
                                emergency_phone = emergencyPhone.ifBlank { null },
                                notes = notes.ifBlank { null },
                                status = status
                            )
                            
                            SupabaseClient.client.from("patients").update(updatedPatient) {
                                filter { eq("id", patient.id!!) }
                            }
                            
                            Toast.makeText(context, "Pasien Berhasil Diupdate", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && name.isNotBlank() && diagnosis.isNotBlank()
            ) {
                Text("Simpan Perubahan")
            }
        }
    }
}
