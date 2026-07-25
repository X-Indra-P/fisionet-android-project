package com.project.fisionettest.utils

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("fisionet_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_CLINIC     = "clinic"
        const val KEY_CLINIC_ID  = "clinic_id"
        const val KEY_USER_ID    = "user_id"
        const val KEY_USER_NAME  = "user_name"
        const val KEY_USER_ROLE  = "user_role"   // 1 = Admin, 2 = Therapist
        const val KEY_ACTIVE_APPOINTMENT_ID = "active_appointment_id"
    }

    // ── Generic helpers ──────────────────────────────────────────────────────
    fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getString(key: String): String? = sharedPreferences.getString(key, null)

    fun saveInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, default: Int = 0): Int = sharedPreferences.getInt(key, default)

    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    // ── Typed shorthand helpers ───────────────────────────────────────────────

    /** Cabang aktif terapis yang sedang login, e.g. "Cabang 1" / "Cabang 2" */
    var clinic: String?
        get() = getString(KEY_CLINIC)
        set(value) { if (value != null) saveString(KEY_CLINIC, value) else remove(KEY_CLINIC) }

    var clinicId: Int
        get() = getInt(KEY_CLINIC_ID, 0)
        set(value) { saveInt(KEY_CLINIC_ID, value) }

    var userId: String?
        get() = getString(KEY_USER_ID)
        set(value) { if (value != null) saveString(KEY_USER_ID, value) else remove(KEY_USER_ID) }

    var userName: String?
        get() = getString(KEY_USER_NAME)
        set(value) { if (value != null) saveString(KEY_USER_NAME, value) else remove(KEY_USER_NAME) }

    /** Role: 1 = Admin, 2 = Therapist */
    var userRole: Int
        get() = getInt(KEY_USER_ROLE, 2)
        set(value) { saveInt(KEY_USER_ROLE, value) }

    var activeAppointmentId: Int
        get() = getInt(KEY_ACTIVE_APPOINTMENT_ID, -1)
        set(value) { saveInt(KEY_ACTIVE_APPOINTMENT_ID, value) }

    /** Hapus semua data sesi (dipanggil saat logout) */
    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_CLINIC)
            .remove(KEY_CLINIC_ID)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_ROLE)
            .remove(KEY_ACTIVE_APPOINTMENT_ID)
            .apply()
    }
}
