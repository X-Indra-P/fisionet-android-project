package com.project.fisionettest.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClient {
    private const val SUPABASE_URL = "https://siyojsvbaqmjognnpwkm.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNpeW9qc3ZiYXFtam9nbm5wd2ttIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUxNzU0MDgsImV4cCI6MjA4MDc1MTQwOH0.SWyy2N0qPbFxttUu1dWJFG0LIHMKrDDlkuU2tuEW4HE"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
        })

        install(Auth)
        install(Postgrest)
    }
}
