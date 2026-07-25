package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@SerialName("diagnosis")
data class Diagnosis(
    val id: Int? = null,
    val created_at: String? = null,
    val patient_id: Int,
    val diagnosa: String,
    val vital_sign: String,
    val patient_problem: String,
    val inspection: String,
    val date: String,
    val profile_id: String? = null,
    val id_cabang: Int? = null,
    val cabang: Clinic? = null,
    val cabang_package_id: Int? = null,
    val cabang_package: CabangPackage? = null,
    val profiles: Profile? = null,
    val status: String? = "Proses"
)
