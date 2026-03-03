package com.project.fisionettest

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.project.fisionettest.databinding.ActivityMainBinding
import com.midtrans.sdk.uikit.external.UiKitApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMidtrans()
        setupNavigation()
    }

    private fun setupMidtrans() {
        val clientKey = "Mid-client-3-jjqxeEzLKfjlw7"
        val merchantBaseUrl = "https://fisionet.midtrans.com/"

        lifecycleScope.launch(Dispatchers.IO) {
            UiKitApi.Builder()
                .withContext(this@MainActivity.applicationContext)
                .withMerchantUrl(merchantBaseUrl)
                .withMerchantClientKey(clientKey)
                .enableLog(true)
                .build()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup bottom navigation with nav controller
        binding.bottomNavigation.setupWithNavController(navController)

        // Control bottom navigation visibility based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.dashboardFragment,
                R.id.homeFragment,
                R.id.appointmentFragment,
                R.id.profileFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }
        }
    }
}