package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CabangPackage(
    val id: Int? = null,
    val id_cabang: Int,
    val id_package: Int,
    val id_tools: List<Int>? = null,
    val cabang: Clinic? = null,
    val packages: Package? = null
)
