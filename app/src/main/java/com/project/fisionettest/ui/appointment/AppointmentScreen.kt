package com.project.fisionettest.ui.appointment

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Appointment
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(navController: NavController) {
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    fun fetchAppointments() {
        scope.launch {
            try {
                val result = SupabaseClient.client.from("appointments")
                    .select()
                    .decodeList<Appointment>()
                appointments = result.sortedBy { it.date + it.time }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        fetchAppointments()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jadwal Terapi") },
                actions = {
                    IconButton(onClick = { fetchAppointments() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Jadwal")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Group by date
            val groupedAppointments = appointments.groupBy { it.date }
            
            groupedAppointments.forEach { (date, dayAppointments) ->
                item {
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(dayAppointments) { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        onDelete = {
                            scope.launch {
                                try {
                                    SupabaseClient.client.from("appointments").delete {
                                        filter { eq("id", appointment.id!!) }
                                    }
                                    fetchAppointments()
                                    Toast.makeText(context, "Jadwal dihapus", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onStatusChange = { newStatus ->
                            scope.launch {
                                try {
                                    SupabaseClient.client.from("appointments").update(
                                        mapOf("status" to newStatus)
                                    ) {
                                        filter { eq("id", appointment.id!!) }
                                    }
                                    fetchAppointments()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
            
            if (appointments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            "Belum ada jadwal",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddAppointmentDialog(
            onDismiss = { showAddDialog = false },
            onSuccess = {
                fetchAppointments()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (appointment.status) {
                "Selesai" -> MaterialTheme.colorScheme.tertiaryContainer
                "Dibatalkan" -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.time,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pasien: ${appointment.patient_name ?: "ID: ${appointment.patient_id}"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = appointment.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            appointment.notes?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Catatan: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (appointment.status == "Terjadwal") {
                    OutlinedButton(
                        onClick = { onStatusChange("Selesai") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Selesai")
                    }
                    OutlinedButton(
                        onClick = { onStatusChange("Dibatalkan") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                }
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Jadwal") },
            text = { Text("Apakah Anda yakin ingin menghapus jadwal ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
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
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        val date = inputFormat.parse(dateString)
        outputFormat.format(date!!)
    } catch (e: Exception) {
        dateString
    }
}
