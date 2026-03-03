package com.project.fisionettest.data.repository

import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Profile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminRepository {

    suspend fun getPendingTherapists(): List<Profile> {
        return withContext(Dispatchers.IO) {
            SupabaseClient.client.from("profiles")
                .select(columns = Columns.list("id", "display_name", "role", "status", "clinic", "created_at")) {
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
                .select(columns = Columns.list("id", "display_name", "role", "status", "clinic", "created_at")) {
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
                    set("clinic", clinic)
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
}
