package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: Int? = null,
    val date: String = "", // Format: YYYY-MM-DD
    val patient_id: Int? = null,
    val diagnosis_id: Int? = null,
    val cabang_package_id: Int? = null,
    val total_amount: Double = 0.0,
    val payment_status: String? = "pending",
    val xendit_id: String? = null,
    val profile_id: String? = null,
    val id_cabang: Int? = null,
    val created_at: String? = null,

    // Relations for fetching (will be null during insert)
    val patients: Patient? = null,
    val diagnosis: Diagnosis? = null,
    val cabang_package: CabangPackage? = null,
    val profiles: Profile? = null
)
