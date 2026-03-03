package com.project.fisionettest

import android.app.Application
import com.project.fisionettest.data.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FisioNetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseClient.initialize(this@FisioNetApplication)
        }
    }
}
