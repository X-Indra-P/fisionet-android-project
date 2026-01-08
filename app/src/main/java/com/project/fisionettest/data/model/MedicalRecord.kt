package com.project.fisionettest.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MedicalRecord(
    val id: Int? = null,
    val created_at: String? = null,
    val patient_id: Int,
    val date: String,
    @SerialName("diagnosa")
    val diagnosis: String,
    val vital_sign: String,
    val patient_problem: String,
    val inspection: String,
    val planning: String
)
