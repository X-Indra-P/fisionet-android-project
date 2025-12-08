package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MedicalRecord(
    val id: Int? = null,
    val created_at: String? = null,
    val patient_id: Int,
    val date: String,
    val notes: String,
    val treatment: String
)
