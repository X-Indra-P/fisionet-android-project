package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Patient(
    val id: Int? = null,
    val created_at: String? = null,
    val name: String,
    val umur: Int? = null,
    val pekerjaan: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val gender: String? = null, // "L" or "P"
    val notes: String? = null,
    val profile_id: String? = null,
    val profiles: Profile? = null
)
