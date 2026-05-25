package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: Int? = null,
    val date: String, // Format: YYYY-MM-DD
    val patient_id: Int?,
    val diagnosis_id: Int? = null,
    val package_id: Int? = null, // New field
    val total_amount: Double,
    val payment_status: String? = "pending", // Payment status (pending, success, failed, etc)
    val xendit_id: String? = null, // Xendit Invoice ID for status checking
    val user_id: String? = null,
    val user_name: String? = null, // Restored for display
    val cabang: String? = null, // New field for branch
    val created_at: String? = null,

    // Relations for fetching (will be null during insert)
    val patients: Patient? = null,
    val diagnosis: Diagnosis? = null,
    val packages: Package? = null // New relation
)
