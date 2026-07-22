@file:OptIn(io.ktor.util.InternalAPI::class, io.github.jan.supabase.annotations.SupabaseInternal::class)
package com.project.fisionettest.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestRetry
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

object SupabaseClient {
    private const val SUPABASE_URL = "https://siyojsvbaqmjognnpwkm.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNpeW9qc3ZiYXFtam9nbm5wd2ttIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUxNzU0MDgsImV4cCI6MjA4MDc1MTQwOH0.SWyy2N0qPbFxttUu1dWJFG0LIHMKrDDlkuU2tuEW4HE"

    lateinit var client: io.github.jan.supabase.SupabaseClient

    fun initialize(context: android.content.Context) {
        client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            httpEngine = io.ktor.client.engine.okhttp.OkHttp.create {
                config {
                    connectTimeout(40, java.util.concurrent.TimeUnit.SECONDS)
                    readTimeout(40, java.util.concurrent.TimeUnit.SECONDS)
                    writeTimeout(40, java.util.concurrent.TimeUnit.SECONDS)
                }
            }

            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 40000
                    connectTimeoutMillis = 40000
                    socketTimeoutMillis = 40000
                }
                install(HttpRequestRetry) {
                    retryOnExceptionOrServerErrors(maxRetries = 3)
                    exponentialDelay()
                }
            }

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
            install(Storage)
        }
    }

    suspend fun checkAndUpdateActiveAppointment(context: android.content.Context) {
        val prefs = com.project.fisionettest.utils.AppPreferences(context)
        val activeApptId = prefs.activeAppointmentId
        if (activeApptId != -1) {
            try {
                client.from("appointments").update(
                    kotlinx.serialization.json.buildJsonObject {
                        put("status", "Selesai")
                    }
                ) {
                    filter { eq("id", activeApptId) }
                }
                prefs.activeAppointmentId = -1
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun autoMarkPastAppointmentsAsMissed() {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(java.util.Date())
        try {
            client.from("appointments").update(
                kotlinx.serialization.json.buildJsonObject {
                    put("status", "Dibatalkan")
                }
            ) {
                filter {
                    eq("status", "Terjadwal")
                    lt("date", today)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getPackagesForClinic(clinicName: String?): List<com.project.fisionettest.data.model.Package> {
        val clinicId = when (clinicName) {
            "Cabang 1" -> 1
            "Cabang 2" -> 2
            else -> null
        }

        try {
            val mappings = client.from("cabang_package").select(
                columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, packages(*)")
            ) {
                if (clinicId != null) {
                    filter { eq("id_cabang", clinicId) }
                }
            }.decodeList<com.project.fisionettest.data.model.CabangPackage>()

            val allTools = client.from("tools").select().decodeList<com.project.fisionettest.data.model.Tool>()
            val toolsMap = allTools.associate { it.id to it.nama_tools }

            return mappings.groupBy { it.id_package }.map { (pkgId, maps) ->
                val firstMap = maps.first()
                val pkgName = firstMap.packages?.name ?: "Paket $pkgId"
                val pkgPrice = firstMap.packages?.price ?: 0.0
                val toolIds = maps.flatMap { it.id_tools ?: emptyList() }.distinct()
                val toolNames = toolIds.mapNotNull { toolsMap[it] }
                com.project.fisionettest.data.model.Package(
                    id = pkgId,
                    name = pkgName,
                    price = pkgPrice,
                    tools = toolNames
                )
            }.sortedBy { it.name }
        } catch (e: Exception) {
            e.printStackTrace()
            return try {
                client.from("packages").select().decodeList<com.project.fisionettest.data.model.Package>()
            } catch (ex: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getCabangPackagesForClinic(clinicName: String?): List<com.project.fisionettest.data.model.CabangPackage> {
        val clinicId = when (clinicName) {
            "Cabang 1" -> 1
            "Cabang 2" -> 2
            else -> null
        }

        try {
            val mappings = client.from("cabang_package").select(
                columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, packages(*)")
            ) {
                if (clinicId != null) {
                    filter { eq("id_cabang", clinicId) }
                }
            }.decodeList<com.project.fisionettest.data.model.CabangPackage>()

            val allTools = client.from("tools").select().decodeList<com.project.fisionettest.data.model.Tool>()
            val toolsMap = allTools.associate { it.id to it.nama_tools }

            return mappings.map { map ->
                val toolNames = map.id_tools?.mapNotNull { toolsMap[it] } ?: emptyList()
                val pkgWithTools = map.packages?.copy(tools = toolNames)
                map.copy(packages = pkgWithTools)
            }.sortedBy { it.packages?.name }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}
