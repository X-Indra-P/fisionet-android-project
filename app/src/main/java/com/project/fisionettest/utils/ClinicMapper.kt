package com.project.fisionettest.utils

object ClinicMapper {

    fun toId(clinicName: String?): Int? = when (clinicName) {
        "Cabang 1" -> 1
        "Cabang 2" -> 2
        else -> null
    }

    fun toName(id: Int?): String = when (id) {
        1 -> "Cabang 1"
        2 -> "Cabang 2"
        else -> "Cabang 1" // Default fallback
    }
}
