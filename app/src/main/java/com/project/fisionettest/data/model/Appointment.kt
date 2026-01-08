package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: Int? = null,
    val created_at: String? = null,
    val patient_id: Int? = null, // Nullable for standalone appointments
    val patient_name: String? = null, // Required for standalone (nullable for backward compatibility)
    val therapist_id: String? = null,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:MM
    val status: String = "Terjadwal", // "Terjadwal", "Selesai", "Dibatalkan"
    val notes: String? = null
)
