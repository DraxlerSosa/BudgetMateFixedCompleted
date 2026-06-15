package com.budgetmate.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.budgetmate.app.R
import com.budgetmate.app.databinding.ActivityMainBinding
import com.budgetmate.app.ui.auth.AuthActivity

/**
 * Host Activity for the main app.
 * Contains the BottomNavigationView and NavHostFragment.
 */
class MainActivity : AppCompatActivity() {

    companion object { private const val TAG = "MainActivity" }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "MainActivity created")

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHost.navController)
    }

    /** Called from SettingsFragment on logout. Clears back stack and returns to Auth. */
    fun logout() {
        Log.i(TAG, "Logging out → AuthActivity")
        startActivity(Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}