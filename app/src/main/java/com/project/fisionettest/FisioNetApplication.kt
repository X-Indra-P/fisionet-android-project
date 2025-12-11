package com.project.fisionettest

import android.app.Application
import com.project.fisionettest.data.SupabaseClient

class FisioNetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClient.initialize(this)
    }
}
