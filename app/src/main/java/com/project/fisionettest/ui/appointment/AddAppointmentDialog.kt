package com.project.fisionettest.ui.appointment

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Appointment
import com.project.fisionettest.data.model.Patient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Fetch patients
    LaunchedEffect(Unit) {
        try {
            patients = SupabaseClient.client.from("patients")
                .select()
                .decodeList<Patient>()
                .filter { it.status == "Aktif" }
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading patients", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Get current date and time as default
    LaunchedEffect(Unit) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()
        selectedDate = dateFormat.format(now)
        selectedTime = timeFormat.format(now)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Jadwal Terapi") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Patient Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedPatient?.name ?: "Pilih Pasien",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pasien *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        patients.forEach { patient ->
                            DropdownMenuItem(
                                text = { Text(patient.name) },
                                onClick = {
                                    selectedPatient = patient
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Date
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    label = { Text("Tanggal (YYYY-MM-DD) *") },
                    placeholder = { Text("2024-12-25") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Time
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    label = { Text("Waktu (HH:MM) *") },
                    placeholder = { Text("14:30") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedPatient == null) {
                        Toast.makeText(context, "Pilih pasien", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    
                    scope.launch {
                        try {
                            val user = SupabaseClient.client.auth.currentSessionOrNull()?.user
                            if (user == null) {
                                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            
                            val appointment = Appointment(
                                patient_id = selectedPatient!!.id!!,
                                therapist_id = user.id,
                                date = selectedDate,
                                time = selectedTime,
                                notes = notes.ifBlank { null }
                            )
                            
                            SupabaseClient.client.from("appointments").insert(appointment)
                            Toast.makeText(context, "Jadwal ditambahkan", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = selectedPatient != null && selectedDate.isNotBlank() && selectedTime.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
