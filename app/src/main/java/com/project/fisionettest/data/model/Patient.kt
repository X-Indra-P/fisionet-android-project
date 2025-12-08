package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Patient(
    val id: Int? = null,
    val created_at: String? = null,
    val name: String,
    val diagnosis: String,
    val therapist_id: String
)
