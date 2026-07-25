package com.project.fisionettest.utils

import com.project.fisionettest.data.model.Clinic

object ClinicMapper {

    private val clinicMap = mutableMapOf<Int, String>()

    fun updateCache(clinics: List<Clinic>) {
        clinics.forEach { clinic ->
            val cid = clinic.id
            if (cid != null && cid > 0 && !clinic.nama_cabang.isNullOrBlank()) {
                clinicMap[cid] = clinic.nama_cabang
            }
        }
    }

    fun toId(clinicName: String?): Int? {
        if (clinicName.isNullOrBlank()) return null
        return clinicMap.entries.firstOrNull { it.value.equals(clinicName, ignoreCase = true) }?.key
            ?: when (clinicName) {
                "Cabang 1" -> 1
                "Cabang 2" -> 2
                else -> null
            }
    }

    fun toName(id: Int?, fallbackCabang: Clinic? = null): String {
        if (fallbackCabang?.nama_cabang?.isNotBlank() == true) {
            if (id != null) clinicMap[id] = fallbackCabang.nama_cabang
            return fallbackCabang.nama_cabang
        }
        if (id != null && clinicMap.containsKey(id)) {
            return clinicMap[id] ?: "Cabang 1"
        }
        return when (id) {
            1 -> "Cabang 1"
            2 -> "Cabang 2"
            else -> "Cabang 1"
        }
    }
}
