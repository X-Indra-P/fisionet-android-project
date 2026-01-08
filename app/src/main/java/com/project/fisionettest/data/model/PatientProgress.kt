package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PatientProgress(
    val id: Int? = null,
    val patient_id: Int,
    val date: String,
    val progress_note: String,
    val created_at: String? = null
)
