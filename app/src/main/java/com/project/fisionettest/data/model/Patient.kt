package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Patient(
    val id: Int? = null,
    val created_at: String? = null,
    val name: String,
    val diagnosis: String,
    val therapist_id: String,
    val phone: String? = null,
    val address: String? = null,
    val date_of_birth: String? = null,
    val gender: String? = null, // "L" or "P"
    val emergency_contact: String? = null,
    val emergency_phone: String? = null,
    val notes: String? = null,
    val status: String = "Aktif" // "Aktif", "Selesai", "Tidak Aktif"
)
