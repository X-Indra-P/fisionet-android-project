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
import com.project.fisionettest.data.model.Patient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@Composable
fun AddPatientScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Tambah Pasien Baru", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama Pasien") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = diagnosis,
            onValueChange = { diagnosis = it },
            label = { Text("Diagnosis") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    val user = SupabaseClient.client.auth.currentSessionOrNull()?.user
                    if (user == null) {
                        Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    try {
                        isLoading = true
                        val newPatient = Patient(
                            name = name,
                            diagnosis = diagnosis,
                            therapist_id = user.id
                        )
                        SupabaseClient.client.from("patients").insert(newPatient)
                        Toast.makeText(context, "Pasien Ditambahkan", Toast.LENGTH_SHORT).show()
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
            Text("Simpan")
        }
    }
}
