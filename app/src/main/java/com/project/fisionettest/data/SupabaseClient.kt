package com.project.fisionettest.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClient {
    private const val SUPABASE_URL = "https://siyojsvbaqmjognnpwkm.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNpeW9qc3ZiYXFtam9nbm5wd2ttIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUxNzU0MDgsImV4cCI6MjA4MDc1MTQwOH0.SWyy2N0qPbFxttUu1dWJFG0LIHMKrDDlkuU2tuEW4HE"

    lateinit var client: io.github.jan.supabase.SupabaseClient

    fun initialize(context: android.content.Context) {
        client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
            })

            install(Auth) {
                // Use a custom settings implementation acting as a bridge to SharedPreferences
                val prefs = context.getSharedPreferences("supabase_auth", android.content.Context.MODE_PRIVATE)
                sessionManager = object : io.github.jan.supabase.gotrue.SessionManager {
                    override suspend fun saveSession(session: io.github.jan.supabase.gotrue.user.UserSession) {
                        prefs.edit().putString("session", Json.encodeToString(io.github.jan.supabase.gotrue.user.UserSession.serializer(), session)).apply()
                    }

                    override suspend fun loadSession(): io.github.jan.supabase.gotrue.user.UserSession? {
                        val sessionStr = prefs.getString("session", null)
                        return if (sessionStr != null) {
                            try {
                                Json.decodeFromString(io.github.jan.supabase.gotrue.user.UserSession.serializer(), sessionStr)
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    }

                    override suspend fun deleteSession() {
                        prefs.edit().remove("session").apply()
                    }
                }
            }
            install(Postgrest)
        }
    }
}
