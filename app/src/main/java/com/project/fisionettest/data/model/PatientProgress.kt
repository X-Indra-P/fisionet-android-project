package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PatientProgress(
    val id: Int? = null,
    val patient_id: Int,
    val diagnosis_id: Int? = null,
    val date: String,
    val progress_note: String,
    val cabang_package_id: Int? = null,
    val status: String? = null,
    val created_at: String? = null,
    val profile_id: String? = null,
    val id_cabang: Int? = null,
    val profiles: Profile? = null,
    val cabang_package: CabangPackage? = null
)
