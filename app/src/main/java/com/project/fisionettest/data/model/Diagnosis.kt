package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@SerialName("diagnosis") // Ensures it maps to 'diagnosis' table if using library features looking for name, though usually explicit
data class Diagnosis(
    val id: Long? = null,
    val created_at: String? = null,
    val patient_id: Int,
    val diagnosa: String,
    val vital_sign: String,
    val patient_problem: String,
    val inspection: String,
    val planning: String,
    val date: String
)
