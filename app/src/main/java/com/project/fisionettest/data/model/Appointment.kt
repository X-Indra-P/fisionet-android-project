package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: Int? = null,
    val created_at: String? = null,
    val patient_id: Int? = null,
    val date: String,
    val time: String,
    val status: String = "Terjadwal", // "Terjadwal", "Selesai", "Dibatalkan"
    val notes: String? = null,
    val id_cabang: Int? = null,
    val profile_id: String? = null,
    val patients: Patient? = null,
    val profiles: Profile? = null
)
