package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: Long? = null,
    val date: String, // Format: YYYY-MM-DD
    val patient_id: Int?,
    val patient_name: String,
    val package_id: Long?,
    val package_name: String,
    val amount: Double,
    val created_at: String? = null
)
