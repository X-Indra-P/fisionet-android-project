package com.project.fisionettest.data.repository

import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Profile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.project.fisionettest.utils.ClinicMapper

class AdminRepository {

    suspend fun getPendingTherapists(): List<Profile> {
        return withContext(Dispatchers.IO) {
            SupabaseClient.client.from("profiles")
                .select(columns = Columns.list("id", "display_name", "role", "status", "id_cabang", "created_at")) {
                    filter {
                        eq("role", 2) // Therapist
                        eq("status", "pending")
                    }
                }.decodeList<Profile>()
        }
    }

    suspend fun getApprovedTherapists(): List<Profile> {
        return withContext(Dispatchers.IO) {
            SupabaseClient.client.from("profiles")
                .select(columns = Columns.list("id", "display_name", "role", "status", "id_cabang", "created_at")) {
                    filter {
                        eq("role", 2) // Therapist
                        eq("status", "verified")
                    }
                }.decodeList<Profile>()
        }
    }

    suspend fun verifyTherapist(userId: String, clinic: String) {
        withContext(Dispatchers.IO) {
            SupabaseClient.client.from("profiles").update(
                {
                    set("status", "verified")
                    set("id_cabang", ClinicMapper.toId(clinic))
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
        }
    }

    suspend fun rejectTherapist(userId: String) {
        withContext(Dispatchers.IO) {
            SupabaseClient.client.from("profiles").update(
                {
                    set("status", "rejected")
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
        }
    }

    suspend fun getUserProfile(userId: String): Profile? {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }.decodeSingleOrNull<Profile>()
            } catch (e: Exception) {
                null
            }
        }
    }
    suspend fun transferBranch(userId: String, newClinic: String) {
        withContext(Dispatchers.IO) {
            SupabaseClient.client.from("profiles").update(
                {
                    set("id_cabang", ClinicMapper.toId(newClinic))
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
        }
    }

    /** Nonaktifkan akun terapis (status: inactive) */
    suspend fun deactivateTherapist(userId: String) {
        withContext(Dispatchers.IO) {
            SupabaseClient.client.from("profiles").update(
                { set("status", "inactive") }
            ) {
                filter { eq("id", userId) }
            }
        }
    }

    /** Aktifkan kembali akun terapis yang inactive (status: verified) */
    suspend fun reactivateTherapist(userId: String) {
        withContext(Dispatchers.IO) {
            SupabaseClient.client.from("profiles").update(
                { set("status", "verified") }
            ) {
                filter { eq("id", userId) }
            }
        }
    }

    /** Ambil semua terapis aktif (verified) dan non-aktif (inactive/suspended) */
    suspend fun getAllTherapists(): List<Profile> {
        return withContext(Dispatchers.IO) {
            SupabaseClient.client.from("profiles")
                .select(columns = Columns.list("id", "display_name", "role", "status", "id_cabang", "created_at")) {
                    filter {
                        eq("role", 2)
                        // Filter: verified, inactive, atau suspended
                        or {
                            eq("status", "verified")
                            eq("status", "inactive")
                            eq("status", "suspended")
                        }
                    }
                }.decodeList<Profile>()
        }
    }
}
