package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Clinic(
    val id: Int? = null,
    val nama_cabang: String,
    val alamat_cabang: String? = null
)
